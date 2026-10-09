package com.roxiun.mellow.stats.duels;

import com.google.gson.*;
import com.roxiun.mellow.api.model.*;
import com.roxiun.mellow.api.hypixel.HypixelPlayerData;
import static com.roxiun.mellow.stats.JsonStats.*;

public final class DuelsParser {
    private DuelsParser() {}
    public static ProviderResult<DuelsPlayer> parse(HypixelPlayerData data, DuelsMode requestedMode) {
        try {
            String name = data.name();
            String formattedName = data.formattedName();

            JsonObject stats = getObject(data.player(), "stats");
            if (!(hasObject(stats, "Duels"))) {
                return ProviderResult.failure(FetchFailureReason.NO_PLAYER_DATA, "No Duels stats supplied");
            }
            JsonObject duelsStats = getObject(stats, "Duels");

            DuelsMode mode = requestedMode == null ? DuelsMode.OVERALL : requestedMode;
            if (
                mode != DuelsMode.OVERALL &&
                !hasModeSpecificDuelsStats(duelsStats, mode)
            ) {
                return ProviderResult.failure(FetchFailureReason.NO_PLAYER_DATA, "No stats supplied for " + mode.getDisplayName());
            }

            int kills = getDuelsStat(duelsStats, mode, "kills");
            int deaths = getDuelsStat(duelsStats, mode, "deaths");
            int wins = getDuelsStat(duelsStats, mode, "wins");
            int losses = getDuelsStat(duelsStats, mode, "losses");
            int winstreak = getDuelsWinstreak(duelsStats, mode);
            String division = DuelsDivision.format(wins, mode);

            return ProviderResult.success(
                new DuelsPlayer(
                    name,
                    formattedName,
                    mode,
                    division,
                    kills,
                    deaths,
                    wins,
                    losses,
                    winstreak
                )
            );
        } catch (Exception e) {
            return ProviderResult.failure(
                FetchFailureReason.PARSE_ERROR,
                e.getMessage()
            );
        }
    }

    private static boolean hasModeSpecificDuelsStats(
        JsonObject duelsStats,
        DuelsMode mode
    ) {
        if (duelsStats == null || mode == null || mode.isOverall()) {
            return true;
        }

        for (String prefix : mode.getStatPrefixes()) {
            if (
                duelsStats.has(prefix + "_wins") ||
                duelsStats.has(prefix + "_losses") ||
                duelsStats.has(prefix + "_kills") ||
                duelsStats.has(prefix + "_deaths")
            ) {
                return true;
            }
        }

        return false;
    }

    private static int getDuelsStat(
        JsonObject duelsStats,
        DuelsMode mode,
        String statSuffix
    ) {
        if (duelsStats == null || statSuffix == null || statSuffix.isEmpty()) {
            return 0;
        }

        if (mode == null || mode.isOverall()) {
            return getInt(duelsStats, statSuffix, 0);
        }

        int total = 0;
        for (String prefix : mode.getStatPrefixes()) {
            String key = prefix + "_" + statSuffix;
            total += getInt(duelsStats, key, 0);
            if (mode == DuelsMode.BRIDGE && (statSuffix.equals("kills") || statSuffix.equals("deaths"))) {
                total += getInt(duelsStats, prefix + "_bridge_" + statSuffix, 0);
            }
        }

        return total;
    }

    // -1 means the API did not supply this streak; it is not a zero streak.
    private static int getDuelsWinstreak(JsonObject stats, DuelsMode mode) {
        if (mode.isOverall()) {
            return getInt(stats, "currentStreak", getInt(stats, "current_winstreak", -1));
        }
        if (mode.getStatPrefixes().length == 1) {
            return getInt(stats, "current_winstreak_mode_" + mode.getStatPrefixes()[0], -1);
        }
        // A family streak is distinct from the maximum of its individual queues.
        for (String family : mode.getDivisionPrefixes()) {
            String key = "current_" + family + "_winstreak";
            if (stats.has(key)) return getInt(stats, key, -1);
        }
        return -1;
    }
}
