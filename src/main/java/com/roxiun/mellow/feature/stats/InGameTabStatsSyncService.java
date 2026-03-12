package com.roxiun.mellow.feature.stats;

import com.roxiun.mellow.Mellow;
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
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.network.NetworkPlayerInfo;

public class InGameTabStatsSyncService {

    private static final long DELTA_WINDOW_MS = 30_000L;
    private static final long SCAN_INTERVAL_MS = 1_500L;

    private final Minecraft mc = Minecraft.getMinecraft();
    private final StatsChecker statsChecker;
    private final NickUtils nickUtils;
    private final MellowOneConfig config;
    private final Map<String, TabStats> tabStats;
    private final Set<String> fetchedOrScheduledThisMatch =
        ConcurrentHashMap.newKeySet();

    private boolean inSupportedMatch;
    private long matchStartMillis;
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
        currentSnapshot = snapshot;
        if (!Mellow.isEnabled()) {
            if (inSupportedMatch) {
                tabStats.clear();
            }
            resetTracking();
            return;
        }
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
            matchStartMillis = now;
            lastScanMillis = 0L;
            fetchedOrScheduledThisMatch.clear();
            statsChecker.resetInGameMatchWarningState();

            runScan(true, false);
            return;
        }

        if (now - matchStartMillis > DELTA_WINDOW_MS) {
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
        if (config == null || (!config.tabStats && !shouldScanForWarnings)) {
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
            if (!forceRefresh && fetchedOrScheduledThisMatch.contains(normalized)) {
                continue;
            }

            pendingPlayers.add(playerName);
        }

        List<String> scheduledPlayers = statsChecker.fetchTabStatsForPlayers(
            pendingPlayers,
            clearBeforeFetch,
            forceRefresh
        );
        fetchedOrScheduledThisMatch.addAll(scheduledPlayers);
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
        fetchedOrScheduledThisMatch.clear();
        if (currentSnapshot != null && isSupportedMatch(currentSnapshot)) {
            runScan(true, true);
        }
    }

    private void resetTracking() {
        inSupportedMatch = false;
        matchStartMillis = 0L;
        lastScanMillis = 0L;
        fetchedOrScheduledThisMatch.clear();
        statsChecker.resetInGameMatchWarningState();
    }
}
