package com.roxiun.mellow.stats.buildbattle;

import com.google.gson.*;
import com.roxiun.mellow.api.model.*;
import com.roxiun.mellow.api.hypixel.HypixelPlayerData;
import static com.roxiun.mellow.stats.JsonStats.*;

public final class BuildBattleParser {
    private BuildBattleParser() {}
    public static ProviderResult<BuildBattlePlayer> parse(HypixelPlayerData data) {
        try {
            JsonObject playerObject = data.player();
            String name = data.name();
            String formattedName = data.formattedName();

            JsonObject stats = getObject(playerObject, "stats");
            if (!(hasObject(stats, "BuildBattle") || hasKey(stats, "score", "wins", "buildbattle_build_battle_score", "buildbattle_wins", "buildbattle_build_battle_wins") || hasKey(playerObject, "score", "wins", "buildbattle_build_battle_score", "buildbattle_wins", "buildbattle_build_battle_wins"))) {
                return ProviderResult.failure(FetchFailureReason.NO_PLAYER_DATA, "No BuildBattle stats supplied");
            }
            JsonObject buildBattleStats = getObject(stats, "BuildBattle");

            int score = maxExistingIntAcrossObjects(
                new JsonObject[] { buildBattleStats, stats, playerObject },
                new String[] { "score", "buildbattle_build_battle_score" }
            );
            if (score == Integer.MIN_VALUE) {
                score = 0;
            }

            JsonObject[] buildBattleObjects = new JsonObject[] {
                buildBattleStats,
                stats,
                playerObject,
            };

            int wins = maxExistingIntAcrossObjects(
                buildBattleObjects,
                new String[] {
                    "wins",
                    "buildbattle_wins",
                    "buildbattle_build_battle_wins"
                }
            );
            if (wins == Integer.MIN_VALUE) {
                wins = sumBestPerKeyAcrossObjects(
                    buildBattleObjects,
                    new String[] {
                        "wins_solo_normal",
                        "wins_teams_normal",
                        "wins_guess_the_build",
                        "wins_guess_the_build_teams",
                        "wins_solo_pro",
                        "wins_speed_builders"
                    }
                );
            }
            if (wins == Integer.MIN_VALUE) {
                wins = 0;
            }

            int gamesPlayed = maxExistingIntAcrossObjects(
                buildBattleObjects,
                new String[] { "games_played", "buildbattle_games_played" }
            );
            if (gamesPlayed == Integer.MIN_VALUE) {
                gamesPlayed = sumBestPerKeyAcrossObjects(
                    buildBattleObjects,
                    new String[] {
                        "games_played_solo_normal",
                        "games_played_teams_normal",
                        "games_played_guess_the_build",
                        "games_played_guess_the_build_teams",
                        "games_played_solo_pro",
                        "games_played_speed_builders"
                    }
                );
            }
            if (gamesPlayed == Integer.MIN_VALUE) {
                gamesPlayed = 0;
            }

            return ProviderResult.success(
                new BuildBattlePlayer(
                    name,
                    formattedName,
                    score,
                    wins,
                    gamesPlayed
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
