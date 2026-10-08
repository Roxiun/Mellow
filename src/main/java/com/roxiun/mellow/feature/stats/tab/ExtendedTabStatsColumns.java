package com.roxiun.mellow.feature.stats.tab;

import com.roxiun.mellow.stats.*;
import com.roxiun.mellow.config.MellowOneConfig;
//? if ornithe {
import com.roxiun.mellow.config.StatOrder;
//?}
import java.util.ArrayList;
import java.util.List;

public final class ExtendedTabStatsColumns {

    public static final int COLUMN_GAP = 4;
    public static final int BEDWARS_NONE_INDEX = 10;
    public static final int BEDWARS_HP_INDEX = 11;
    public static final int SKYWARS_NONE_INDEX = 7;
    public static final int SKYWARS_HP_INDEX = 8;
    public static final int DUELS_NONE_INDEX = 10;
    public static final int DUELS_HP_INDEX = 11;
    public static final int BUILD_BATTLE_NONE_INDEX = 4;
    public static final int BUILD_BATTLE_HP_INDEX = 5;
    public static final int TNT_RUN_NONE_INDEX = 4;
    public static final int TNT_RUN_HP_INDEX = 5;
    public static final int BEDWARS_TAGS_COLUMN = 12;
    public static final int BEDWARS_PING_COLUMN = 13;
    public static final int SKYWARS_TAGS_COLUMN = 9;
    public static final int SKYWARS_PING_COLUMN = 10;
    public static final int DUELS_TAGS_COLUMN = 12;
    public static final int DUELS_PING_COLUMN = 13;
    public static final int BUILD_BATTLE_TAGS_COLUMN = 6;
    public static final int BUILD_BATTLE_PING_COLUMN = 7;
    public static final int TNT_RUN_TAGS_COLUMN = 6;
    public static final int TNT_RUN_PING_COLUMN = 7;

    private ExtendedTabStatsColumns() {}

    public static int[] getConfiguredStatsForScope(
        StatScope scope,
        MellowOneConfig config
    ) {
        GameDefinition<?> game = GameRegistry.find(scope);
        if (game == null) return new int[0];
        if (config == null) return scope == StatScope.BUILD_BATTLE || scope == StatScope.TNT_RUN
            ? game.defaultColumns() : new int[0];

        if (scope == StatScope.BUILD_BATTLE) {
            //? if ornithe {
            return StatOrder.toColumns(StatOrder.BUILD_BATTLE, config.buildBattleStatOrder);
            //?} else {
            /*return new int[] {
                config.buildBattleCustomStat1, config.buildBattleCustomStat2, config.buildBattleCustomStat3, config.buildBattleCustomStat4, config.buildBattleCustomStat5, config.buildBattleCustomStat6, config.buildBattleCustomStat7, config.buildBattleCustomStat8, config.buildBattleCustomStat9, config.buildBattleCustomStat10
            };
            *///?}
        }

        if (scope == StatScope.TNT_RUN) {
            //? if ornithe {
            return StatOrder.toColumns(StatOrder.TNT_RUN, config.tntRunStatOrder);
            //?} else {
            /*return new int[] {
                config.tntRunCustomStat1, config.tntRunCustomStat2, config.tntRunCustomStat3, config.tntRunCustomStat4, config.tntRunCustomStat5, config.tntRunCustomStat6, config.tntRunCustomStat7, config.tntRunCustomStat8, config.tntRunCustomStat9, config.tntRunCustomStat10
            };
            *///?}
        }

        if (scope == StatScope.SKYWARS) {
            //? if ornithe {
            return StatOrder.toColumns(StatOrder.SKYWARS, config.skywarsStatOrder);
            //?} else {
            /*return new int[] {
                config.skywarsCustomStat1,
                config.skywarsCustomStat2,
                config.skywarsCustomStat3,
                config.skywarsCustomStat4,
                config.skywarsCustomStat5,
                config.skywarsCustomStat6,
                config.skywarsCustomStat7,
                config.skywarsCustomStat8,
                config.skywarsCustomStat9,
                config.skywarsCustomStat10,
            };
            *///?}
        }

        if (scope == StatScope.DUELS) {
            //? if ornithe {
            return StatOrder.toColumns(StatOrder.DUELS, config.duelsStatOrder);
            //?} else {
            /*return new int[] {
                config.duelsCustomStat1,
                config.duelsCustomStat2,
                config.duelsCustomStat3,
                config.duelsCustomStat4,
                config.duelsCustomStat5,
                config.duelsCustomStat6,
                config.duelsCustomStat7,
                config.duelsCustomStat8,
                config.duelsCustomStat9,
                config.duelsCustomStat10,
            };
            *///?}
        }
        if (scope != StatScope.BEDWARS) return game.defaultColumns();
        //? if ornithe {
        return StatOrder.toColumns(StatOrder.BEDWARS, config.bedwarsStatOrder);
        //?} else {
        /*
        return new int[] {
            config.customStat1,
            config.customStat2,
            config.customStat3,
            config.customStat4,
            config.customStat5,
            config.customStat6,
            config.customStat7,
            config.customStat8,
            config.customStat9,
            config.customStat10,
        };
        *///?}
    }

    public static List<Integer> getConfiguredColumns(StatScope scope, MellowOneConfig config) {
        List<Integer> result = new ArrayList<>();
        for (int column : getConfiguredStatsForScope(scope, config)) {
            if (isSupportedColumn(scope, column)) result.add(column);
        }
        return result;
    }
    public static StatDefinition definition(StatScope scope, int column) {
        GameDefinition<?> game = GameRegistry.find(scope);
        return game == null ? null : game.column(column);
    }
    private static int index(StatScope scope, String id) {
        GameDefinition<?> game = GameRegistry.find(scope);
        return game == null ? -1 : game.columnIndex(id);
    }
    public static int getNoneIndex(StatScope scope) { return index(scope, "none"); }
    public static int getTagsColumnIndex(StatScope scope) { return index(scope, "tags"); }
    public static int getPingColumnIndex(StatScope scope) { return index(scope, "ping"); }
    public static boolean isTagsColumn(StatScope scope, int column) { return hasStyle(scope, column, StatDefinition.Style.TAGS); }
    public static boolean isPingColumn(StatScope scope, int column) { return hasStyle(scope, column, StatDefinition.Style.PING); }
    public static boolean isHealthColumn(StatScope scope, int column) { return hasStyle(scope, column, StatDefinition.Style.HEALTH); }
    private static boolean hasStyle(StatScope scope, int column, StatDefinition.Style style) {
        StatDefinition stat = definition(scope, column);
        return stat != null && stat.style() == style;
    }
    public static boolean isSupportedColumn(StatScope scope, int column) {
        StatDefinition stat = definition(scope, column);
        return stat != null && stat.style() != StatDefinition.Style.NONE;
    }
    public static String getHeaderLabel(StatScope scope, int column) {
        StatDefinition stat = definition(scope, column);
        return stat == null ? "" : stat.label();
    }
    public static int getMinimumColumnWidth(StatScope scope, int column) {
        StatDefinition stat = definition(scope, column);
        return stat == null ? 36 : stat.width();
    }
    public static int estimateTotalWidth(StatScope scope, MellowOneConfig config) {
        List<Integer> columns = getConfiguredColumns(scope, config);
        if (columns.isEmpty()) {
            return 0;
        }

        int total = 0;
        for (int i = 0; i < columns.size(); i++) {
            total += getMinimumColumnWidth(scope, columns.get(i));
            if (i > 0) {
                total += COLUMN_GAP;
            }
        }

        return total;
    }
}
