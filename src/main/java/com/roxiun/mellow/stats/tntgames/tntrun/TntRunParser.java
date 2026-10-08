package com.roxiun.mellow.stats.tntgames.tntrun;

import com.google.gson.*;
import com.roxiun.mellow.api.model.*;
import com.roxiun.mellow.api.hypixel.HypixelPlayerData;
import static com.roxiun.mellow.stats.JsonStats.*;

public final class TntRunParser {
    private TntRunParser() {}
    public static ProviderResult<TntRunPlayer> parse(HypixelPlayerData data) {
        try {
            JsonObject playerObject = data.player();
            String name = data.name();
            String formattedName = data.formattedName();

            JsonObject stats = getObject(playerObject, "stats");
            if (!(hasObject(stats, "TNTGames") || hasKey(stats, "wins_tntrun", "tntgames_tnt_run_wins", "deaths_tntrun", "tntgames_tnt_run_deaths") || hasKey(playerObject, "wins_tntrun", "tntgames_tnt_run_wins", "deaths_tntrun", "tntgames_tnt_run_deaths"))) {
                return ProviderResult.failure(FetchFailureReason.NO_PLAYER_DATA, "No TNTGames stats supplied");
            }
            JsonObject tntGamesStats = getObject(stats, "TNTGames");

            int wins = maxExistingIntAcrossObjects(
                new JsonObject[] { tntGamesStats, stats, playerObject },
                new String[] { "wins_tntrun", "tntgames_tnt_run_wins" }
            );
            if (wins == Integer.MIN_VALUE) {
                wins = 0;
            }

            int deaths = maxExistingIntAcrossObjects(
                new JsonObject[] { tntGamesStats, stats, playerObject },
                new String[] {
                    "deaths_tntrun",
                    "deaths_tourney_tnt_run_0",
                    "tntgames_tnt_run_deaths"
                }
            );
            if (deaths == Integer.MIN_VALUE) {
                deaths = 0;
            }

            int bestRecord = maxExistingIntAcrossObjects(
                new JsonObject[] { tntGamesStats, stats, playerObject },
                new String[] { "record_tntrun", "tntgames_tnt_run_record" }
            );
            if (bestRecord == Integer.MIN_VALUE) {
                bestRecord = 0;
            }

            return ProviderResult.success(
                new TntRunPlayer(
                    name,
                    formattedName,
                    wins,
                    deaths,
                    bestRecord
                )
            );
        } catch (Exception e) {
            return ProviderResult.failure(
                FetchFailureReason.PARSE_ERROR,
                e.getMessage()
            );
        }
    }

}
