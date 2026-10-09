package com.roxiun.mellow.stats.bedwars;

import com.google.gson.*;
import com.roxiun.mellow.api.model.*;
import com.roxiun.mellow.api.hypixel.HypixelPlayerData;
import com.roxiun.mellow.util.formatting.FormattingUtils;
import static com.roxiun.mellow.stats.JsonStats.*;

public final class BedwarsParser {
    private BedwarsParser() {}
    public static ProviderResult<BedwarsPlayer> parse(HypixelPlayerData data, BedwarsMode mode) {
        try {
            JsonObject playerObject = data.player();
            String name = data.name();
            String formattedName = data.formattedName();

            JsonObject achievements = getObject(playerObject, "achievements");
            int stars = getInt(achievements, "bedwars_level", 0);

            JsonObject stats = getObject(playerObject, "stats");
            if (!(hasObject(stats, "Bedwars"))) {
                return ProviderResult.failure(FetchFailureReason.NO_PLAYER_DATA, "No Bedwars stats supplied");
            }
            JsonObject bedwarsStats = getObject(stats, "Bedwars");

            if (mode != BedwarsMode.OVERALL && mode.getStatPrefixes().stream().noneMatch(prefix ->
                hasKey(bedwarsStats, prefix + "wins_bedwars", prefix + "losses_bedwars",
                    prefix + "final_kills_bedwars", prefix + "final_deaths_bedwars", prefix + "beds_broken_bedwars"))) {
                return ProviderResult.failure(FetchFailureReason.NO_PLAYER_DATA, "No stats supplied for " + mode.getFullName());
            }

            int finalKills = stat(bedwarsStats, mode, "final_kills_bedwars");
            int finalDeaths = stat(bedwarsStats, mode, "final_deaths_bedwars");
            double fkdr = finalDeaths == 0
                ? finalKills
                : (double) finalKills / finalDeaths;

            Integer winstreakValue = mode == BedwarsMode.OVERALL ? getNullableInt(bedwarsStats, "winstreak") : modeWinstreak(bedwarsStats, mode);
            int winstreak = winstreakValue == null ? 0 : winstreakValue;
            boolean hasWinstreakData = winstreakValue != null;
            int wins = stat(bedwarsStats, mode, "wins_bedwars");
            int losses = stat(bedwarsStats, mode, "losses_bedwars");
            int bedsBroken = stat(bedwarsStats, mode, "beds_broken_bedwars");
            int bedsLost = stat(bedwarsStats, mode, "beds_lost_bedwars");
            int finals = finalKills;

            return ProviderResult.success(
                new BedwarsPlayer(
                    name,
                    formattedName,
                    FormattingUtils.formatStars(String.valueOf(stars)),
                    fkdr,
                    winstreak,
                    hasWinstreakData,
                    finalKills,
                    finalDeaths,
                    wins,
                    losses,
                    bedsBroken,
                    bedsLost,
                    finals
                )
            );
        } catch (Exception e) {
            return ProviderResult.failure(
                FetchFailureReason.PARSE_ERROR,
                e.getMessage()
            );
        }
    }

    private static int stat(JsonObject stats, BedwarsMode mode, String key) {
        int total = 0;
        for (String prefix : mode.getStatPrefixes()) total += getInt(stats, prefix + key, 0);
        return total;
    }
    private static Integer modeWinstreak(JsonObject stats, BedwarsMode mode) {
        // A streak cannot be summed across queues; aggregate modes have no single streak.
        return mode.getStatPrefixes().size() == 1 ? getNullableInt(stats, mode.getStatPrefixes().get(0) + "winstreak") : null;
    }
}
