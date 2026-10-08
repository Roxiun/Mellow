package com.roxiun.mellow.feature.stats;

import com.roxiun.mellow.feature.tags.TagPolicy;
import com.roxiun.mellow.api.tags.TagReport;
import com.roxiun.mellow.Mellow;
import com.roxiun.mellow.api.bedwars.BedwarsPlayer;
import com.roxiun.mellow.api.provider.model.FetchFailureReason;
import com.roxiun.mellow.api.hypixel.HypixelFeatures;
import com.roxiun.mellow.api.provider.model.StatScope;
import com.roxiun.mellow.cache.PlayerCache;
import com.roxiun.mellow.cache.ProfileFetchResult;
import com.roxiun.mellow.config.MellowOneConfig;
import com.roxiun.mellow.core.async.AsyncExecutor;
import com.roxiun.mellow.data.PlayerProfile;
import com.roxiun.mellow.data.TabStats;
import com.roxiun.mellow.feature.alerts.AlertSoundGate;
import com.roxiun.mellow.feature.nicks.NickUtils;
import com.roxiun.mellow.feature.stats.tab.ExtendedTabStatsColumns;
import com.roxiun.mellow.feature.tags.TagUtils;
import com.roxiun.mellow.gamestate.GameSnapshot;
import com.roxiun.mellow.util.ChatUtils;
import com.roxiun.mellow.util.UUIDUtils;
import com.roxiun.mellow.util.annoylist.AnnoylistManager;
import com.roxiun.mellow.util.blacklist.BlacklistManager;
import com.roxiun.mellow.util.blacklist.BlacklistedPlayer;
import com.roxiun.mellow.util.formatting.FormattingUtils;
import com.roxiun.mellow.util.player.PlayerUtils;
import com.roxiun.mellow.util.tagignore.TagIgnoreManager;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.scoreboard.ScorePlayerTeam;

public class StatsChecker {

    private static final String MC_COLOR_CODES = "0123456789abcdef";
    private static final int BEDWARS_WINSTREAK_COLUMN = 4;
    private final PlayerCache playerCache;
    private final NickUtils nickUtils;
    private final MellowOneConfig config;
    private final Map<String, TabStats> tabStats;
    private final TagUtils tagUtils;
    private final BlacklistManager blacklistManager;
    private final AnnoylistManager annoylistManager;
    private final TagIgnoreManager tagIgnoreManager;
    private final Minecraft mc = Minecraft.getMinecraft();
    private final Set<String> tabFetchInFlight = ConcurrentHashMap.newKeySet();
    private final Set<String> reportedTabFetchFailuresThisMatch =
        ConcurrentHashMap.newKeySet();
    private final Set<UUID> outboundWarnedOpponentsThisMatch =
        ConcurrentHashMap.newKeySet();
    private final AlertSoundGate inGameAlertSoundGate = new AlertSoundGate();

    public StatsChecker(
        PlayerCache playerCache,
        NickUtils nickUtils,
        MellowOneConfig config,
        Map<String, TabStats> tabStats,
        TagUtils tagUtils,
        BlacklistManager blacklistManager,
        AnnoylistManager annoylistManager,
        TagIgnoreManager tagIgnoreManager
    ) {
        this.playerCache = playerCache;
        this.nickUtils = nickUtils;
        this.config = config;
        this.tabStats = tabStats;
        this.tagUtils = tagUtils;
        this.blacklistManager = blacklistManager;
        this.annoylistManager = annoylistManager;
        this.tagIgnoreManager = tagIgnoreManager;
    }

    private long fetchGeneration;
    private final Map<String, Long> retryAfter = new ConcurrentHashMap<>();
    private final Set<String> alertedSources = ConcurrentHashMap.newKeySet();

