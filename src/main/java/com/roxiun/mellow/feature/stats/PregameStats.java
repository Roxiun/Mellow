package com.roxiun.mellow.feature.stats;

import com.roxiun.mellow.feature.tags.TagPolicy;
import com.roxiun.mellow.api.bedwars.BedwarsPlayer;
import com.roxiun.mellow.api.hypixel.HypixelFeatures;
import com.roxiun.mellow.cache.PlayerCache;
import com.roxiun.mellow.cache.ProfileFetchContext;
import com.roxiun.mellow.cache.ProfileFetchResult;
import com.roxiun.mellow.config.MellowOneConfig;
import com.roxiun.mellow.core.async.AsyncExecutor;
import com.roxiun.mellow.core.async.MainThreadDispatcher;
import com.roxiun.mellow.data.PlayerProfile;
import com.roxiun.mellow.feature.alerts.AlertSoundGate;
import com.roxiun.mellow.gamestate.GameSnapshot;
import com.roxiun.mellow.gamestate.PartyState;
import com.roxiun.mellow.util.ChatUtils;
import com.roxiun.mellow.util.UUIDUtils;
import com.roxiun.mellow.util.annoylist.AnnoylistManager;
import com.roxiun.mellow.util.annoylist.AnnoylistedPlayer;
import com.roxiun.mellow.util.blacklist.BlacklistManager;
import com.roxiun.mellow.util.blacklist.BlacklistedPlayer;
import com.roxiun.mellow.util.player.PlayerUtils;
import com.roxiun.mellow.util.tagignore.TagIgnoreManager;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.minecraft.client.Minecraft;
import net.hypixel.data.type.GameType;
import net.minecraftforge.client.event.ClientChatReceivedEvent;

public class PregameStats {

    private final Minecraft mc = Minecraft.getMinecraft();
    private final PlayerCache playerCache;
    private final MellowOneConfig config;
    private final BlacklistManager blacklistManager;
    private final AnnoylistManager annoylistManager;
    private final TagIgnoreManager tagIgnoreManager;

    private final Set<String> alreadyLookedUp = ConcurrentHashMap.newKeySet();
    private final Set<String> alertedSources = ConcurrentHashMap.newKeySet();
    private final AlertSoundGate pregameAlertSoundGate = new AlertSoundGate();
    private boolean autoLeaveTriggeredThisPregame;

    private static final Pattern BEDWARS_CHAT_PATTERN = Pattern.compile(
        "^(?:\\[.*?\\]\\s*)*(\\w{3,16})(?::| ») (.*)$"
    );
    public PregameStats(
        PlayerCache playerCache,
        MellowOneConfig config,
        BlacklistManager blacklistManager,
        AnnoylistManager annoylistManager,
        TagIgnoreManager tagIgnoreManager
    ) {
        this.playerCache = playerCache;
        this.config = config;
        this.blacklistManager = blacklistManager;
        this.annoylistManager = annoylistManager;
        this.tagIgnoreManager = tagIgnoreManager;
    }

    public void onWorldChange() {
        alreadyLookedUp.clear();
        alertedSources.clear();
        pregameAlertSoundGate.reset();
        autoLeaveTriggeredThisPregame = false;
    }

    public void onChat(ClientChatReceivedEvent event) {
        if (!config.pregameStats && !config.mentionLobbyStats) {
            return;
        }

        GameSnapshot snapshot = HypixelFeatures.getInstance().getGameSnapshot();
        boolean inPregameLobby = snapshot.isInBedwarsSession() && snapshot.isPregame();
        boolean inBedwarsLobby = isInBedwarsLobby(snapshot);

        boolean pregameTriggerEnabled = config.pregameStats && inPregameLobby;
        boolean mentionTriggerEnabled = config.mentionLobbyStats && inBedwarsLobby;

        if (!pregameTriggerEnabled && !mentionTriggerEnabled) {
            return;
        }

        String raw = event.message.getUnformattedText();
        String formatted = event.message.getFormattedText();
        String message = raw.replaceAll("§.", "").trim();

        ParsedChatMessage parsedMessage = parseChatMessage(message);
        if (parsedMessage == null) {
            return;
        }

        String username = parsedMessage.sender;
        if (username.equalsIgnoreCase(mc.thePlayer.getName())) {
            return;
        }

        boolean isMention = mentionTriggerEnabled &&
        containsSelfMention(parsedMessage.content);
        boolean shouldLookup = pregameTriggerEnabled || isMention;
        if (!shouldLookup) {
            return;
        }

        if (isPartyMemberByName(username)) {
            return;
        }

        if (hasObfuscatedSender(formatted)) {
            alreadyLookedUp.add(username.toLowerCase(Locale.ROOT));
            return;
        }

        if (!alreadyLookedUp.add(username.toLowerCase())) {
            return;
        }

        long session = snapshot.getSessionId();
        AsyncExecutor.getInstance().profileIo(() -> handlePlayer(username, true, session));
    }

