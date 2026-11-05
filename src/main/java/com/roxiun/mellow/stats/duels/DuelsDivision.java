package com.roxiun.mellow.stats.duels;

/** Earned divisions use family wins, independently of selected cosmetic titles. */
final class DuelsDivision {
    private static final int[] REQUIREMENTS = {50, 100, 250, 500, 1000, 2000, 5000, 10000, 25000, 50000, 100000};
    private static final int[] STEPS = {10, 30, 50, 100, 200, 600, 1000, 3000, 5000, 10000, 10000};
    private static final String[] NAMES = {
        "Rookie", "Iron", "Gold", "Diamond", "Master", "Legend", "Grandmaster",
        "Godlike", "CELESTIAL", "DIVINE", "ASCENDED"
    };
    private static final String[] STYLES = {
        "§7", "§f", "§6", "§3", "§2", "§4§l", "§e§l", "§5§l", "§b§l", "§d§l", "§c§l"
    };

    private DuelsDivision() {}

    static String format(int wins, DuelsMode mode) {
        if (mode == DuelsMode.ARENA) return "§7-";
        int numerator = mode.isOverall() ? 2 : 1;
        int denominator = mode.hasHalfTitleRequirements() ? 2 : 1;
        for (int rank = REQUIREMENTS.length - 1; rank >= 0; rank--) {
            int required = REQUIREMENTS[rank] * numerator / denominator;
            if (wins < required) continue;
            int step = STEPS[rank] * numerator / denominator;
            int tier = Math.min(rank == REQUIREMENTS.length - 1 ? 50 : 5, (wins - required) / step + 1);
            return STYLES[rank] + NAMES[rank] + (tier == 1 ? "" : " " + roman(tier));
        }
        return "§7-";
    }

    private static String roman(int value) {
        int[] amounts = {50, 40, 10, 9, 5, 4, 1};
        String[] symbols = {"L", "XL", "X", "IX", "V", "IV", "I"};
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < amounts.length; i++) {
            while (value >= amounts[i]) {
                result.append(symbols[i]);
                value -= amounts[i];
            }
        }
        return result.toString();
    }
}