    public List<String> fetchTabStatsForPlayers(List<String> names, boolean clearBeforeFetch, boolean forceRefresh) {
        if (clearBeforeFetch || forceRefresh) {
            fetchGeneration++;
            tabFetchInFlight.clear();
            retryAfter.clear();
        }
        if (clearBeforeFetch) tabStats.clear();
        List<String> scheduled = new ArrayList<>();
        List<Runnable> statsRequests = new ArrayList<>();
        Set<String> statsUuids = new java.util.LinkedHashSet<>();
        Map<String, PlayerProfile> tagLookups = new java.util.LinkedHashMap<>();
        if (names == null) return scheduled;
        final long generation = fetchGeneration;
        final long session = HypixelFeatures.getInstance().getGameSnapshot().getSessionId();
        final StatScope scope = resolveActiveScope();
        for (String name : names) {
            UUID uuid = PlayerUtils.getTrustedTabUuid(name);
            if (uuid == null || nickUtils.isNicked(name) || PlayerUtils.isNickedOrNpc(name)) continue;
            String key = name.toLowerCase(Locale.ROOT);
            if (!forceRefresh && retryAfter.getOrDefault(key, 0L) > System.currentTimeMillis()) continue;
            if (!tabFetchInFlight.add(key)) continue;
            scheduled.add(key);
            retryAfter.put(key, System.currentTimeMillis() + 120_000L);
            if (forceRefresh) {
                playerCache.clearPlayerStats(name);
                playerCache.clearPlayerTags(uuid.toString(), name);
            }
            PlayerProfile identity = PlayerProfile.identity(uuid.toString(), name);
            if (shouldScanForInGameWarnings()) sendBlacklistAndTagAlerts(identity, name);
            if (shouldDeferRemoteTagLookup()) tagLookups.put(name, identity);
            if (!config.tabStats && !config.printStats) {
                tabFetchInFlight.remove(key);
                continue;
            }
            statsUuids.add(uuid.toString());
            statsRequests.add(() -> {
                try {
                    ProfileFetchResult result = playerCache.getProfileForIdentity(name, uuid.toString(), false);
                    mc.addScheduledTask(() -> {
                        if (!isCurrent(session, generation)) return;
                        PlayerProfile profile = result.getProfile();
                        boolean hasStats = profile != null && hasStatsForScope(profile, scope);
                        if (!hasStats) {
                            retryAfter.put(key, System.currentTimeMillis() + 10_000L);
                            maybeReportLiveFetchFailure(name, result);
                            return;
                        }
                        if (passesScopeFilters(profile, scope)) {
                            if (config.tabStats) {
                                TabStats old = tabStats.get(name);
                                TabStats row = profile.getTabStats(scope);
                                tabStats.put(name, old == null ? row : row.withTags(old.getTags()));
                            }
                            if (config.printStats && (forceRefresh || alertedSources.add(key + ":stats"))) ChatUtils.sendMessage(formatChatStats(profile, scope));
                        }
                        warmHiddenWinstreakCache(profile, scope);
                    });
                } finally {
                    mc.addScheduledTask(() -> { if (isCurrent(session, generation)) tabFetchInFlight.remove(key); });
                }
            });
        }
        if (playerCache.getSelectedProvider() != null && playerCache.getSelectedProvider().supportsBatch()) {
            AsyncExecutor.getInstance().profileIo(() -> {
                playerCache.prefetchStats(statsUuids);
                for (Runnable request : statsRequests) request.run();
            });
        } else for (Runnable request : statsRequests) AsyncExecutor.getInstance().profileIo(request);
        if (!tagLookups.isEmpty()) AsyncExecutor.getInstance().supplementalIo(() -> {
            Set<String> uuids = new java.util.LinkedHashSet<>();
            for (PlayerProfile identity : tagLookups.values()) uuids.add(identity.getUuid());
            Map<String, TagReport> reports = playerCache.fetchTagReports(uuids);
            mc.addScheduledTask(() -> {
                if (!isCurrent(session, generation)) return;
                for (Map.Entry<String, PlayerProfile> entry : tagLookups.entrySet()) {
                    String name = entry.getKey();
                    TagReport report = reports.get(entry.getValue().getUuid().replace("-", ""));
                    if (report == null) continue;
                    PlayerProfile tagged = entry.getValue().withTags(report);
                    if (config.showAutomaticStatsErrors) for (Map.Entry<String, String> failure : report.getFailures().entrySet()) {
                        if (reportedTabFetchFailuresThisMatch.add("tags:" + failure.getKey()))
                            ChatUtils.sendMessage("§e" + failure.getKey() + " tags unavailable: " + failure.getValue());
                    }
                    if (!report.getFailures().isEmpty()) retryAfter.put(name.toLowerCase(Locale.ROOT), System.currentTimeMillis() + 10_000L);
                    TabStats row = tabStats.get(name);
                    if (config.tabStats && shouldShowRemoteTagsInTab())
                        tabStats.put(name, row == null ? tagged.getTabStats(scope) : row.withTags(report.retainFailedSources(row.getTags())));
                    if (shouldScanForInGameWarnings()) sendBlacklistAndTagAlerts(tagged, name);
                }
            });
        });
        return scheduled;
    }

