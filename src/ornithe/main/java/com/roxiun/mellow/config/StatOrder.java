package com.roxiun.mellow.config;

import java.util.*;

/** Stable names in config map to the existing renderer's numeric column identifiers. */
public final class StatOrder {
    private StatOrder() {}
    public static final String[] BEDWARS = {"Team", "Stars", "Name", "FKDR", "Winstreak", "WLR", "BBLR", "Wins", "Beds", "Finals", "None", "HP", "Tags", "Ping"};
    public static final String[] SKYWARS = {"Team", "Level", "Name", "KDR", "WLR", "Wins", "Kills", "None", "HP", "Tags", "Ping"};
    public static final String[] DUELS = {"Team", "Division", "Name", "KDR", "WLR", "Wins", "Losses", "Kills", "Deaths", "Winstreak", "None", "HP", "Tags", "Ping"};

    public static int[] toColumns(String[] options, String[] selected) {
        if (selected == null) return new int[0];
        List<String> available = Arrays.asList(options);
        return Arrays.stream(selected).filter(Objects::nonNull).filter(s -> !"None".equals(s))
            .mapToInt(available::indexOf).filter(i -> i >= 0).distinct().toArray();
    }
}
