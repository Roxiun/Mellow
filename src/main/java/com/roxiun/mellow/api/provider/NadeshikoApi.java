package com.roxiun.mellow.api.provider;

import com.roxiun.mellow.api.bedwars.BedwarsPlayer;
import com.roxiun.mellow.api.mojang.MojangApi;
import com.roxiun.mellow.api.util.HypixelApiUtils;
import com.roxiun.mellow.util.player.PlayerUtils;
import java.io.IOException;

public class NadeshikoApi implements StatsProvider {

    private final MojangApi mojangApi;

    public NadeshikoApi(MojangApi mojangApi) {
        this.mojangApi = mojangApi;
    }

    @Override
    public String fetchPlayerData(String uuid) {
        return HypixelApiUtils.fetchPlayerData(
            "https://nadeshiko.io/player/" + uuid + "/network",
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36"
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

        return HypixelApiUtils.parsePlayerData(stjson, "Nadeshiko");
    }
}