    private boolean isCurrent(long session, long generation) {
        GameSnapshot current = HypixelFeatures.getInstance().getGameSnapshot();
        return generation == fetchGeneration && current.getSessionId() == session
            && StatScopeResolver.isSupportedLiveMatch(current);
    }

    private boolean shouldDeferRemoteTagLookup() {
        if (config == null) {
            return false;
        }

        boolean tabNeedsCoralTags = config.shouldShowCoralTagsInTab() && config.isCoralEnabled();
        boolean warningNeedsTags =
            config.printBlacklistTags && (config.isCoralEnabled() || config.xadia);
        return tabNeedsCoralTags || (config.xadia && config.showXadiaTagsInTab) || warningNeedsTags;
    }

    private boolean shouldShowRemoteTagsInTab() {
        return (
            config != null &&
            ((config.shouldShowCoralTagsInTab() && config.isCoralEnabled()) ||
                (config.xadia && config.showXadiaTagsInTab))
        );
    }

    private void maybeReportLiveFetchFailure(
        String playerName,
        ProfileFetchResult result
    ) {
        if (!config.showAutomaticStatsErrors) {
            return;
        }
        if (playerName == null || playerName.trim().isEmpty()) {
            return;
        }
        if (
            result != null &&
            result.getFailureReason() == FetchFailureReason.UUID_UNAVAILABLE
        ) {
            return;
        }

        String reasonKey = result == null || result.getFailureReason() == null
            ? "UNKNOWN"
            : result.getFailureReason().name();
        String failureKey =
            playerName.toLowerCase(Locale.ROOT) + ":" + reasonKey;
        if (!reportedTabFetchFailuresThisMatch.add(failureKey)) {
            return;
        }

        String reason = StatsFetchFailureFormatter.describe(result);
        mc.addScheduledTask(() ->
            ChatUtils.sendMessage(
                "§cFailed to fetch stats for: §r" +
                playerName +
                "§c (" +
                reason +
                ")"
            )
        );
    }

    public void resetInGameAlertSoundGate() {
        inGameAlertSoundGate.reset();
    }

    public void resetInGameMatchWarningState() {
        fetchGeneration++;
        tabFetchInFlight.clear();
        retryAfter.clear();
        alertedSources.clear();
        inGameAlertSoundGate.reset();
        reportedTabFetchFailuresThisMatch.clear();
        outboundWarnedOpponentsThisMatch.clear();
    }

    public boolean shouldScanForInGameWarnings() {
        if (config == null) {
            return false;
        }
        if (config.inGameBlacklistWarningDestination != 0) {
            return true;
        }
        if (!blacklistManager.getBlacklist().isEmpty()) {
            return true;
        }
        if (
            annoylistManager != null && !annoylistManager.getAnnoylist().isEmpty()
        ) {
            return true;
        }
        return config.printBlacklistTags && (config.isCoralEnabled() || config.xadia);
    }

    private void warmHiddenWinstreakCache(
        PlayerProfile profile,
        StatScope scope
    ) {
        if (
            config == null ||
            scope != StatScope.BEDWARS ||
            !config.tabStats ||
            !config.extendedTabStatsView ||
            !ExtendedTabStatsColumns.getConfiguredColumns(scope, config).contains(BEDWARS_WINSTREAK_COLUMN) ||
            !config.showHiddenWinstreaks ||
            Mellow.auroraWinstreakService == null
        ) {
            return;
        }

        BedwarsPlayer bedwarsPlayer = profile.getBedwarsPlayer();
        UUID playerUuid = parseUuid(profile.getUuid());
        if (bedwarsPlayer == null || playerUuid == null) {
            return;
        }
        if (isVisibleWinstreakUsable(bedwarsPlayer)) {
            return;
        }

        int minStars = resolveMinStars(config.winstreakMinStars);
        double minFkdr = resolveMinFkdr(config.winstreakMinFkdr);
        int stars = parseStarsInt(bedwarsPlayer.getStars());
        if (stars < minStars || bedwarsPlayer.getFkdr() < minFkdr) {
            return;
        }

        String compactUuid = playerUuid.toString().replace("-", "");
        if (Mellow.auroraWinstreakService.hasCachedWinstreak(compactUuid)) {
            return;
        }
        if (!Mellow.auroraWinstreakService.tryStartFetch(compactUuid)) {
            return;
        }

        long requestGeneration = Mellow.auroraWinstreakService.getGeneration();
        AsyncExecutor.getInstance().supplementalIo(() -> {
            try {
                int winstreak = Mellow.auroraWinstreakService.fetchWinstreakBlocking(
                    compactUuid
                );
                if (Mellow.auroraWinstreakService.getGeneration() == requestGeneration) Mellow.auroraWinstreakService.storeInCache(compactUuid, winstreak);
            } catch (Exception e) {
                if (config.showAutomaticStatsErrors && !Mellow.auroraWinstreakService.hasShownError()) {
                    Mellow.auroraWinstreakService.markErrorShown();
                    String detail = e.getMessage() == null ? "unknown" : e.getMessage();
                    mc.addScheduledTask(() ->
                        ChatUtils.sendMessage(
                            "§cAurora Winstreak API error: §6" + detail
                        )
                    );
                }
            } finally {
                if (Mellow.auroraWinstreakService.getGeneration() == requestGeneration) Mellow.auroraWinstreakService.finishFetch(compactUuid);
            }
        });
    }

