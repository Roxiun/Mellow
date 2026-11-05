package com.roxiun.mellow.stats.murdermystery;

import com.google.gson.JsonObject;
import com.roxiun.mellow.api.hypixel.HypixelPlayerData;
import com.roxiun.mellow.api.model.*;
import static com.roxiun.mellow.stats.JsonStats.*;

public final class MurderMysteryParser {
    private MurderMysteryParser() {}

    public static ProviderResult<MurderMysteryPlayer> parse(HypixelPlayerData data, String mode) {
        JsonObject stats = getObject(getObject(data.player(), "stats"), "MurderMystery");
        String suffix = "overall".equals(mode) ? "" : "_MURDER_" + mode.toUpperCase(java.util.Locale.ROOT);
        if (!hasKey(stats, "games" + suffix, "wins" + suffix, "kills" + suffix)) {
            return ProviderResult.failure(FetchFailureReason.NO_PLAYER_DATA, "No Murder Mystery stats supplied for " + mode);
        }
        // Infection tracks kills by role instead of supplying kills_MURDER_INFECTION.
        int kills = "infection".equals(mode)
            ? getInt(stats, "kills_as_infected" + suffix, 0) + getInt(stats, "kills_as_survivor" + suffix, 0)
            : getInt(stats, "kills" + suffix, 0);
        return ProviderResult.success(new MurderMysteryPlayer(data.formattedName(),
            getInt(stats, "wins" + suffix, 0), kills, getInt(stats, "games" + suffix, 0)));
    }
}
