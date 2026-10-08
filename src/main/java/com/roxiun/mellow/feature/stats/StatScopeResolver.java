package com.roxiun.mellow.feature.stats;

import com.roxiun.mellow.api.provider.model.StatScope;
import com.roxiun.mellow.gamestate.GameSnapshot;
import java.util.List;
import java.util.Locale;
import net.hypixel.data.type.GameType;

public final class StatScopeResolver {

    private StatScopeResolver() {}

    public static StatScope resolveSupportedScope(GameSnapshot snapshot) {
        if (snapshot == null || !snapshot.isOnHypixel()) {
            return null;
        }

        GameType gameType = snapshot.getGameType();
        if (gameType == GameType.BEDWARS) {
            return StatScope.BEDWARS;
        }
        if (gameType == GameType.SKYWARS) {
            return StatScope.SKYWARS;
        }
        if (gameType == GameType.DUELS) {
            return StatScope.DUELS;
        }
        if (gameType == GameType.BUILD_BATTLE) {
            return StatScope.BUILD_BATTLE;
        }
        if (gameType == GameType.TNTGAMES && isTntRun(snapshot)) {
            return StatScope.TNT_RUN;
        }

        return null;
    }

    public static StatScope resolveInGameScope(GameSnapshot snapshot) {
        StatScope scope = resolveSupportedScope(snapshot);
        return scope == null ? StatScope.BEDWARS : scope;
    }

    public static boolean isSupportedLiveMatch(GameSnapshot snapshot) {
        if (snapshot == null || !snapshot.isOnHypixel()) {
            return false;
        }
        return snapshot.getPhase() == com.roxiun.mellow.gamestate.GamePhase.LIVE
            && resolveSupportedScope(snapshot) != null;
    }

    public static boolean isTntRun(GameSnapshot snapshot) {
        if (snapshot == null) {
            return false;
        }

        if (containsTntRunToken(snapshot.getMode())) {
            return true;
        }
        if (containsTntRunToken(snapshot.getMap())) {
            return true;
        }
        if (containsTntRunToken(snapshot.getScoreboardTitle())) {
            return true;
        }

        List<String> lines = snapshot.getScoreboardLines();
        if (lines == null || lines.isEmpty()) {
            return false;
        }

        for (String line : lines) {
            if (containsTntRunToken(line)) {
                return true;
            }
        }

        return false;
    }

    private static boolean containsTntRunToken(String value) {
        String normalized = normalize(value);
        return normalized.contains("tntrun") || normalized.contains("tnt run");
    }

    private static String normalize(String value) {
        if (value == null || value.isEmpty()) {
            return "";
        }

        return value
            .replaceAll("§.", "")
            .toLowerCase(Locale.ROOT)
            .replace('_', ' ')
            .replace('-', ' ')
            .replaceAll("\\s+", " ")
            .trim();
    }
}
