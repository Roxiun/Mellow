package com.roxiun.mellow.stats.murdermystery;

public final class MurderMysteryPlayer {
    private final String formattedName;
    private final int wins;
    private final int kills;
    private final int games;

    public MurderMysteryPlayer(String formattedName, int wins, int kills, int games) {
        this.formattedName = formattedName;
        this.wins = Math.max(0, wins);
        this.kills = Math.max(0, kills);
        this.games = Math.max(0, games);
    }

    public String getFormattedName() { return formattedName; }
    public int getWins() { return wins; }
    public int getKills() { return kills; }
    public int getGames() { return games; }
}
