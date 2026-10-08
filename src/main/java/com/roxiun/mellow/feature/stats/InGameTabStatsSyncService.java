package com.roxiun.mellow.feature.stats;

import com.roxiun.mellow.config.MellowOneConfig;
import com.roxiun.mellow.data.TabStats;
import com.roxiun.mellow.feature.nicks.NickUtils;
import com.roxiun.mellow.gamestate.GameSnapshot;
import com.roxiun.mellow.util.player.PlayerUtils;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import net.minecraft.client.Minecraft;
import net.minecraft.client.network.NetworkPlayerInfo;

public class InGameTabStatsSyncService {

    private long sessionId = -1;
    private String settings = "";
    private static final long SCAN_INTERVAL_MS = 1_500L;

    private final Minecraft mc = Minecraft.getMinecraft();
    private final StatsChecker statsChecker;
    private final NickUtils nickUtils;
    private final MellowOneConfig config;
    private final Map<String, TabStats> tabStats;

    private boolean inSupportedMatch;
    private long lastScanMillis;
    private GameSnapshot currentSnapshot;

    public InGameTabStatsSyncService(
        StatsChecker statsChecker,
        NickUtils nickUtils,
        MellowOneConfig config,
        Map<String, TabStats> tabStats
    ) {
        this.statsChecker = statsChecker;
        this.nickUtils = nickUtils;
        this.config = config;
        this.tabStats = tabStats;
    }

    public synchronized void onSnapshotUpdate(GameSnapshot snapshot) {
        if (snapshot != null && sessionId != snapshot.getSessionId()) {
            sessionId = snapshot.getSessionId();
            tabStats.clear();
            resetTracking();
        }
        String nextSettings = config.statsProvider + "|" + config.hypixelApiKey + "|" + config.getCoralApiKey()
            + "|" + config.isCoralEnabled() + "|" + config.xadia + "|" + config.xadiaKey + "|" + config.xadiaVerifiedOnly
            + "|" + config.tabStats + "|" + config.printStats + "|" + config.printBlacklistTags;
        if (!settings.equals(nextSettings)) {
            settings = nextSettings;
            resetTracking();
        }
        currentSnapshot = snapshot;
        boolean supportedNow = isSupportedMatch(snapshot);
        if (!supportedNow) {
            if (inSupportedMatch) {
                tabStats.clear();
            }
            resetTracking();
            return;
        }

        long now = System.currentTimeMillis();
        if (!inSupportedMatch) {
            inSupportedMatch = true;
            lastScanMillis = 0L;
            statsChecker.resetInGameMatchWarningState();

            runScan(true, false);
            return;
        }

        if (now - lastScanMillis < SCAN_INTERVAL_MS) {
            return;
        }

        runScan(false, false);
    }

    public boolean isSupportedMatch(GameSnapshot snapshot) {
        return StatScopeResolver.isSupportedLiveMatch(snapshot);
    }

    public synchronized GameSnapshot getCurrentSnapshot() {
        return currentSnapshot;
    }

    private void runScan(boolean clearBeforeFetch, boolean forceRefresh) {
        lastScanMillis = System.currentTimeMillis();
        boolean shouldScanForWarnings =
            config != null && statsChecker.shouldScanForInGameWarnings();
        if (config == null || (!config.tabStats && !config.printStats && !shouldScanForWarnings)) {
            if (clearBeforeFetch) {
                tabStats.clear();
            }
            return;
        }

        List<String> tabPlayers = getTabPlayerNames();
        if (!tabPlayers.isEmpty()) {
            nickUtils.updateNickedPlayers(tabPlayers);
        }

        List<String> pendingPlayers = new ArrayList<>();
        for (String playerName : tabPlayers) {
            if (playerName == null || playerName.isEmpty()) {
                continue;
            }

            String normalized = playerName.toLowerCase(Locale.ROOT);

            pendingPlayers.add(playerName);
        }

        statsChecker.fetchTabStatsForPlayers(
            pendingPlayers,
            clearBeforeFetch,
            forceRefresh
        );
    }

    private List<String> getTabPlayerNames() {
        if (mc.getNetHandler() == null || mc.getNetHandler().getPlayerInfoMap() == null) {
            return new ArrayList<>();
        }

        Set<String> unique = new LinkedHashSet<>();
        for (NetworkPlayerInfo info : mc.getNetHandler().getPlayerInfoMap()) {
            if (info == null || info.getGameProfile() == null) {
                continue;
            }
            if (PlayerUtils.isObfuscatedTabEntry(info)) {
                continue;
            }

            String name = info.getGameProfile().getName();
            if (name == null || name.trim().isEmpty()) {
                continue;
            }
            unique.add(name);
        }

        return new ArrayList<>(unique);
    }

    public synchronized void forceRefresh() {
        if (currentSnapshot != null && isSupportedMatch(currentSnapshot)) {
            runScan(false, true);
        }
    }

    public synchronized void clear() { resetTracking(); }

    private void resetTracking() {
        inSupportedMatch = false;
        lastScanMillis = 0L;
        statsChecker.resetInGameMatchWarningState();
    }
}
