package com.roxiun.mellow.stats.tntgames.bowspleef;

public final class BowSpleefPlayer {
    private final String formattedName;
    private final int wins;
    private final int deaths;

    public BowSpleefPlayer(String formattedName, int wins, int deaths) {
        this.formattedName = formattedName;
        this.wins = Math.max(0, wins);
        this.deaths = Math.max(0, deaths);
    }

    public String getFormattedName() { return formattedName; }
    public int getWins() { return wins; }
    public int getDeaths() { return deaths; }
    public double getRatio() { return deaths == 0 ? wins : (double) wins / deaths; }
}