    private int resolveMinStars(int index) {
        if (index <= 0) {
            return 0;
        }
        return index * 100;
    }

    private double resolveMinFkdr(int index) {
        int[] values = {
            0,
            1,
            2,
            3,
            4,
            5,
            10,
            15,
            20,
            25,
            30,
            40,
            50,
            60,
            70,
            80,
            90,
            100,
        };
        if (index < 0 || index >= values.length) {
            return 0;
        }
        return values[index];
    }

    private int parseStarsInt(String stars) {
        if (stars == null || stars.isEmpty()) {
            return 0;
        }
        try {
            return Integer.parseInt(
                stars.replaceAll("§.", "").replaceAll("[^0-9]", "")
            );
        } catch (NumberFormatException ignored) {
            return 0;
        }
    }

    private boolean isVisibleWinstreakUsable(BedwarsPlayer bedwarsPlayer) {
        if (bedwarsPlayer == null) {
            return false;
        }
        if (!bedwarsPlayer.hasWinstreakData()) {
            return false;
        }
        return bedwarsPlayer.getWinstreak() > 0;
    }

    private UUID parseUuid(String uuid) {
        if (uuid == null || uuid.trim().isEmpty()) {
            return null;
        }
        try {
            return UUIDUtils.fromString(uuid);
        } catch (Exception ignored) {
            return null;
        }
    }

    private StatScope resolveActiveScope() {
        GameSnapshot snapshot = HypixelFeatures.getInstance().getGameSnapshot();
        return StatScopeResolver.resolveInGameScope(snapshot);
    }

    private boolean hasStatsForScope(PlayerProfile profile, StatScope scope) {
        if (scope == StatScope.SKYWARS) {
            return profile.getSkywarsPlayer() != null;
        }
        if (scope == StatScope.DUELS) {
            return profile.getDuelsPlayer() != null;
        }
        if (scope == StatScope.BUILD_BATTLE) {
            return profile.getBuildBattlePlayer() != null;
        }
        if (scope == StatScope.TNT_RUN) {
            return profile.getTntRunPlayer() != null;
        }
        return profile.getBedwarsPlayer() != null;
    }

    private boolean passesScopeFilters(PlayerProfile profile, StatScope scope) {
        if (
            scope == StatScope.SKYWARS ||
            scope == StatScope.DUELS ||
            scope == StatScope.BUILD_BATTLE ||
            scope == StatScope.TNT_RUN
        ) {
            return true;
        }

        BedwarsPlayer player = profile.getBedwarsPlayer();
        return player != null && player.getFkdr() >= config.minFkdr;
    }

    private String formatChatStats(PlayerProfile profile, StatScope scope) {
        return scope == StatScope.BEDWARS
            ? formatBedwarsChatStats(profile)
            : ChatStatsFormatter.format(profile, scope);
    }

