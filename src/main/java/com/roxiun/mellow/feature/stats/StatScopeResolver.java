package com.roxiun.mellow.feature.stats;

import com.roxiun.mellow.stats.StatScope;
import com.roxiun.mellow.gamestate.GameSnapshot;

public final class StatScopeResolver {

    private StatScopeResolver() {}

    public static StatScope resolveSupportedScope(GameSnapshot snapshot) {
        if (snapshot == null || !snapshot.isOnHypixel()) {
            return null;
        }

        com.roxiun.mellow.stats.StatsSelection selection = com.roxiun.mellow.stats.GameRegistry.detect(snapshot);
        return selection == null ? null : selection.game().scope();
    }

    public static StatScope resolveInGameScope(GameSnapshot snapshot) {
        return resolveSupportedScope(snapshot);
    }

    public static boolean isSupportedLiveMatch(GameSnapshot snapshot) {
        if (snapshot == null || !snapshot.isOnHypixel()) {
            return false;
        }
        return snapshot.getPhase() == com.roxiun.mellow.gamestate.GamePhase.LIVE
            && resolveSupportedScope(snapshot) != null;
    }

    public static boolean isTntRun(GameSnapshot snapshot) {
        return com.roxiun.mellow.stats.tntgames.tntrun.TntRunGame.matchesTntRun(snapshot);
    }
}
