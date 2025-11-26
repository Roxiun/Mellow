package com.strawberry.statsify.api.provider;

import com.strawberry.statsify.api.bedwars.BedwarsPlayer;
import com.strawberry.statsify.api.mojang.MojangApi;
import com.strawberry.statsify.api.util.HypixelApiUtils;
import com.strawberry.statsify.util.player.PlayerUtils;
import java.io.IOException;

public class AbyssApi implements StatsProvider {

    private final MojangApi mojangApi;

    public AbyssApi(MojangApi mojangApi) {
        this.mojangApi = mojangApi;
    }

    @Override
    public String fetchPlayerData(String uuid) {
        return HypixelApiUtils.fetchPlayerData(
            "http://api.abyssoverlay.com/player?uuid=" + uuid,
            "node-ao/2.0.3"
        );
    }

    @Override
    public BedwarsPlayer fetchPlayerStats(String playerName)
        throws IOException {
        String uuid = PlayerUtils.getUUIDFromPlayerName(playerName);
        if (uuid == null) {
            uuid = mojangApi.fetchUUID(playerName);
            if (uuid.equals("ERROR")) {
                return null;
            }
        }
        String stjson = fetchPlayerData(uuid);
        if (stjson == null || stjson.isEmpty()) {
            return null;
        }

        return HypixelApiUtils.parsePlayerData(stjson, "Abyss");
    }
}
