package com.roxiun.mellow.feature.stats;

import com.roxiun.mellow.api.bedwars.BedwarsPlayer;
import com.roxiun.mellow.api.provider.model.StatScope;
import com.roxiun.mellow.api.tags.*;
import com.roxiun.mellow.data.*;
import java.util.*;
import org.junit.Test;
import static org.junit.Assert.*;

public class StatsCheckerTest {
    private final TagReport tags = new TagReport(Collections.singletonList(
        new PlayerTag("Coral", "sniper", "Sniper", "S", true)), Collections.emptyMap());

    private PlayerProfile profile(double fkdr, TagReport report) {
        return new PlayerProfile("uuid", "Player",
            new BedwarsPlayer("Player", "100", fkdr, 0, 0, 0, 0, 0, 0, 0, 0),
            null, null, null, null, report);
    }

    @Test public void belowThresholdStatsRemoveAnEarlyTagRowAndRejectLaterTags() {
        TabStats earlyTags = StatsChecker.mergeTabRow(null,
            PlayerProfile.identity("uuid", "Player").withTags(tags), StatScope.BEDWARS, 5, true);
        assertNotNull(earlyTags);
        assertNull(StatsChecker.mergeTabRow(earlyTags, profile(1, tags), StatScope.BEDWARS, 5, true));
        assertNull(StatsChecker.mergeTabRow(null, profile(1, TagReport.empty()), StatScope.BEDWARS, 5, true));
        assertNull(StatsChecker.mergeTabRow(null, profile(1, tags), StatScope.BEDWARS, 5, true));
    }

    @Test public void qualifyingStatsKeepTagsAndMissingStatsKeepTheExistingRow() {
        TabStats stats = StatsChecker.mergeTabRow(null, profile(5, tags), StatScope.BEDWARS, 5, true);
        assertNotNull(stats);
        assertTrue(stats.getTags().has("Coral"));
        TabStats refreshedTags = StatsChecker.mergeTabRow(stats,
            PlayerProfile.identity("uuid", "Player").withTags(tags), StatScope.BEDWARS, 5, true);
        assertEquals(stats.getFkdr(), refreshedTags.getFkdr());
        assertTrue(refreshedTags.getTags().has("Coral"));
        assertNull(StatsChecker.mergeTabRow(null, PlayerProfile.identity("uuid", "Player"),
            StatScope.BEDWARS, 5, false));
    }

}
