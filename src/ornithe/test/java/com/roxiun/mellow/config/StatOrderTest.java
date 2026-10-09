package com.roxiun.mellow.config;

import org.junit.Test;
import static org.junit.Assert.*;

public class StatOrderTest {
    @Test public void resolvesReorderedEnabledStatsForEachMode() {
        assertArrayEquals(new int[]{13, 2, 11}, StatOrder.toColumns(StatOrder.BEDWARS, new String[]{"Ping", "Name", "HP"}));
        assertArrayEquals(new int[]{1, 10, 8}, StatOrder.toColumns(StatOrder.SKYWARS, new String[]{"Level", "Ping", "HP"}));
        assertArrayEquals(new int[]{9, 1, 13}, StatOrder.toColumns(StatOrder.DUELS, new String[]{"Winstreak", "Division", "Ping"}));
    }
    @Test public void allUncheckedRemainsEmpty() {
        assertArrayEquals(new int[0], StatOrder.toColumns(StatOrder.BEDWARS, new String[0]));
    }
    @Test public void ignoresUnknownAndDuplicateSavedNames() {
        assertArrayEquals(new int[]{2, 0}, StatOrder.toColumns(StatOrder.BEDWARS, new String[]{null, "Name", "removed", "None", "Name", "Team"}));
    }
}
