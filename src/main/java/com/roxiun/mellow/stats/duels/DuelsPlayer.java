package com.roxiun.mellow.stats.duels;

import java.text.DecimalFormat;

public class DuelsPlayer {

    private final String name;
    private final String formattedNameWithRank;
    private final DuelsMode mode;
    private final String division;
    private final int kills;
    private final int deaths;
    private final int wins;
    private final int losses;
    private final int winstreak;

    public DuelsPlayer(
        String name,
        String formattedNameWithRank,
        DuelsMode mode,
        String division,
        int kills,
        int deaths,
        int wins,
        int losses,
        int winstreak
    ) {
        this.name = name;
        this.formattedNameWithRank = formattedNameWithRank;
        this.mode = mode == null ? DuelsMode.OVERALL : mode;
        this.division = division == null || division.trim().isEmpty()
            ? "§7Unranked"
            : division;
        this.kills = Math.max(0, kills);
        this.deaths = Math.max(0, deaths);
        this.wins = Math.max(0, wins);
        this.losses = Math.max(0, losses);
        this.winstreak = Math.max(-1, winstreak);
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

    public DuelsMode getMode() {
        return mode;
    }

    public String getDivision() {
        return division;
    }

    public int getKills() {
        return kills;
    }

    public int getDeaths() {
        return deaths;
    }

    public int getWins() {
        return wins;
    }

    public int getLosses() {
        return losses;
    }

    public int getWinstreak() {
        return winstreak;
    }

    public double getKdr() {
        return deaths == 0 ? kills : (double) kills / deaths;
    }

    public double getWlr() {
        return losses == 0 ? wins : (double) wins / losses;
    }

    public String getFormattedKdr() {
        return formatRatio(getKdr());
    }

    public String getFormattedWlr() {
        return formatRatio(getWlr());
    }

    public String getKdrColor() {
        double kdr = getKdr();
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

    public String getLossesColor() {
        if (losses >= 10000) {
            return "§5";
        } else if (losses >= 5000) {
            return "§d";
        } else if (losses >= 2500) {
            return "§4";
        } else if (losses >= 1000) {
            return "§c";
        } else if (losses >= 500) {
            return "§6";
        } else if (losses >= 250) {
            return "§e";
        } else if (losses >= 100) {
            return "§2";
        } else if (losses >= 25) {
            return "§a";
        } else if (losses >= 5) {
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

    public String getDeathsColor() {
        if (deaths >= 25000) {
            return "§5";
        } else if (deaths >= 15000) {
            return "§d";
        } else if (deaths >= 10000) {
            return "§4";
        } else if (deaths >= 5000) {
            return "§c";
        } else if (deaths >= 2500) {
            return "§6";
        } else if (deaths >= 1000) {
            return "§e";
        } else if (deaths >= 300) {
            return "§2";
        } else if (deaths >= 100) {
            return "§a";
        } else if (deaths >= 25) {
            return "§f";
        }
        return "§7";
    }

    public String getWinstreakColor() {
        if (winstreak >= 100) {
            return "§5";
        } else if (winstreak >= 50) {
            return "§d";
        } else if (winstreak >= 25) {
            return "§4";
        } else if (winstreak >= 15) {
            return "§c";
        } else if (winstreak >= 10) {
            return "§6";
        } else if (winstreak >= 5) {
            return "§e";
        } else if (winstreak >= 3) {
            return "§a";
        } else if (winstreak >= 1) {
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

    public String getFormattedLossesWithColor() {
        return getLossesColor() + losses;
    }

    public String getFormattedKillsWithColor() {
        return getKillsColor() + kills;
    }

    public String getFormattedDeathsWithColor() {
        return getDeathsColor() + deaths;
    }

    public String getFormattedWinstreakWithColor() {
        return winstreak < 0 ? "§7?" : getWinstreakColor() + winstreak;
    }

    private String formatRatio(double value) {
        DecimalFormat df = new DecimalFormat("#.##");
        return df.format(value);
    }
}
