package com.roxiun.mellow.feature.bedwars;

import com.roxiun.mellow.gamestate.ScoreboardObservation;
import com.roxiun.mellow.gamestate.GameSnapshot;
import java.util.List;
import java.util.Locale;

public class BedwarsTimerService {

    private GameSnapshot lastSnapshot;
    private BedwarsTimerState state = BedwarsTimerState.empty();

    public BedwarsTimerState getState() {
        return state;
    }

    public void reset() {
        state = BedwarsTimerState.empty();
        lastSnapshot = null;
    }

    public void update(GameSnapshot snapshot) {
        if (snapshot == null || !snapshot.isInBedwarsSession()) {
            reset();
            return;
        }

        if (lastSnapshot == snapshot) return;
        lastSnapshot = snapshot;
        List<String> sidebarLines = snapshot.getScoreboardLines();
        ScoreboardObservation stage = snapshot.getObservation();
        if (stage.stageSeconds < 0) {
            return;
        }

        int elapsed = stage.stageSeconds - stage.stageRemaining;
        String modeGroup = resolveModeGroup(snapshot.getMode(), sidebarLines);

        SpawnState emeraldState = calculateSpawns(
            elapsed + 1,
            31,
            new int[] { 12 * 60, 24 * 60 },
            new IntervalProvider() {
                @Override
                public int intervalForTier(int tier) {
                    return getEmeraldInterval(modeGroup, tier);
                }
            }
        );

        SpawnState diamondState = calculateSpawns(
            elapsed + 1,
            1,
            new int[] { 6 * 60, 18 * 60 },
            new IntervalProvider() {
                @Override
                public int intervalForTier(int tier) {
                    return getDiamondInterval(tier);
                }
            }
        );

        state = new BedwarsTimerState(
            emeraldState.count,
            emeraldState.next,
            diamondState.count,
            diamondState.next
        );
    }

    private String resolveModeGroup(String locationMode, List<String> lines) {
        String mode = locationMode == null ? "" : locationMode.toUpperCase(Locale.ROOT);

        if (mode.contains("_EIGHT_")) {
            return "eight";
        }
        if (mode.contains("_FOUR_")) {
            return "four";
        }

        for (String line : lines) {
            if (line.contains("Pink:")) {
                return "eight";
            }
        }

        return "four";
    }

    private int getEmeraldInterval(String modeGroup, int tier) {
        if ("eight".equals(modeGroup)) {
            if (tier == 1) {
                return 65;
            }
            if (tier == 2) {
                return 50;
            }
            return 35;
        }

        if (tier == 1) {
            return 55;
        }
        if (tier == 2) {
            return 40;
        }
        return 27;
    }

    private int getDiamondInterval(int tier) {
        if (tier == 1) {
            return 30;
        }
        if (tier == 2) {
            return 23;
        }
        return 12;
    }

    private SpawnState calculateSpawns(
        int elapsedTime,
        int firstSpawn,
        int[] upgrades,
        IntervalProvider intervalProvider
    ) {
        if (elapsedTime < firstSpawn) {
            return new SpawnState(0, firstSpawn - elapsedTime);
        }

        int count = 1;
        int lastSpawn = firstSpawn;
        int upgradeIndex = 0;
        int nextUpgrade = upgrades.length > 0 ? upgrades[0] : Integer.MAX_VALUE;

        while (true) {
            int tier = getTier(lastSpawn, upgrades);
            int interval = intervalProvider.intervalForTier(tier);
            int nextSpawn = lastSpawn + interval;

            if (nextUpgrade <= elapsedTime && nextUpgrade < nextSpawn) {
                count++;
                lastSpawn = nextUpgrade;
                upgradeIndex++;
                nextUpgrade = upgradeIndex < upgrades.length
                    ? upgrades[upgradeIndex]
                    : Integer.MAX_VALUE;
                continue;
            }

            if (nextSpawn > elapsedTime) {
                return new SpawnState(count, Math.max(1, nextSpawn - elapsedTime));
            }

            count++;
            lastSpawn = nextSpawn;
        }
    }

    private int getTier(int time, int[] upgrades) {
        if (upgrades.length > 1 && time >= upgrades[1]) {
            return 3;
        }
        if (upgrades.length > 0 && time >= upgrades[0]) {
            return 2;
        }
        return 1;
    }

    private interface IntervalProvider {
        int intervalForTier(int tier);
    }

    private static class SpawnState {

        private final int count;
        private final int next;

        private SpawnState(int count, int next) {
            this.count = count;
            this.next = next;
        }
    }

}
