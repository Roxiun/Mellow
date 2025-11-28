package com.roxiun.mellow.api.bedwars;

import java.text.DecimalFormat;

public class BedwarsPlayer {

    private final String name;
    private final String stars;
    private final double fkdr;
    private final int winstreak;
    private final int finalKills;
    private final int finalDeaths;

    public BedwarsPlayer(
        String name,
        String stars,
        double fkdr,
        int winstreak,
        int finalKills,
        int finalDeaths
    ) {
        this.name = name;
        this.stars = stars;
        this.fkdr = fkdr;
        this.winstreak = winstreak;
        this.finalKills = finalKills;
        this.finalDeaths = finalDeaths;
    }

    public String getName() {
        return name;
    }

    public String getStars() {
        return stars;
    }

    public double getFkdr() {
        return fkdr;
    }

    public int getWinstreak() {
        return winstreak;
    }

    public int getFinalKills() {
        return finalKills;
    }

    public int getFinalDeaths() {
        return finalDeaths;
    }

    public String getFormattedFkdr() {
        DecimalFormat df = new DecimalFormat("#.##");
        return df.format(fkdr);
    }

    public String getFkdrColor() {
        if (fkdr >= 100) {
            return "§5";
        } else if (fkdr >= 50) {
            return "§d";
        } else if (fkdr >= 30) {
            return "§4";
        } else if (fkdr >= 20) {
            return "§c";
        } else if (fkdr >= 10) {
            return "§6";
        } else if (fkdr >= 7) {
            return "§e";
        } else if (fkdr >= 5) {
            return "§2";
        } else if (fkdr >= 3) {
            return "§a";
        } else if (fkdr >= 1) {
            return "§f";
        } else {
            return "§7";
        }
    }
}
