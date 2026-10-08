package com.roxiun.mellow.stats.tntgames.tnttag;

import com.google.gson.JsonObject;
import com.roxiun.mellow.api.hypixel.HypixelPlayerData;
import com.roxiun.mellow.api.model.*;
import static com.roxiun.mellow.stats.JsonStats.*;

public final class TntTagParser {
    private TntTagParser() {}

    public static ProviderResult<TntTagPlayer> parse(HypixelPlayerData data, String mode) {
        JsonObject stats = getObject(getObject(data.player(), "stats"), "TNTGames");
        if (!hasKey(stats, "wins_tntag", "deaths_tntag")) {
            return ProviderResult.failure(FetchFailureReason.NO_PLAYER_DATA, "No TNT Tag stats supplied");
        }
        return ProviderResult.success(new TntTagPlayer(data.formattedName(), getInt(stats, "wins_tntag", 0)));
    }
}
