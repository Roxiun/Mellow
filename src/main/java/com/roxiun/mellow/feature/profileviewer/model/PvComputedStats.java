package com.roxiun.mellow.feature.profileviewer.model;

import com.roxiun.mellow.stats.bedwars.BedwarsMode;
import com.roxiun.mellow.stats.bedwars.BedwarsPlayer;
import java.math.BigDecimal;
import java.math.RoundingMode;

public class PvComputedStats {

    public final int level;
    public final int wins;
    public final int losses;
    public final int gamesPlayed;
    public final int finalKills;
    public final int finalDeaths;
    public final int kills;
    public final int deaths;
    public final int beds;
    public final int bedsLost;
    public final int tokens;
    public final int slumberTickets;

    public final double wlr;
    public final double kdr;
    public final double fkdr;
    public final double bblr;
    public final double winsPerStar;
    public final double lossesPerStar;
    public final double killsPerGame;
    public final double killsPerStar;
    public final double finalsPerGame;
    public final double finalsPerStar;
    public final double bedsPerGame;
    public final double bedsPerStar;
    public final double clutchRatePercent;
    public final double winRatePercent;

    public final int skillIndex;

    public PvComputedStats(
        int level,
        int wins,
        int losses,
        int gamesPlayed,
        int finalKills,
        int finalDeaths,
        int kills,
        int deaths,
        int beds,
        int bedsLost,
        int tokens,
        int slumberTickets,
        double wlr,
        double kdr,
        double fkdr,
        double bblr,
        double winsPerStar,
        double lossesPerStar,
        double killsPerGame,
        double killsPerStar,
        double finalsPerGame,
        double finalsPerStar,
        double bedsPerGame,
        double bedsPerStar,
        double clutchRatePercent,
        double winRatePercent,
        int skillIndex
    ) {
        this.level = level;
        this.wins = wins;
        this.losses = losses;
        this.gamesPlayed = gamesPlayed;
        this.finalKills = finalKills;
        this.finalDeaths = finalDeaths;
        this.kills = kills;
        this.deaths = deaths;
        this.beds = beds;
        this.bedsLost = bedsLost;
        this.tokens = tokens;
        this.slumberTickets = slumberTickets;
        this.wlr = wlr;
        this.kdr = kdr;
        this.fkdr = fkdr;
        this.bblr = bblr;
        this.winsPerStar = winsPerStar;
        this.lossesPerStar = lossesPerStar;
        this.killsPerGame = killsPerGame;
        this.killsPerStar = killsPerStar;
        this.finalsPerGame = finalsPerGame;
        this.finalsPerStar = finalsPerStar;
        this.bedsPerGame = bedsPerGame;
        this.bedsPerStar = bedsPerStar;
        this.clutchRatePercent = clutchRatePercent;
        this.winRatePercent = winRatePercent;
        this.skillIndex = skillIndex;
    }

    public static PvComputedStats from(
        PvSourceData sourceData,
        BedwarsPlayer fallbackPlayer,
        BedwarsMode mode
    ) {
        int wins = 0;
        int losses = 0;
        int finalKills = 0;
        int finalDeaths = 0;
        int kills = 0;
        int deaths = 0;
        int beds = 0;
        int bedsLost = 0;

        for (String prefix : mode.getStatPrefixes()) {
            wins += sourceData.bedwarsStat(prefix + "wins_bedwars");
            losses += sourceData.bedwarsStat(prefix + "losses_bedwars");
            finalKills += sourceData.bedwarsStat(prefix + "final_kills_bedwars");
            finalDeaths += sourceData.bedwarsStat(prefix + "final_deaths_bedwars");
            kills += sourceData.bedwarsStat(prefix + "kills_bedwars");
            deaths += sourceData.bedwarsStat(prefix + "deaths_bedwars");
            beds += sourceData.bedwarsStat(prefix + "beds_broken_bedwars");
            bedsLost += sourceData.bedwarsStat(prefix + "beds_lost_bedwars");
        }

        if (
            mode == BedwarsMode.OVERALL &&
            wins == 0 &&
            losses == 0 &&
            finalKills == 0 &&
            finalDeaths == 0 &&
            beds == 0 &&
            bedsLost == 0 &&
            fallbackPlayer != null
        ) {
            wins = fallbackPlayer.getWins();
            losses = fallbackPlayer.getLosses();
            finalKills = fallbackPlayer.getFinalKills();
            finalDeaths = fallbackPlayer.getFinalDeaths();
            beds = fallbackPlayer.getBedsBroken();
            bedsLost = fallbackPlayer.getBedsLost();
        }

        int level = sourceData.getBedwarsLevel();
        if (level <= 0 && fallbackPlayer != null) {
            level = extractLevelFromStars(fallbackPlayer.getStars());
        }

        int gamesPlayed = wins + losses;

        double wlr = safeRatio(wins, losses);
        double kdr = safeRatio(kills, deaths);
        double fkdr = safeRatio(finalKills, finalDeaths);
        double bblr = safeRatio(beds, bedsLost);
        double winsPerStar = safeRatio(wins, level);
        double lossesPerStar = safeRatio(losses, level);
        double killsPerGame = safeRatio(kills, gamesPlayed);
        double killsPerStar = safeRatio(kills, level);
        double finalsPerGame = safeRatio(finalKills, gamesPlayed);
        double finalsPerStar = safeRatio(finalKills, level);
        double bedsPerGame = safeRatio(beds, gamesPlayed);
        double bedsPerStar = safeRatio(beds, level);

        double clutchRatePercent = bedsLost > 0
            ? roundOneDecimal(
                Math.max(
                    0.0,
                    ((double) (wins - (gamesPlayed - bedsLost)) / bedsLost) * 100.0
                )
            )
            : 0.0;
        double winRatePercent = gamesPlayed > 0
            ? roundOneDecimal((100.0 * wins) / gamesPlayed)
            : 0.0;

        int skillIndex = (int) Math.pow(fkdr, 2.0) * Math.max(level, 1);

        return new PvComputedStats(
            level,
            wins,
            losses,
            gamesPlayed,
            finalKills,
            finalDeaths,
            kills,
            deaths,
            beds,
            bedsLost,
            sourceData.getTokens(),
            sourceData.getSlumberTickets(),
            wlr,
            kdr,
            fkdr,
            bblr,
            winsPerStar,
            lossesPerStar,
            killsPerGame,
            killsPerStar,
            finalsPerGame,
            finalsPerStar,
            bedsPerGame,
            bedsPerStar,
            clutchRatePercent,
            winRatePercent,
            skillIndex
        );
    }

    private static double safeRatio(int numerator, int denominator) {
        if (numerator != 0 && denominator != 0) {
            return roundTwoDecimals((double) numerator / (double) denominator);
        }
        if (numerator != 0) {
            return roundTwoDecimals(numerator);
        }
        return 0.0;
    }

    private static int extractLevelFromStars(String stars) {
        if (stars == null || stars.isEmpty()) {
            return 0;
        }
        String stripped = stars.replaceAll("§.", "").replaceAll("[^0-9]", "");
        if (stripped.isEmpty()) {
            return 0;
        }
        try {
            return Integer.parseInt(stripped);
        } catch (NumberFormatException ignored) {
            return 0;
        }
    }

    private static double roundTwoDecimals(double value) {
        return new BigDecimal(value).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }

    private static double roundOneDecimal(double value) {
        return new BigDecimal(value).setScale(1, RoundingMode.HALF_UP).doubleValue();
    }
}
