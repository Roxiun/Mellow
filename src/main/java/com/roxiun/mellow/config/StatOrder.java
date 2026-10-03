package com.roxiun.mellow.config;

import java.util.*;

/** Stable names in config map to the existing renderer's numeric column identifiers. */
public final class StatOrder {
    private StatOrder() {}
    public static final String[] BEDWARS = {"Team", "Stars", "Name", "FKDR", "Winstreak", "WLR", "BBLR", "Wins", "Beds", "Finals", "None", "HP", "Tags", "Ping", "Client"};
    public static final String[] SKYWARS = {"Team", "Level", "Name", "KDR", "WLR", "Wins", "Kills", "None", "HP", "Tags", "Ping", "Client"};
    public static final String[] DUELS = {"Team", "Division", "Name", "KDR", "WLR", "Wins", "Losses", "Kills", "Deaths", "Winstreak", "None", "HP", "Tags", "Ping", "Client"};

    public static String[] fromLegacy(String[] options, int[] slots) {
        Set<String> ordered = new LinkedHashSet<>();
        for (int index : slots) {
            if (index >= 0 && index < options.length && !"None".equals(options[index])) ordered.add(options[index]);
        }
        return ordered.toArray(new String[0]);
    }

    public static int[] toColumns(String[] options, String[] selected) {
        if (selected == null) return new int[0];
        List<String> available = Arrays.asList(options);
        return Arrays.stream(selected).filter(Objects::nonNull).filter(s -> !"None".equals(s))
            .mapToInt(available::indexOf).filter(i -> i >= 0).distinct().toArray();
    }
}
