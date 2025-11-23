package com.strawberry.statsify.api;

import java.io.IOException;

public interface StatsProvider {
    String fetchPlayerStats(String playerName) throws IOException;
    String fetchPlayerData(String uuid);
}
