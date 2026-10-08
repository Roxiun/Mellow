package com.roxiun.mellow.stats.tntgames.tnttag;

public final class TntTagPlayer {
    private final String formattedName;
    private final int wins;

    public TntTagPlayer(String formattedName, int wins) {
        this.formattedName = formattedName;
        this.wins = Math.max(0, wins);
    }

    public String getFormattedName() { return formattedName; }
    public int getWins() { return wins; }
}