    private String formatBedwarsChatStats(PlayerProfile profile) {
        BedwarsPlayer player = profile.getBedwarsPlayer();
        if (player == null) {
            return "";
        }

        String displayName = player.getFormattedNameWithRank();
        String stars = player.getStars();
        String fkdr = player.getFkdrColor() + player.getFormattedFkdr();

        String winstreak = "";
        if (!player.hasWinstreakData()) {
            winstreak = "§7?";
        } else if (player.getWinstreak() > 0) {
            winstreak =
                FormattingUtils.formatWinstreak(
                    String.valueOf(player.getWinstreak())
                );
        }

        String base = String.format(
            "%s §r%s§r§7 |§r FKDR: %s",
            displayName,
            stars,
            fkdr
        );

        if (config.tags) {
            String tagsValue = buildTagsValue(profile);
            if (winstreak.isEmpty()) {
                return String.format("%s §r§7|§r [ %s ]", base, tagsValue);
            } else {
                return String.format(
                    "%s §r§7|§r WS: %s§r [ %s ]",
                    base,
                    winstreak,
                    tagsValue
                );
            }
        } else {
            if (winstreak.isEmpty()) {
                return base;
            } else {
                return String.format("%s §r§7|§r WS: %s§r", base, winstreak);
            }
        }
    }

    private String buildTagsValue(PlayerProfile profile) {
        BedwarsPlayer player = profile.getBedwarsPlayer();
        int starsInt = 0;
        try {
            starsInt = Integer.parseInt(
                player.getStars().replaceAll("§.", "").replaceAll("[^0-9]", "")
            );
        } catch (NumberFormatException ignored) {}

        String tagsValue = tagUtils.buildTags(
            profile.getName(),
            profile.getUuid(),
            starsInt,
            player.getFkdr(),
            player.getWinstreak(),
            player.getFinalKills(),
            player.getFinalDeaths()
        );

        if (tagsValue.endsWith(" ")) {
            return tagsValue.substring(0, tagsValue.length() - 1);
        }
        return tagsValue;
    }

    private void sendBlacklistAndTagAlerts(PlayerProfile profile, String tabPlayerName) {
        if (profile == null) return;
        UUID uuid = UUIDUtils.fromString(profile.getUuid());
        boolean ignored = tagIgnoreManager != null && tagIgnoreManager.isTagIgnored(uuid);
        Map<String, String> sources = TagPolicy.warnings(profile.getTags(), config.printBlacklistTags, ignored);
        sources.entrySet().removeIf(entry -> !alertedSources.add(uuid + ":" + entry.getKey()));
        for (Map.Entry<String, String> source : sources.entrySet())
            ChatUtils.sendMessage("§c" + profile.getName() + " is tagged on §d" + source.getKey() + "§c for: " + source.getValue());

        BlacklistedPlayer local = blacklistManager.getBlacklistedPlayer(uuid);
        boolean blacklisted = local != null && alertedSources.add(uuid + ":local");
        boolean annoylisted = annoylistManager != null && annoylistManager.isAnnoylisted(uuid) && alertedSources.add(uuid + ":annoy");
        if (blacklisted) ChatUtils.sendMessage("§6" + profile.getName() + " §cis on your blacklist" + formatBlacklistReasonSuffix(local.getReason()));
        if (annoylisted) ChatUtils.sendMessage("§6" + profile.getName() + " §3is on your annoy list: " + normalizeReason(annoylistManager.getAnnoylistedPlayer(uuid).getReason()));
        if (shouldSendOutboundOpponentWarning(uuid, tabPlayerName, blacklisted, !sources.isEmpty()))
            sendOutboundOpponentWarning(profile, tabPlayerName, local, blacklisted, sources);
        if (blacklisted || annoylisted || !sources.isEmpty()) inGameAlertSoundGate.tryPlayPling(mc, 1.0F, 1.0F);
    }

    private boolean shouldSendOutboundOpponentWarning(
        UUID uuid,
        String tabPlayerName,
        boolean blacklisted,
        boolean tagged
    ) {
        if (uuid == null) {
            return false;
        }
        if (!blacklisted && !tagged) {
            return false;
        }
        if (!isInBedwarsMatch()) {
            return false;
        }
        InGameBlacklistWarningDestination destination = resolveWarningDestination();
        if (destination == InGameBlacklistWarningDestination.NONE) {
            return false;
        }
        if (
            destination == InGameBlacklistWarningDestination.ALL_CHAT &&
            isBedwarsSolosMode()
        ) {
            return false;
        }
        if (!isOpponentByTabName(tabPlayerName)) {
            return false;
        }
        return outboundWarnedOpponentsThisMatch.add(uuid);
    }

