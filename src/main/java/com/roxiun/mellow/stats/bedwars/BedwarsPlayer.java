package com.roxiun.mellow.stats.bedwars;

import java.text.DecimalFormat;

public class BedwarsPlayer {

    private final String name;
    private final String formattedNameWithRank;
    private final String stars;
    private final double fkdr;
    private final int winstreak;
    private final boolean hasWinstreakData;
    private final int finalKills;
    private final int finalDeaths;
    private final int wins;
    private final int losses;
    private final int bedsBroken;
    private final int bedsLost;
    private final int finals;

    public BedwarsPlayer(
        String name,
        String stars,
        double fkdr,
        int winstreak,
        int finalKills,
        int finalDeaths,
        int wins,
        int losses,
        int bedsBroken,
        int bedsLost,
        int finals
    ) {
        this(
            name,
            name,
            stars,
            fkdr,
            winstreak,
            true,
            finalKills,
            finalDeaths,
            wins,
            losses,
            bedsBroken,
            bedsLost,
            finals
        );
    }

    public BedwarsPlayer(
        String name,
        String stars,
        double fkdr,
        int winstreak,
        boolean hasWinstreakData,
        int finalKills,
        int finalDeaths,
        int wins,
        int losses,
        int bedsBroken,
        int bedsLost,
        int finals
    ) {
        this(
            name,
            name,
            stars,
            fkdr,
            winstreak,
            hasWinstreakData,
            finalKills,
            finalDeaths,
            wins,
            losses,
            bedsBroken,
            bedsLost,
            finals
        );
    }

    public BedwarsPlayer(
        String name,
        String formattedNameWithRank,
        String stars,
        double fkdr,
        int winstreak,
        int finalKills,
        int finalDeaths,
        int wins,
        int losses,
        int bedsBroken,
        int bedsLost,
        int finals
    ) {
        this(
            name,
            formattedNameWithRank,
            stars,
            fkdr,
            winstreak,
            true,
            finalKills,
            finalDeaths,
            wins,
            losses,
            bedsBroken,
            bedsLost,
            finals
        );
    }

