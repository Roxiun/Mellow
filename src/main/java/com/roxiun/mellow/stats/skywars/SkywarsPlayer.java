package com.roxiun.mellow.stats.skywars;

import java.text.DecimalFormat;

public class SkywarsPlayer {

    private final String name;
    private final String formattedNameWithRank;
    private final String levelFormatted;
    private final String levelFormattedWithBrackets;
    private final double kdr;
    private final int wins;
    private final int losses;
    private final int kills;
    private final int deaths;

    public SkywarsPlayer(
        String name,
        String formattedNameWithRank,
        String levelFormatted,
        String levelFormattedWithBrackets,
        double kdr,
        int wins,
        int losses,
        int kills,
        int deaths
    ) {
        this.name = name;
        this.formattedNameWithRank = formattedNameWithRank;
        this.levelFormatted = levelFormatted;
        this.levelFormattedWithBrackets = levelFormattedWithBrackets;
        this.kdr = kdr;
        this.wins = wins;
        this.losses = losses;
        this.kills = kills;
        this.deaths = deaths;
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

    public String getLevelFormatted() {
        return levelFormatted == null ? "§70" : levelFormatted;
    }

    public String getLevelFormattedWithBrackets() {
        return levelFormattedWithBrackets == null
            ? "§7[§70✯§7]"
            : levelFormattedWithBrackets;
    }

    public double getKdr() {
        return kdr;
    }

    public int getWins() {
        return wins;
    }

    public int getLosses() {
        return losses;
    }

    public int getKills() {
        return kills;
    }

    public int getDeaths() {
        return deaths;
    }

    public double getWlr() {
        return losses == 0 ? wins : (double) wins / losses;
    }

    public String getFormattedKdr() {
        DecimalFormat df = new DecimalFormat("#.##");
        return df.format(kdr);
    }

    public String getFormattedWlr() {
        DecimalFormat df = new DecimalFormat("#.##");
        return df.format(getWlr());
    }

    public String getKdrColor() {
        if (kdr >= 10) {
            return "§5";
        } else if (kdr >= 7) {
            return "§d";
        } else if (kdr >= 5) {
            return "§4";
        } else if (kdr >= 3) {
            return "§c";
        } else if (kdr >= 2) {
            return "§6";
        } else if (kdr >= 1.2) {
            return "§e";
        } else if (kdr >= 0.8) {
            return "§a";
        } else if (kdr >= 0.4) {
            return "§f";
        }
        return "§7";
    }

    public String getWlrColor() {
        double wlr = getWlr();
        if (wlr >= 10) {
            return "§5";
        } else if (wlr >= 7) {
            return "§d";
        } else if (wlr >= 5) {
            return "§4";
        } else if (wlr >= 3) {
            return "§c";
        } else if (wlr >= 2) {
            return "§6";
        } else if (wlr >= 1.2) {
            return "§e";
        } else if (wlr >= 0.8) {
            return "§a";
        } else if (wlr >= 0.4) {
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

    public String getKillsColor() {
        if (kills >= 25000) {
            return "§5";
        } else if (kills >= 15000) {
            return "§d";
        } else if (kills >= 10000) {
            return "§4";
        } else if (kills >= 5000) {
            return "§c";
        } else if (kills >= 2500) {
            return "§6";
        } else if (kills >= 1000) {
            return "§e";
        } else if (kills >= 300) {
            return "§2";
        } else if (kills >= 100) {
            return "§a";
        } else if (kills >= 25) {
            return "§f";
        }
        return "§7";
    }

    public String getFormattedKdrWithColor() {
        return getKdrColor() + getFormattedKdr();
    }

    public String getFormattedWlrWithColor() {
        return getWlrColor() + getFormattedWlr();
    }

    public String getFormattedWinsWithColor() {
        return getWinsColor() + wins;
    }

    public String getFormattedKillsWithColor() {
        return getKillsColor() + kills;
    }
}