    private void sendOutboundOpponentWarning(
        PlayerProfile profile,
        String tabPlayerName,
        BlacklistedPlayer blacklistedPlayer,
        boolean blacklisted,
        Map<String, String> sources
    ) {
        InGameBlacklistWarningDestination destination = resolveWarningDestination();
        String commandPrefix = destination.getCommandPrefix();
        if (commandPrefix == null) {
            return;
        }

        String playerName = profile.getName();
        if (playerName == null || playerName.trim().isEmpty()) {
            playerName = "Unknown";
        }
        String opponentTeamName = resolveOpponentTeamName(tabPlayerName, playerName);

        List<String> sourceLabels = new ArrayList<>(sources.keySet());
        List<String> detailParts = new ArrayList<>();
        if (blacklisted) {
            sourceLabels.add(0, "Local");
            detailParts.add("Local: " + formatOutboundBlacklistReason(blacklistedPlayer));
        }
        for (Map.Entry<String, String> source : sources.entrySet())
            detailParts.add(source.getKey() + ": " + normalizeOutboundDetail(source.getValue()));

        String mainMessage = opponentTeamName.isEmpty()
            ? "[Mellow] Flagged opponent: " +
            playerName +
            " [" +
            String.join(", ", sourceLabels) +
            "]"
            : "[Mellow] Flagged opponent on " +
            opponentTeamName +
            " team: " +
            playerName +
            " [" +
            String.join(", ", sourceLabels) +
            "]";
        String detailMessage = detailParts.isEmpty()
            ? null
            : "[Mellow] " + playerName + " tagged for: " + String.join(" | ", detailParts);

        mc.addScheduledTask(() -> {
            ChatUtils.sendChatCommandMessage(
                commandPrefix,
                mainMessage
            );
            if (detailMessage != null) {
                ChatUtils.sendChatCommandMessage(commandPrefix, detailMessage);
            }
        });
    }

    private InGameBlacklistWarningDestination resolveWarningDestination() {
        if (config == null) {
            return InGameBlacklistWarningDestination.NONE;
        }
        return InGameBlacklistWarningDestination.fromConfig(
            config.inGameBlacklistWarningDestination
        );
    }

    private boolean isInBedwarsMatch() {
        GameSnapshot snapshot = HypixelFeatures.getInstance().getGameSnapshot();
        return snapshot != null && snapshot.isInBedwarsMatch();
    }

    private boolean isBedwarsSolosMode() {
        GameSnapshot snapshot = HypixelFeatures.getInstance().getGameSnapshot();
        if (snapshot == null || !snapshot.isInBedwarsMatch()) {
            return false;
        }

        String mode = snapshot.getMode();
        if (mode == null || mode.trim().isEmpty()) {
            return false;
        }

        String normalized = mode
            .toLowerCase(Locale.ROOT)
            .replace('-', '_')
            .replaceAll("\\s+", "_");
        return normalized.contains("eight_one");
    }

    private boolean isOpponentByTabName(String tabPlayerName) {
        if (mc == null || mc.thePlayer == null) {
            return false;
        }
        if (tabPlayerName == null || tabPlayerName.trim().isEmpty()) {
            return false;
        }

        String selfTeam = resolveTeamKey(mc.thePlayer.getName());
        String playerTeam = resolveTeamKey(tabPlayerName);
        if (selfTeam.isEmpty() || playerTeam.isEmpty()) {
            return false;
        }
        return !selfTeam.equalsIgnoreCase(playerTeam);
    }

    private String resolveTeamKey(String playerName) {
        if (
            mc == null ||
            mc.theWorld == null ||
            mc.theWorld.getScoreboard() == null ||
            playerName == null ||
            playerName.trim().isEmpty()
        ) {
            return "";
        }

        ScorePlayerTeam team = mc.theWorld.getScoreboard().getPlayersTeam(playerName);
        if (team == null) {
            return "";
        }

        String registeredName = team.getRegisteredName();
        if (registeredName != null && !registeredName.trim().isEmpty()) {
            return registeredName.trim();
        }

        return ChatUtils.stripFormatting(team.getColorPrefix()).trim();
    }

    private String resolveOpponentTeamName(
        String tabPlayerName,
        String fallbackPlayerName
    ) {
        ScorePlayerTeam team = resolveScoreboardTeam(tabPlayerName, fallbackPlayerName);
        if (team == null) {
            return "";
        }

        String fromRegisteredName = normalizeTeamName(team.getRegisteredName());
        if (!fromRegisteredName.isEmpty()) {
            return fromRegisteredName;
        }

        String colorPrefix = team.getColorPrefix();
        String fromPrefixText = normalizeTeamName(
            ChatUtils.stripFormatting(colorPrefix)
        );
        if (!fromPrefixText.isEmpty()) {
            return fromPrefixText;
        }

        return mapColorCodeToTeamName(extractMinecraftColorCode(colorPrefix));
    }