    private ParsedChatMessage parseChatMessage(String message) {
        Matcher chatMatch = BEDWARS_CHAT_PATTERN.matcher(message);
        if (chatMatch.find()) {
            return new ParsedChatMessage(chatMatch.group(1), chatMatch.group(2));
        }
        // Ignore non-chat/system lines (for example "/pl" output) to avoid
        // false mention-trigger lookups.
        return null;
    }

    private boolean containsSelfMention(String content) {
        if (mc.thePlayer == null || content == null || content.isEmpty()) {
            return false;
        }

        String selfName = mc.thePlayer.getName();
        if (selfName == null || selfName.isEmpty()) {
            return false;
        }

        String mentionPattern =
            "(?i)(^|[^A-Za-z0-9_])" +
            Pattern.quote(selfName) +
            "($|[^A-Za-z0-9_])";
        return Pattern.compile(mentionPattern).matcher(content).find();
    }

    private boolean isInBedwarsLobby(GameSnapshot snapshot) {
        if (snapshot == null || !snapshot.isOnHypixel() || !snapshot.isLobby()) {
            return false;
        }

        if (snapshot.getGameType() == GameType.BEDWARS) {
            return true;
        }

        return snapshot
            .getScoreboardTitle()
            .toLowerCase(Locale.ROOT)
            .contains("bed wars");
    }

    private void handlePlayer(String username, boolean sendStats, long session) {
        ProfileFetchResult identityResult = playerCache.getIdentityResult(username, ProfileFetchContext.PREGAME);
        PlayerProfile identity = identityResult.getProfile();
        if (identity == null) {
            inSession(session, () -> { alreadyLookedUp.remove(username.toLowerCase(Locale.ROOT)); });
            return;
        }
        inSession(session, () -> publishProfile(username, identityResult, false, session));
        if (config.printBlacklistTags && (config.isCoralEnabled() || config.xadia)) {
            AsyncExecutor.getInstance().supplementalIo(() -> {
                PlayerProfile tagged = playerCache.enrichProfileWithTags(identity);
                inSession(session, () -> publishProfile(username, ProfileFetchResult.success(tagged, null), false, session));
            });
        }
        ProfileFetchResult result = playerCache.getProfileForIdentity(username, identity.getUuid(), false);
        inSession(session, () -> publishProfile(username, result, sendStats, session));
    }

    private void publishProfile(String username, ProfileFetchResult result, boolean sendStats, long session) {
        PlayerProfile profile = result.getProfile();
        BedwarsPlayer player = profile == null
            ? null
            : profile.getBedwarsPlayer();

        if (sendStats && player == null) {
            alreadyLookedUp.remove(username.toLowerCase(Locale.ROOT));
            if (shouldSuppressFailureMessage(result)) {
                return;
            }
            if (sendStats && config.showAutomaticStatsErrors) {
                inSession(session, () ->
                    ChatUtils.sendMessage(
                        "§cFailed to fetch stats for: §r" +
                            username +
                            "§c (" +
                            StatsFetchFailureFormatter.describe(result) +
                            ")"
                    )
                );
            }
            if (profile == null) {
                return;
            }
        }

        if (config.showAutomaticStatsErrors) for (java.util.Map.Entry<String, String> failure : profile.getTags().getFailures().entrySet()) {
            if (alertedSources.add("error:" + failure.getKey()))
                ChatUtils.sendMessage("§e" + failure.getKey() + " tags unavailable: " + failure.getValue());
        }
        UUID uuid = UUIDUtils.fromString(profile.getUuid());
        if (isPartyMember(uuid)) {
            return;
        }

        boolean blacklisted = blacklistManager.isBlacklisted(uuid) && alertedSources.add(uuid + ":local");
        boolean annoylisted =
            annoylistManager != null && annoylistManager.isAnnoylisted(uuid) && alertedSources.add(uuid + ":annoy");
        boolean tagsIgnored =
            tagIgnoreManager != null && tagIgnoreManager.isTagIgnored(uuid);
        java.util.Map<String, String> tagWarnings = TagPolicy.warnings(
            profile.getTags(), config.printBlacklistTags, tagsIgnored);
        tagWarnings.entrySet().removeIf(entry -> !alertedSources.add(uuid + ":" + entry.getKey()));
        if (blacklisted || annoylisted) {
            BlacklistedPlayer blacklistedPlayer = blacklisted
                ? blacklistManager.getBlacklistedPlayer(uuid)
                : null;
            AnnoylistedPlayer annoylistedPlayer = annoylisted
                ? annoylistManager.getAnnoylistedPlayer(uuid)
                : null;
            String blacklistReasonSuffix = formatBlacklistReasonSuffix(
                blacklistedPlayer == null ? null : blacklistedPlayer.getReason()
            );
            String annoyReason = normalizeReason(
                annoylistedPlayer == null ? null : annoylistedPlayer.getReason()
            );

            inSession(session, () -> {
                if (blacklisted) {
                    ChatUtils.sendMessage(
                        "§6" +
                        username +
                        " §cis on your blacklist" +
                        blacklistReasonSuffix
                    );
                    maybeAutoLeavePregameForBlacklistedChat(username);
                }
                if (annoylisted) {
                    ChatUtils.sendMessage(
                        "§6" +
                        username +
                        " §3is on your annoy list: " +
                        annoyReason
                    );
                }
            });
        }

        if (sendStats && player != null) {
            String stats =
                player.getStars() +
                " §r" +
                player.getFormattedNameWithRank() +
                " §7|§r FKDR: " +
                player.getFkdrColor() +
                player.getFormattedFkdr();
            inSession(session, () -> ChatUtils.sendMessage(stats));
        }

        for (java.util.Map.Entry<String, String> source : tagWarnings.entrySet()) {
            inSession(session, () -> ChatUtils.sendMessage("§c" + username + " is tagged on §d" + source.getKey() + "§c for: " + source.getValue()));
        }
        if (blacklisted || annoylisted || !tagWarnings.isEmpty())
            inSession(session, () -> pregameAlertSoundGate.tryPlayPling(mc, 1.0F, 1.0F));
    }

