package com.roxiun.mellow.stats.bedwars;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public enum BedwarsMode {
    OVERALL("Overall", "Overall", "Core Modes", ""),
    EIGHT_ONE("1s", "Solos", "Core Modes", "eight_one_"),
    EIGHT_TWO("2s", "Doubles", "Core Modes", "eight_two_"),
    FOUR_THREE("3s", "3v3v3v3", "Core Modes", "four_three_"),
    FOUR_FOUR("4s", "4v4v4v4", "Core Modes", "four_four_"),
    TWO_FOUR("4v4", "4v4", "4v4", "two_four_"),
    CASTLE("Castle", "Castle", "Castle", "castle_"),
    OVERALL_LUCKY(
        "Lucky Overall",
        "Lucky Overall",
        "Lucky Blocks",
        "eight_two_lucky_",
        "four_four_lucky_"
    ),
    EIGHT_TWO_LUCKY(
        "Lucky Doubles",
        "Lucky Doubles",
        "Lucky Blocks",
        "eight_two_lucky_"
    ),
    FOUR_FOUR_LUCKY("Lucky Fours", "Lucky Fours", "Lucky Blocks", "four_four_lucky_"),
    OVERALL_RUSH(
        "Rush Overall",
        "Rush Overall",
        "Rush",
        "eight_two_rush_",
        "four_four_rush_"
    ),
    EIGHT_TWO_RUSH("Rush Doubles", "Rush Doubles", "Rush", "eight_two_rush_"),
    FOUR_FOUR_RUSH("Rush Fours", "Rush Fours", "Rush", "four_four_rush_"),
    OVERALL_VOIDLESS(
        "Voidless Overall",
        "Voidless Overall",
        "Voidless",
        "eight_two_voidless_",
        "four_four_voidless_"
    ),
    EIGHT_TWO_VOIDLESS(
        "Voidless Doubles",
        "Voidless Doubles",
        "Voidless",
        "eight_two_voidless_"
    ),
    FOUR_FOUR_VOIDLESS(
        "Voidless Fours",
        "Voidless Fours",
        "Voidless",
        "four_four_voidless_"
    ),
    OVERALL_ARMED(
        "Armed Overall",
        "Armed Overall",
        "Armed",
        "eight_two_armed_",
        "four_four_armed_"
    ),
    EIGHT_TWO_ARMED("Armed Doubles", "Armed Doubles", "Armed", "eight_two_armed_"),
    FOUR_FOUR_ARMED("Armed Fours", "Armed Fours", "Armed", "four_four_armed_"),
    OVERALL_ULTIMATE(
        "Ultimate Overall",
        "Ultimate Overall",
        "Ultimate",
        "eight_two_ultimate_",
        "four_four_ultimate_"
    ),
    EIGHT_TWO_ULTIMATE(
        "Ultimate Doubles",
        "Ultimate Doubles",
        "Ultimate",
        "eight_two_ultimate_"
    ),
    FOUR_FOUR_ULTIMATE(
        "Ultimate Fours",
        "Ultimate Fours",
        "Ultimate",
        "four_four_ultimate_"
    ),
    OVERALL_SWAP(
        "Swap Overall",
        "Swap Overall",
        "Swap",
        "eight_two_swap_",
        "four_four_swap_"
    ),
    EIGHT_TWO_SWAP("Swap Doubles", "Swap Doubles", "Swap", "eight_two_swap_"),
    FOUR_FOUR_SWAP("Swap Fours", "Swap Fours", "Swap", "four_four_swap_");

    private final String shortName;
    private final String fullName;
    private final String category;
    private final List<String> statPrefixes;

    BedwarsMode(String shortName, String fullName, String category, String... statPrefixes) {
        this.shortName = shortName;
        this.fullName = fullName;
        this.category = category;
        this.statPrefixes = Collections.unmodifiableList(
            Arrays.asList(statPrefixes == null ? new String[0] : statPrefixes)
        );
    }

    public String getShortName() {
        return shortName;
    }

    public String getFullName() {
        return fullName;
    }

    public String getCategory() {
        return category;
    }

    public List<String> getStatPrefixes() {
        return statPrefixes;
    }

    public boolean isOverallAggregate() {
        return statPrefixes.size() > 1;
    }

    public static List<String> categories() {
        Set<String> categories = new LinkedHashSet<>();
        for (BedwarsMode mode : values()) {
            categories.add(mode.category);
        }
        return new ArrayList<>(categories);
    }

    public static List<BedwarsMode> modesForCategory(String category) {
        List<BedwarsMode> modes = new ArrayList<>();
        for (BedwarsMode mode : values()) {
            if (mode.category.equals(category)) {
                modes.add(mode);
            }
        }
        return modes;
    }
}
