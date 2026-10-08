package com.roxiun.mellow.feature.stats;

import com.roxiun.mellow.util.cache.LookupTracker;
import com.roxiun.mellow.feature.tags.TagPolicy;
import com.roxiun.mellow.api.tags.TagReport;
import com.roxiun.mellow.Mellow;
import com.roxiun.mellow.stats.bedwars.BedwarsPlayer;
import com.roxiun.mellow.api.model.FetchFailureReason;
import com.roxiun.mellow.api.hypixel.HypixelFeatures;
import com.roxiun.mellow.stats.*;
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

    private final Map<String, TagReport> matchTags = new java.util.HashMap<>();
    private final Map<String, PlayerProfile> matchProfiles = new java.util.HashMap<>();
    private final LookupTracker<String> winstreakLookups = new LookupTracker<>();
    private long fetchGeneration;
    private final LookupTracker<String> statsLookups = new LookupTracker<>();
    private final LookupTracker<String> tagRequests = new LookupTracker<>();
    private final Set<String> alertedSources = ConcurrentHashMap.newKeySet();

    public List<String> fetchTabStatsForPlayers(List<String> names, boolean clearBeforeFetch, boolean forceRefresh) {
        if (clearBeforeFetch || forceRefresh) resetLookups();
        if (clearBeforeFetch) tabStats.clear();
        List<String> scheduled = new ArrayList<>();
        List<Runnable> statsRequests = new ArrayList<>();
        Set<String> statsUuids = new java.util.LinkedHashSet<>();
        Map<String, PlayerProfile> tagLookups = new java.util.LinkedHashMap<>();
        Map<String, LookupTracker.Attempt> tagAttempts = new java.util.LinkedHashMap<>();
        if (names == null) return scheduled;
        final long generation = fetchGeneration;
        final long session = HypixelFeatures.getInstance().getGameSnapshot().getSessionId();
        final StatsSelection selection = GameRegistry.detect(HypixelFeatures.getInstance().getGameSnapshot());
        if (selection == null) return scheduled;
        final StatScope scope = selection.game().scope();
        for (String name : names) {
            UUID uuid = PlayerUtils.getTrustedTabUuid(name);
            if (uuid == null || nickUtils.isNicked(name) || PlayerUtils.isNickedOrNpc(name)) continue;
            String key = name.toLowerCase(Locale.ROOT);
            scheduled.add(key);
            if (forceRefresh) {
                playerCache.clearPlayerStats(name);
                playerCache.clearPlayerTags(uuid.toString(), name);
                if (Mellow.auroraWinstreakService != null) Mellow.auroraWinstreakService.clearPlayer(uuid.toString().replace("-", ""));
            }
            if (matchProfiles.containsKey(key)) warmHiddenWinstreakCache(matchProfiles.get(key), scope);
            PlayerProfile identity = PlayerProfile.identity(uuid.toString(), name);
            if (shouldScanForInGameWarnings()) sendBlacklistAndTagAlerts(identity.withTags(matchTags.getOrDefault(name, TagReport.empty())), name, tagRequests.finished(key));
            if (shouldDeferRemoteTagLookup()) {
                LookupTracker.Attempt attempt = tagRequests.begin(key);
                if (attempt != null) { tagLookups.put(name, identity); tagAttempts.put(name, attempt); }
            }
            if (!config.tabStats && !config.printStats) {
                continue;
            }
            LookupTracker.Attempt statsAttempt = statsLookups.begin(key);
            if (statsAttempt == null) continue;
            statsUuids.add(uuid.toString());
            statsRequests.add(() -> {
                try {
                    ProfileFetchResult result = playerCache.getProfileForIdentity(name, uuid.toString(), false, selection);
                    PlayerProfile profile = result.getProfile();
                    boolean hasStats = profile != null && hasStatsForScope(profile, scope);
                    // Tag formatting may load a skin: finish it on the profile worker, never the client thread.
                    String chatMessage = hasStats && config.printStats && passesScopeFilters(profile, scope, config.minFkdr)
                        ? formatChatStats(profile, scope) : null;
                    mc.addScheduledTask(() -> {
                        if (!isCurrent(session, generation)) return;
                        statsLookups.finish(key, statsAttempt, hasStats || result.getFailureReason() == FetchFailureReason.MISSING_API_KEY);
                        if (!hasStats) {
                            maybeReportLiveFetchFailure(name, result);
                            return;
                        }
                        matchProfiles.put(key, profile);
                        TagReport tags = matchTags.getOrDefault(name,
                            tabStats.containsKey(name) ? tabStats.get(name).getTags() : TagReport.empty());
                        updateTabRow(name, profile.withTags(tags), scope);
                        if (passesScopeFilters(profile, scope, config.minFkdr)) {
                            if (config.printStats && chatMessage != null && (forceRefresh || alertedSources.add(key + ":stats"))) ChatUtils.sendMessage(chatMessage);
                        }
                        warmHiddenWinstreakCache(profile, scope);
                    });
                } catch (RuntimeException error) {
                    mc.addScheduledTask(() -> { if (isCurrent(session, generation)) statsLookups.finish(key, statsAttempt, false); });
                }
            });
        }
        if (playerCache.getSelectedProvider() != null && playerCache.getSelectedProvider().supportsBatch()) {
            AsyncExecutor.getInstance().profileIo(() -> {
                try { playerCache.prefetchStats(statsUuids); }
                finally { for (Runnable request : statsRequests) request.run(); }
            });
        } else for (Runnable request : statsRequests) AsyncExecutor.getInstance().profileIo(request);
        if (!tagLookups.isEmpty()) AsyncExecutor.getInstance().supplementalIo(() -> {
            Set<String> uuids = new java.util.LinkedHashSet<>();
            for (PlayerProfile identity : tagLookups.values()) uuids.add(identity.getUuid());
            Map<String, TagReport> reports;
            try { reports = playerCache.fetchTagReports(uuids); }
            catch (RuntimeException error) {
                reports = new java.util.LinkedHashMap<>();
                for (String uuid : uuids) reports.put(uuid.replace("-", ""), new TagReport(
                    java.util.Collections.emptyList(), java.util.Collections.singletonMap("Tags", "Request failed")));
            }
            final Map<String, TagReport> fetchedReports = reports;
            mc.addScheduledTask(() -> {
                if (!isCurrent(session, generation)) return;
                for (Map.Entry<String, PlayerProfile> entry : tagLookups.entrySet()) {
                    String name = entry.getKey();
                    TagReport report = fetchedReports.get(entry.getValue().getUuid().replace("-", ""));
                    if (report == null) report = new TagReport(java.util.Collections.emptyList(), java.util.Collections.singletonMap("Tags", "Missing result"));
                    tagRequests.finish(name.toLowerCase(Locale.ROOT), tagAttempts.get(name), report.getFailures().isEmpty());
                    report = report.retainFailedSources(matchTags.getOrDefault(name, TagReport.empty()));
                    matchTags.put(name, report);
                    PlayerProfile tagged = entry.getValue().withTags(report);
                    if (config.showAutomaticStatsErrors) for (Map.Entry<String, String> failure : report.getFailures().entrySet()) {
                        if (reportedTabFetchFailuresThisMatch.add("tags:" + failure.getKey()))
                            ChatUtils.sendMessage("§e" + failure.getKey() + " tags unavailable: " + failure.getValue());
                    }
                    TabStats row = tabStats.get(name);
                    TagReport displayTags = row == null ? report : report.retainFailedSources(row.getTags());
                    PlayerProfile displayProfile = matchProfiles.getOrDefault(name.toLowerCase(Locale.ROOT), entry.getValue());
                    updateTabRow(name, displayProfile.withTags(displayTags), scope);
                    if (shouldScanForInGameWarnings()) sendBlacklistAndTagAlerts(tagged, name, tagRequests.finished(name.toLowerCase(Locale.ROOT)));
                }
            });
        });
        return scheduled;
    }

    private void updateTabRow(String name, PlayerProfile profile, StatScope scope) {
        if (!config.tabStats) return;
        TabStats row = mergeTabRow(tabStats.get(name), profile, scope, config.minFkdr, shouldShowRemoteTagsInTab());
        if (row == null) tabStats.remove(name);
        else tabStats.put(name, row);
    }

    /** Apply the same display policy regardless of whether stats or tags arrive first. */
    static TabStats mergeTabRow(TabStats previous, PlayerProfile profile, StatScope scope,
                               int minFkdr, boolean showRemoteTags) {
        if (hasStatsForScope(profile, scope))
            return passesScopeFilters(profile, scope, minFkdr) ? profile.getTabStats(scope) : null;
        if (!showRemoteTags) return previous;
        return previous == null ? profile.getTabStats(scope) : previous.withTags(profile.getTags());
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

    /** Invalidate requests and retained data without forgetting warnings already shown this match. */
    public void resetLookups() {
        fetchGeneration++;
        statsLookups.clear();
        tagRequests.clear();
        matchTags.clear();
        matchProfiles.clear();
        winstreakLookups.clear();
        if (Mellow.auroraWinstreakService != null) Mellow.auroraWinstreakService.clearMatch();
    }

    public void resetInGameMatchWarningState() {
        resetLookups();
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

        BedwarsPlayer bedwarsPlayer = profile.getStats(GameRegistry.BEDWARS);
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
        if (Mellow.auroraWinstreakService.hasMatchWinstreak(compactUuid)) return;
        if (Mellow.auroraWinstreakService.hasCachedWinstreak(compactUuid)) {
            Mellow.auroraWinstreakService.pinForMatch(compactUuid, Mellow.auroraWinstreakService.getCachedWinstreak(compactUuid));
            return;
        }
        LookupTracker.Attempt attempt = winstreakLookups.begin(compactUuid);
        if (attempt == null) return;
        if (!Mellow.auroraWinstreakService.tryStartFetch(compactUuid)) {
            winstreakLookups.finish(compactUuid, attempt, false);
            return;
        }

        Object requestGeneration = Mellow.auroraWinstreakService.getGeneration(compactUuid);
        AsyncExecutor.getInstance().supplementalIo(() -> {
            try {
                int winstreak = Mellow.auroraWinstreakService.fetchWinstreakBlocking(
                    compactUuid
                );
                mc.addScheduledTask(() -> {
                    if (!winstreakLookups.finish(compactUuid, attempt, winstreak >= 0)) return;
                    if (Mellow.auroraWinstreakService.storeIfCurrent(compactUuid, requestGeneration, winstreak)) {
                        Mellow.auroraWinstreakService.pinForMatch(compactUuid, winstreak);
                    }
                });
            } catch (Exception e) {
                winstreakLookups.finish(compactUuid, attempt, false);
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
                Mellow.auroraWinstreakService.finishIfCurrent(compactUuid, requestGeneration);
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

    private static boolean hasStatsForScope(PlayerProfile profile, StatScope scope) {
        return profile.hasStats(scope);
    }

    private static boolean passesScopeFilters(PlayerProfile profile, StatScope scope, int minFkdr) {
        if (scope != StatScope.BEDWARS) return true;

        BedwarsPlayer player = profile.getStats(GameRegistry.BEDWARS);
        return player != null && player.getFkdr() >= minFkdr;
    }

    private String formatChatStats(PlayerProfile profile, StatScope scope) {
        return scope == StatScope.BEDWARS
            ? formatBedwarsChatStats(profile)
            : ChatStatsFormatter.format(profile, scope);
    }

    private String formatBedwarsChatStats(PlayerProfile profile) {
        BedwarsPlayer player = profile.getStats(GameRegistry.BEDWARS);
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
        BedwarsPlayer player = profile.getStats(GameRegistry.BEDWARS);
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
            player.getFinalDeaths(),
            profile.getFirstLogin()
        );

        if (tagsValue.endsWith(" ")) {
            return tagsValue.substring(0, tagsValue.length() - 1);
        }
        return tagsValue;
    }

    private void sendBlacklistAndTagAlerts(PlayerProfile profile, String tabPlayerName, boolean remoteComplete) {
        if (profile == null) return;
        UUID uuid = UUIDUtils.fromString(profile.getUuid());
        boolean ignored = tagIgnoreManager != null && tagIgnoreManager.isTagIgnored(uuid);
        Map<String, String> allSources = TagPolicy.warnings(profile.getTags(), config.printBlacklistTags, ignored);
        Map<String, String> sources = new java.util.LinkedHashMap<>(allSources);
        sources.entrySet().removeIf(entry -> !alertedSources.add(uuid + ":" + entry.getKey()));
        for (Map.Entry<String, String> source : sources.entrySet())
            ChatUtils.sendMessage("§c" + profile.getName() + " is tagged on " + FormattingUtils.formatTagSource(source.getKey(), false) + "§c for: " + source.getValue());

        BlacklistedPlayer local = blacklistManager.getBlacklistedPlayer(uuid);
        boolean blacklisted = local != null && alertedSources.add(uuid + ":local");
        boolean annoylisted = annoylistManager != null && annoylistManager.isAnnoylisted(uuid) && alertedSources.add(uuid + ":annoy");
        if (blacklisted) ChatUtils.sendMessage("§6" + profile.getName() + " §cis on your blacklist" + formatBlacklistReasonSuffix(local.getReason()));
        if (annoylisted) ChatUtils.sendMessage("§6" + profile.getName() + " §3is on your annoy list: " + normalizeReason(annoylistManager.getAnnoylistedPlayer(uuid).getReason()));
        if ((remoteComplete || !shouldDeferRemoteTagLookup())
            && shouldSendOutboundOpponentWarning(uuid, tabPlayerName, local != null, !allSources.isEmpty()))
            sendOutboundOpponentWarning(profile, tabPlayerName, local, local != null,
                allSources);
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
