package com.strawberry.statsify.api.provider;

import com.strawberry.statsify.api.bedwars.BedwarsPlayer;
import java.io.IOException;

public interface StatsProvider {
    BedwarsPlayer fetchPlayerStats(String playerName) throws IOException;

    String fetchPlayerData(String uuid);
}
