package com.roxiun.mellow.stats.duels;

import com.google.gson.*;
import com.roxiun.mellow.api.model.*;
import com.roxiun.mellow.api.hypixel.HypixelPlayerData;
import static com.roxiun.mellow.stats.JsonStats.*;

public final class DuelsParser {
    private DuelsParser() {}
    public static ProviderResult<DuelsPlayer> parse(HypixelPlayerData data, DuelsMode requestedMode) {
        try {
            JsonObject playerObject = data.player();
            String name = data.name();
            String formattedName = data.formattedName();

            JsonObject achievements = getObject(playerObject, "achievements");
            JsonObject stats = getObject(playerObject, "stats");
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
            String division = resolveDuelsDivision(duelsStats, achievements, mode);

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
        boolean found = false;
        for (String prefix : mode.getStatPrefixes()) {
            String key = prefix + "_" + statSuffix;
            if (duelsStats.has(key)) {
                total += getInt(duelsStats, key, 0);
                found = true;
            }
        }

        return found ? total : 0;
    }

    private static int getDuelsWinstreak(JsonObject duelsStats, DuelsMode mode) {
        if (duelsStats == null) {
            return 0;
        }

        if (mode == null || mode.isOverall()) {
            return getInt(duelsStats, "current_winstreak", 0);
        }

        int best = 0;
        boolean found = false;
        for (String prefix : mode.getStatPrefixes()) {
            int value = maxExistingInt(
                duelsStats,
                new String[] {
                    "current_winstreak_mode_" + prefix,
                    "current_" + prefix + "_winstreak",
                    prefix + "_winstreak"
                }
            );
            if (value != Integer.MIN_VALUE) {
                best = Math.max(best, value);
                found = true;
            }

            if (prefix.endsWith("_duel")) {
                String trimmed = prefix.substring(0, prefix.length() - "_duel".length());
                int trimmedValue = maxExistingInt(
                    duelsStats,
                    new String[] {
                        "current_" + trimmed + "_winstreak",
                        trimmed + "_winstreak"
                    }
                );
                if (trimmedValue != Integer.MIN_VALUE) {
                    best = Math.max(best, trimmedValue);
                    found = true;
                }
            }
        }

        for (String alias : mode.getDivisionPrefixes()) {
            int value = maxExistingInt(
                duelsStats,
                new String[] {
                    "current_" + alias + "_winstreak",
                    alias + "_winstreak"
                }
            );
            if (value != Integer.MIN_VALUE) {
                best = Math.max(best, value);
                found = true;
            }
        }

        if (found) {
            return best;
        }
        return getInt(duelsStats, "current_winstreak", 0);
    }

    private static String resolveDuelsDivision(
        JsonObject duelsStats,
        JsonObject achievements,
        DuelsMode mode
    ) {
        String explicit = findDuelsDivisionString(duelsStats, mode);
        if (!explicit.isEmpty()) {
            return explicit;
        }

        String tiered = resolveTieredDuelsDivision(duelsStats, mode);
        if (!tiered.isEmpty()) {
            return tiered;
        }

        Integer prestige = getDuelsPrestige(achievements, mode);
        if (prestige != null && prestige >= 0) {
            return formatDuelsDivision(prestige);
        }

        return "§7Unranked";
    }

    private static String findDuelsDivisionString(
        JsonObject duelsStats,
        DuelsMode mode
    ) {
        if (duelsStats == null) {
            return "";
        }

        String[] generalKeys = new String[] {
            "duels_division",
            "duels_title",
            "division",
            "title"
        };
        if (mode == null || mode.isOverall()) {
            return findFirstNonEmptyString(duelsStats, generalKeys);
        }

        for (String prefix : mode.getStatPrefixes()) {
            String candidate = findFirstNonEmptyString(
                duelsStats,
                new String[] {
                    prefix + "_division",
                    prefix + "_title",
                    "current_" + prefix + "_division",
                    "current_" + prefix + "_title"
                }
            );
            if (!candidate.isEmpty()) {
                return candidate;
            }
        }

        return findFirstNonEmptyString(duelsStats, generalKeys);
    }

    private static Integer getDuelsPrestige(
        JsonObject achievements,
        DuelsMode mode
    ) {
        if (achievements == null) {
            return null;
        }

        if (mode != null && !mode.isOverall()) {
            for (String key : mode.getTitlePrestigeKeys()) {
                Integer value = getNullableInt(achievements, key);
                if (value != null) {
                    return value;
                }
            }
        }

        return getNullableInt(achievements, "duels_title_prestige");
    }

    private static String resolveTieredDuelsDivision(
        JsonObject duelsStats,
        DuelsMode mode
    ) {
        if (duelsStats == null) {
            return "";
        }

        String resolved = resolveTieredDuelsDivisionForPrefixes(
            duelsStats,
            mode == null ? DuelsMode.OVERALL.getDivisionPrefixes() : mode.getDivisionPrefixes()
        );
        if (!resolved.isEmpty()) {
            return resolved;
        }

        if (mode != null && !mode.isOverall()) {
            return resolveTieredDuelsDivisionForPrefixes(
                duelsStats,
                DuelsMode.OVERALL.getDivisionPrefixes()
            );
        }

        return "";
    }

    private static String resolveTieredDuelsDivisionForPrefixes(
        JsonObject duelsStats,
        String[] prefixes
    ) {
        if (duelsStats == null || prefixes == null || prefixes.length == 0) {
            return "";
        }

        String[] rankKeys = new String[] {
            "rookie",
            "iron",
            "gold",
            "diamond",
            "master",
            "legend",
            "grandmaster",
            "godlike",
            "celestial",
            "divine",
            "ascended",
        };

        for (String prefix : prefixes) {
            if (prefix == null || prefix.isEmpty()) {
                continue;
            }

            int bestRank = -1;
            int bestProgress = -1;
            for (int i = 0; i < rankKeys.length; i++) {
                String key = prefix + "_" + rankKeys[i] + "_title_prestige";
                if (!duelsStats.has(key)) {
                    continue;
                }

                int progress = getInt(duelsStats, key, -1);
                if (progress < 0) {
                    continue;
                }

                bestRank = i;
                bestProgress = progress;
            }

            if (bestRank >= 0) {
                int tier = Math.min(Math.max(bestProgress, 0), 4) + 1;
                return formatDuelsDivisionWithTier(bestRank, tier);
            }
        }

        return "";
    }

    private static String formatDuelsDivision(int prestige) {
        if (prestige < 0) {
            return "§7Unranked";
        }

        String[] names = new String[] {
            "Rookie",
            "Iron",
            "Gold",
            "Diamond",
            "Master",
            "Legend",
            "Grandmaster",
            "Godlike",
            "Celestial",
            "Divine",
            "Ascended",
        };
        String[] colors = new String[] {
            "§7",
            "§f",
            "§6",
            "§b",
            "§2",
            "§d",
            "§4",
            "§5",
            "§3",
            "§c",
            "§e",
        };

        int index = Math.min(prestige, names.length - 1);
        return colors[index] + names[index];
    }

    private static String formatDuelsDivisionWithTier(int rankIndex, int tier) {
        String[] names = new String[] {
            "Rookie",
            "Iron",
            "Gold",
            "Diamond",
            "Master",
            "Legend",
            "Grandmaster",
            "Godlike",
            "Celestial",
            "Divine",
            "Ascended",
        };
        String[] colors = new String[] {
            "§7",
            "§f",
            "§6",
            "§b",
            "§2",
            "§d",
            "§4",
            "§5",
            "§3",
            "§c",
            "§e",
        };
        String[] roman = new String[] { "I", "II", "III", "IV", "V" };

        if (rankIndex < 0 || rankIndex >= names.length) {
            return "§7Unranked";
        }

        int tierIndex = Math.min(Math.max(tier, 1), 5) - 1;
        return colors[rankIndex] + names[rankIndex] + " " + roman[tierIndex];
    }
}