    private ScorePlayerTeam resolveScoreboardTeam(
        String primaryPlayerName,
        String fallbackPlayerName
    ) {
        if (mc == null || mc.theWorld == null || mc.theWorld.getScoreboard() == null) {
            return null;
        }

        if (primaryPlayerName != null && !primaryPlayerName.trim().isEmpty()) {
            ScorePlayerTeam team = mc.theWorld
                .getScoreboard()
                .getPlayersTeam(primaryPlayerName);
            if (team != null) {
                return team;
            }
        }

        if (fallbackPlayerName != null && !fallbackPlayerName.trim().isEmpty()) {
            return mc.theWorld.getScoreboard().getPlayersTeam(fallbackPlayerName);
        }

        return null;
    }

    private String normalizeTeamName(String value) {
        if (value == null) {
            return "";
        }

        String normalized = ChatUtils
            .stripFormatting(value)
            .toLowerCase(Locale.ROOT)
            .replaceAll("[^a-z]", "");
        if (normalized.isEmpty()) {
            return "";
        }

        if (
            normalized.equals("r") ||
            normalized.contains("red")
        ) {
            return "Red";
        }
        if (
            normalized.equals("b") ||
            normalized.contains("blue")
        ) {
            return "Blue";
        }
        if (
            normalized.equals("g") ||
            normalized.contains("green")
        ) {
            return "Green";
        }
        if (
            normalized.equals("y") ||
            normalized.contains("yellow")
        ) {
            return "Yellow";
        }
        if (
            normalized.equals("a") ||
            normalized.contains("aqua") ||
            normalized.contains("cyan")
        ) {
            return "Aqua";
        }
        if (
            normalized.equals("w") ||
            normalized.contains("white")
        ) {
            return "White";
        }
        if (
            normalized.equals("p") ||
            normalized.contains("pink") ||
            normalized.contains("lightpurple") ||
            normalized.contains("magenta")
        ) {
            return "Pink";
        }
        if (
            normalized.equals("gr") ||
            normalized.contains("gray") ||
            normalized.contains("grey") ||
            normalized.contains("silver")
        ) {
            return "Gray";
        }

        return "";
    }

    private String mapColorCodeToTeamName(char colorCode) {
        switch (colorCode) {
            case 'c':
            case '4':
                return "Red";
            case '9':
            case '1':
                return "Blue";
            case 'a':
            case '2':
                return "Green";
            case 'e':
            case '6':
                return "Yellow";
            case 'b':
            case '3':
                return "Aqua";
            case 'f':
                return "White";
            case 'd':
            case '5':
                return "Pink";
            case '7':
            case '8':
                return "Gray";
            default:
                return "";
        }
    }

    private char extractMinecraftColorCode(String input) {
        if (input == null || input.length() < 2) {
            return '\0';
        }

        for (int i = 0; i < input.length() - 1; i++) {
            if (input.charAt(i) == '\u00A7') {
                char maybeColor = Character.toLowerCase(input.charAt(i + 1));
                if (MC_COLOR_CODES.indexOf(maybeColor) >= 0) {
                    return maybeColor;
                }
            }
        }
        return '\0';
    }

    private String formatOutboundBlacklistReason(BlacklistedPlayer blacklistedPlayer) {
        if (blacklistedPlayer == null) {
            return "listed locally";
        }
        String reason = blacklistedPlayer.getReason();
        if (reason == null) {
            return "listed locally";
        }
        String trimmed = reason.trim();
        if (
            trimmed.isEmpty() ||
            BlacklistManager.isExternalFileImportReason(trimmed)
        ) {
            return "listed locally";
        }
        return normalizeOutboundDetail(trimmed);
    }

    private String normalizeOutboundDetail(String detail) {
        if (detail == null) {
            return "unknown reason";
        }

        String normalized = ChatUtils
            .stripFormatting(detail)
            .replace("\r", "")
            .replace("\n", ", ")
            .replace("(null)", "(unknown reason)")
            .replaceAll("\\s+", " ")
            .trim();
        if (normalized.isEmpty()) {
            return "unknown reason";
        }
        return normalized;
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
}