    public BedwarsPlayer(
        String name,
        String formattedNameWithRank,
        String stars,
        double fkdr,
        int winstreak,
        boolean hasWinstreakData,
        int finalKills,
        int finalDeaths,
        int wins,
        int losses,
        int bedsBroken,
        int bedsLost,
        int finals
    ) {
        this.name = name;
        this.formattedNameWithRank = formattedNameWithRank;
        this.stars = stars;
        this.fkdr = fkdr;
        this.winstreak = winstreak;
        this.hasWinstreakData = hasWinstreakData;
        this.finalKills = finalKills;
        this.finalDeaths = finalDeaths;
        this.wins = wins;
        this.losses = losses;
        this.bedsBroken = bedsBroken;
        this.bedsLost = bedsLost;
        this.finals = finals;
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

    public String getStars() {
        return stars;
    }

    public double getFkdr() {
        return fkdr;
    }

    public int getWinstreak() {
        return winstreak;
    }

    public boolean hasWinstreakData() {
        return hasWinstreakData;
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

    public int getWins() {
        return wins;
    }

    public int getLosses() {
        return losses;
    }

    public int getBedsBroken() {
        return bedsBroken;
    }

    public int getBedsLost() {
        return bedsLost;
    }

    public int getFinals() {
        return finals;
    }

    public double getWLR() {
        return (losses == 0) ? wins : (double) wins / losses;
    }

    public double getBBLR() {
        return (losses == 0) ? bedsBroken : (double) bedsBroken / losses;
    }

    public String getFormattedWLR() {
        DecimalFormat df = new DecimalFormat("#.##");
        return df.format(getWLR());
    }

    public String getBBLRColor() {
        double bblr = getBBLR();
        if (bblr < 0.3) {
            return "§7"; // Gray
        } else if (bblr < 0.9) {
            return "§f"; // White
        } else if (bblr < 1.5) {
            return "§a"; // Green
        } else if (bblr < 2.1) {
            return "§2"; // Dark Green
        } else if (bblr < 3.0) {
            return "§e"; // Yellow
        } else if (bblr < 6.0) {
            return "§6"; // Gold
        } else if (bblr < 9.0) {
            return "§c"; // Red
        } else if (bblr < 15.0) {
            return "§4"; // Dark Red
        } else if (bblr < 30.0) {
            return "§d"; // Light Purple
        } else {
            return "§5"; // Dark Purple
        }
    }

    public String getFormattedBBLR() {
        DecimalFormat df = new DecimalFormat("#.##");
        return df.format(getBBLR());
    }

    public String getFormattedBBLRWithColor() {
        return getBBLRColor() + getFormattedBBLR();
    }

    public String getWLRColor() {
        double wlr = getWLR();
        if (wlr < 0.3) {
            return "§7"; // Gray
        } else if (wlr < 0.9) {
            return "§f"; // White
        } else if (wlr < 1.5) {
            return "§a"; // Green
        } else if (wlr < 2.1) {
            return "§2"; // Dark Green
        } else if (wlr < 3.0) {
            return "§e"; // Yellow
        } else if (wlr < 6.0) {
            return "§6"; // Gold
        } else if (wlr < 9.0) {
            return "§c"; // Red
        } else if (wlr < 15.0) {
            return "§4"; // Dark Red
        } else if (wlr < 30.0) {
            return "§d"; // Light Purple
        } else {
            return "§5"; // Dark Purple
        }
    }

    public String getWinsColor() {
        int wins = getWins();
        if (wins < 150) {
            return "§7"; // Gray
        } else if (wins < 300) {
            return "§f"; // White
        } else if (wins < 450) {
            return "§a"; // Green
        } else if (wins < 1500) {
            return "§2"; // Dark Green
        } else if (wins < 2250) {
            return "§e"; // Yellow
        } else if (wins < 4500) {
            return "§6"; // Gold
        } else if (wins < 7500) {
            return "§c"; // Red
        } else if (wins < 15000) {
            return "§4"; // Dark Red
        } else if (wins < 30000) {
            return "§d"; // Light Purple
        } else {
            return "§5"; // Dark Purple
        }
    }

    public String getBedsColor() {
        int beds = getBedsBroken();
        if (beds < 250) {
            return "§7"; // Gray
        } else if (beds < 500) {
            return "§f"; // White
        } else if (beds < 1250) {
            return "§a"; // Green
        } else if (beds < 2500) {
            return "§2"; // Dark Green
        } else if (beds < 3750) {
            return "§e"; // Yellow
        } else if (beds < 7500) {
            return "§6"; // Gold
        } else if (beds < 12500) {
            return "§c"; // Red
        } else if (beds < 25000) {
            return "§4"; // Dark Red
        } else if (beds < 50000) {
            return "§d"; // Light Purple
        } else {
            return "§5"; // Dark Purple
        }
    }

    public String getFinalsColor() {
        int finals = getFinals();
        if (finals < 500) {
            return "§7"; // Gray
        } else if (finals < 1000) {
            return "§f"; // White
        } else if (finals < 2500) {
            return "§a"; // Green
        } else if (finals < 5000) {
            return "§2"; // Dark Green
        } else if (finals < 7500) {
            return "§e"; // Yellow
        } else if (finals < 15000) {
            return "§6"; // Gold
        } else if (finals < 25000) {
            return "§c"; // Red
        } else if (finals < 50000) {
            return "§4"; // Dark Red
        } else if (finals < 100000) {
            return "§d"; // Light Purple
        } else {
            return "§5"; // Dark Purple
        }
    }

    public String getWinstreakColor() {
        if (!hasWinstreakData) {
            return "§7";
        }

        int winstreak = getWinstreak();
        if (winstreak < 5) {
            return "§7"; // Gray
        } else if (winstreak < 15) {
            return "§f"; // White
        } else if (winstreak < 25) {
            return "§a"; // Green
        } else if (winstreak < 40) {
            return "§2"; // Dark Green
        } else if (winstreak < 50) {
            return "§e"; // Yellow
        } else if (winstreak < 75) {
            return "§6"; // Gold
        } else if (winstreak < 100) {
            return "§c"; // Red
        } else if (winstreak < 250) {
            return "§4"; // Dark Red
        } else if (winstreak < 500) {
            return "§d"; // Light Purple
        } else {
            return "§5"; // Dark Purple
        }
    }

    public String getFormattedWLRWithColor() {
        return getWLRColor() + getFormattedWLR();
    }

    public String getFormattedWinsWithColor() {
        return getWinsColor() + getWins();
    }

    public String getFormattedBedsWithColor() {
        return getBedsColor() + getBedsBroken();
    }

    public String getFormattedFinalsWithColor() {
        return getFinalsColor() + getFinals();
    }

    public String getFormattedWinstreakWithColor() {
        if (!hasWinstreakData) {
            return "§7?";
        }
        return getWinstreakColor() + getWinstreak();
    }
}
