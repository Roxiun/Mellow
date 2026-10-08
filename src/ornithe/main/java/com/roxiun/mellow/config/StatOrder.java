package com.roxiun.mellow.config;

import java.util.*;

/** Stable names in config map to the existing renderer's numeric column identifiers. */
public final class StatOrder {
    private StatOrder() {}
    public static final String[] BEDWARS = com.roxiun.mellow.stats.GameRegistry.BEDWARS.columnOptions();
    public static final String[] SKYWARS = com.roxiun.mellow.stats.GameRegistry.SKYWARS.columnOptions();
    public static final String[] DUELS = com.roxiun.mellow.stats.GameRegistry.DUELS.columnOptions();

    public static final String[] BUILD_BATTLE = com.roxiun.mellow.stats.GameRegistry.BUILD_BATTLE.columnOptions();
    public static final String[] TNT_RUN = com.roxiun.mellow.stats.GameRegistry.TNT_RUN.columnOptions();

    public static int[] toColumns(String[] options, String[] selected) {
        if (selected == null) return new int[0];
        List<String> available = Arrays.asList(options);
        return Arrays.stream(selected).filter(Objects::nonNull).filter(s -> !"None".equals(s))
            .mapToInt(available::indexOf).filter(i -> i >= 0).distinct().toArray();
    }
}
