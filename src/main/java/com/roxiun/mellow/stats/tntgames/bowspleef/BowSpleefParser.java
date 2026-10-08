package com.roxiun.mellow.stats.tntgames.bowspleef;

import com.google.gson.JsonObject;
import com.roxiun.mellow.api.hypixel.HypixelPlayerData;
import com.roxiun.mellow.api.model.*;
import static com.roxiun.mellow.stats.JsonStats.*;

public final class BowSpleefParser {
    private BowSpleefParser() {}

    public static ProviderResult<BowSpleefPlayer> parse(HypixelPlayerData data, String mode) {
        JsonObject stats = getObject(getObject(data.player(), "stats"), "TNTGames");
        if (!hasKey(stats, "wins_bowspleef", "deaths_bowspleef")) {
            return ProviderResult.failure(FetchFailureReason.NO_PLAYER_DATA, "No Bow Spleef stats supplied");
        }
        return ProviderResult.success(new BowSpleefPlayer(data.formattedName(), getInt(stats, "wins_bowspleef", 0), getInt(stats, "deaths_bowspleef", 0)));
    }
}
