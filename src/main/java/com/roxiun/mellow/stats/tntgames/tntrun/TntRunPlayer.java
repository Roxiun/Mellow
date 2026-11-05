package com.roxiun.mellow.stats.tntgames.tntrun;

import java.text.DecimalFormat;

public class TntRunPlayer {

    private final String name;
    private final String formattedNameWithRank;
    private final int wins;
    private final int deaths;
    private final int bestRecord;

    public TntRunPlayer(
        String name,
        String formattedNameWithRank,
        int wins,
        int deaths,
        int bestRecord
    ) {
        this.name = name;
        this.formattedNameWithRank = formattedNameWithRank;
        this.wins = Math.max(0, wins);
        this.deaths = Math.max(0, deaths);
        this.bestRecord = Math.max(0, bestRecord);
    }

    public String getName() {
        return name;
    }

    public String getFormattedNameWithRank() {
        if (formattedNameWithRank == null || formattedNameWithRank.isEmpty()) {
            return name;
        }
        return formattedNameWithRank;
    }

    public int getWins() {
        return wins;
    }

    public int getDeaths() {
        return deaths;
    }

    public int getBestRecord() {
        return bestRecord;
    }

    public double getRatio() {
        return deaths == 0 ? wins : (double) wins / deaths;
    }

    public String getFormattedRatio() {
        DecimalFormat df = new DecimalFormat("#.##");
        return df.format(getRatio());
    }

    public String getRatioColor() {
        double ratio = getRatio();
        if (ratio >= 10) {
            return "§5";
        } else if (ratio >= 7) {
            return "§d";
        } else if (ratio >= 5) {
            return "§4";
        } else if (ratio >= 3) {
            return "§c";
        } else if (ratio >= 2) {
            return "§6";
        } else if (ratio >= 1.2) {
            return "§e";
        } else if (ratio >= 0.8) {
            return "§a";
        } else if (ratio >= 0.4) {
            return "§f";
        }
        return "§7";
    }

    public String getWinsColor() {
        if (wins >= 10000) {
            return "§5";
        } else if (wins >= 5000) {
            return "§d";
        } else if (wins >= 2500) {
            return "§4";
        } else if (wins >= 1000) {
            return "§c";
        } else if (wins >= 500) {
            return "§6";
        } else if (wins >= 250) {
            return "§e";
        } else if (wins >= 100) {
            return "§2";
        } else if (wins >= 25) {
            return "§a";
        } else if (wins >= 5) {
            return "§f";
        }
        return "§7";
    }

    public String getDeathsColor() {
        if (deaths >= 10000) {
            return "§5";
        } else if (deaths >= 5000) {
            return "§d";
        } else if (deaths >= 2500) {
            return "§4";
        } else if (deaths >= 1000) {
            return "§c";
        } else if (deaths >= 500) {
            return "§6";
        } else if (deaths >= 250) {
            return "§e";
        } else if (deaths >= 100) {
            return "§2";
        } else if (deaths >= 25) {
            return "§a";
        } else if (deaths >= 5) {
            return "§f";
        }
        return "§7";
    }

    public String getFormattedWinsWithColor() {
        return getWinsColor() + wins;
    }

    public String getFormattedDeathsWithColor() {
        return getDeathsColor() + deaths;
    }

    public String getFormattedRatioWithColor() {
        return getRatioColor() + getFormattedRatio();
    }
}