    private boolean hasObfuscatedSender(String formattedMessage) {
        if (formattedMessage == null || formattedMessage.isEmpty()) {
            return false;
        }

        int delimiterIndex = formattedMessage.indexOf(": ");
        if (delimiterIndex < 0) {
            delimiterIndex = formattedMessage.indexOf(" » ");
        }
        if (delimiterIndex < 0) {
            return false;
        }

        String senderSection = formattedMessage.substring(0, delimiterIndex);
        return senderSection.toLowerCase(Locale.ROOT).contains("§k");
    }

    private boolean shouldSuppressFailureMessage(ProfileFetchResult result) {
        return result != null &&
        result.getFailureReason() ==
        com.roxiun.mellow.api.provider.model.FetchFailureReason.UUID_UNAVAILABLE;
    }

    private boolean isPartyMemberByName(String username) {
        if (username == null || username.isEmpty()) {
            return false;
        }

        String compactUuid = PlayerUtils.getUUIDFromPlayerName(username);
        if (compactUuid == null || compactUuid.isEmpty()) {
            return false;
        }

        try {
            return isPartyMember(UUIDUtils.fromString(compactUuid));
        } catch (IllegalArgumentException ignored) {
            return false;
        }
    }

    private String formatBlacklistReasonSuffix(String reason) {
        if (reason == null) {
            return "";
        }
        String trimmed = reason.trim();
        if (
            trimmed.isEmpty() ||
            "(none)".equalsIgnoreCase(trimmed) ||
            BlacklistManager.isExternalFileImportReason(trimmed)
        ) {
            return "";
        }
        return ": " + trimmed;
    }

    private String normalizeReason(String reason) {
        if (reason == null || reason.trim().isEmpty()) {
            return "(none)";
        }
        return reason;
    }

    private boolean isPartyMember(UUID uuid) {
        if (uuid == null) {
            return false;
        }

        PartyState partyState = HypixelFeatures.getInstance().getPartyState();
        return (
            partyState != null &&
            partyState.isInParty() &&
            partyState.getMembers().containsKey(uuid)
        );
    }

    private void maybeAutoLeavePregameForBlacklistedChat(String username) {
        if (!config.pregameStats || !config.autoLeaveBlacklistedPregameChat || autoLeaveTriggeredThisPregame) {
            return;
        }

        GameSnapshot snapshot = HypixelFeatures.getInstance().getGameSnapshot();
        if (
            snapshot == null ||
            !snapshot.isOnHypixel() ||
            snapshot.getGameType() != GameType.BEDWARS ||
            !snapshot.isPregame()
        ) {
            return;
        }

        int secondsUntilStart = snapshot.getObservation().countdownSeconds;
        if (secondsUntilStart <= 2) {
            return;
        }

        autoLeaveTriggeredThisPregame = true;

        ChatUtils.sendMessage(
            "§eBlacklisted chatter detected (" +
            username +
            ") with §f" +
            secondsUntilStart +
            "§es until start. Leaving pregame."
        );

        if (mc.thePlayer != null) {
            mc.thePlayer.sendChatMessage(getAutoLeaveCommand());
        }
    }

    private String getAutoLeaveCommand() {
        String configured = config.autoLeaveBlacklistedPregameCommand;
        if (configured == null) {
            return "/lobby";
        }

        String command = configured.trim();
        if (command.isEmpty()) {
            return "/lobby";
        }

        if (!command.startsWith("/")) {
            return "/" + command;
        }

        return command;
    }

    private void inSession(long session, Runnable action) {
        MainThreadDispatcher.run(() -> {
            GameSnapshot current = HypixelFeatures.getInstance().getGameSnapshot();
            if (current.isOnHypixel() && current.getSessionId() == session && !current.isInBedwarsMatch()) action.run();
        });
    }

    private static class ParsedChatMessage {

        private final String sender;
        private final String content;

        private ParsedChatMessage(String sender, String content) {
            this.sender = sender;
            this.content = content;
        }
    }
}
