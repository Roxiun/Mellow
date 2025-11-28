package com.roxiun.mellow.api.provider;

import com.roxiun.mellow.api.bedwars.BedwarsPlayer;
import java.io.IOException;

public interface StatsProvider {
    BedwarsPlayer fetchPlayerStats(String playerName) throws IOException;

    String fetchPlayerData(String uuid);
}
