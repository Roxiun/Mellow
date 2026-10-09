package com.roxiun.mellow.feature.stats;

import com.roxiun.mellow.stats.StatScope;
import com.roxiun.mellow.gamestate.GameSnapshot;

public final class StatScopeResolver {

    private StatScopeResolver() {}

    public static StatScope resolveSupportedScope(GameSnapshot snapshot) {
        if (snapshot == null || !snapshot.isOnHypixel()) {
            return null;
        }

        return snapshot.getStatsGame() == null ? null : snapshot.getStatsGame().scope();
    }

    public static StatScope resolveInGameScope(GameSnapshot snapshot) {
        return resolveSupportedScope(snapshot);
    }

    public static boolean isSupportedStatsSession(GameSnapshot snapshot) {
        if (snapshot == null || !snapshot.isOnHypixel()) {
            return false;
        }
        StatScope scope = resolveSupportedScope(snapshot);
        // Other games need a game server, not an inferred match-start signal.
        return scope != null && !snapshot.isLobby() && (scope == StatScope.BEDWARS
            ? snapshot.isInBedwarsMatch()
            : snapshot.getServerName() != null && !snapshot.getServerName().isEmpty());
    }

    public static boolean isTntRun(GameSnapshot snapshot) {
        return resolveSupportedScope(snapshot) == StatScope.TNT_RUN;
    }
}
