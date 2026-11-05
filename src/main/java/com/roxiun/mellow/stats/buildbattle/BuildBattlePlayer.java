package com.roxiun.mellow.stats.buildbattle;

public class BuildBattlePlayer {

    private static final TitleTier[] TITLE_TIERS = new TitleTier[] {
        new TitleTier(0, "Rookie", "§f"),
        new TitleTier(100, "Untrained", "§7"),
        new TitleTier(250, "Amateur", "§8"),
        new TitleTier(500, "Prospect", "§a"),
        new TitleTier(1000, "Apprentice", "§2"),
        new TitleTier(2000, "Experienced", "§b"),
        new TitleTier(3500, "Seasoned", "§3"),
        new TitleTier(5000, "Trained", "§9"),
        new TitleTier(7500, "Skilled", "§1"),
        new TitleTier(10000, "Talented", "§5"),
        new TitleTier(15000, "Professional", "§d"),
        new TitleTier(20000, "Artisan", "§c"),
        new TitleTier(30000, "Expert", "§4"),
        new TitleTier(50000, "Master", "§6"),
        new TitleTier(100000, "Legend", "§a"),
        new TitleTier(200000, "Grandmaster", "§b"),
        new TitleTier(300000, "Celestial", "§d"),
        new TitleTier(400000, "Divine", "§c"),
        new TitleTier(500000, "Ascended", "§6"),
    };

    private final String name;
    private final String formattedNameWithRank;
    private final int score;
    private final int wins;
    private final int gamesPlayed;

    public BuildBattlePlayer(
        String name,
        String formattedNameWithRank,
        int score,
        int wins,
        int gamesPlayed
    ) {
        this.name = name;
        this.formattedNameWithRank = formattedNameWithRank;
        this.score = Math.max(0, score);
        this.wins = Math.max(0, wins);
        this.gamesPlayed = Math.max(0, gamesPlayed);
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

    public int getScore() {
        return score;
    }

    public int getWins() {
        return wins;
    }

    public int getGamesPlayed() {
        return gamesPlayed;
    }

    public String getFormattedTitle() {
        TitleTier activeTier = TITLE_TIERS[0];
        for (TitleTier tier : TITLE_TIERS) {
            if (score >= tier.minScore) {
                activeTier = tier;
            } else {
                break;
            }
        }
        return activeTier.colorCode + activeTier.name;
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

    public String getFormattedWinsWithColor() {
        return getWinsColor() + wins;
    }

    private static class TitleTier {

        private final int minScore;
        private final String name;
        private final String colorCode;

        private TitleTier(int minScore, String name, String colorCode) {
            this.minScore = minScore;
            this.name = name;
            this.colorCode = colorCode;
        }
    }
}
