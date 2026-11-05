package com.roxiun.mellow.data;

import com.roxiun.mellow.api.tags.TagReport;
import com.roxiun.mellow.api.coral.CoralTag;
import com.roxiun.mellow.stats.StatScope;
import java.util.Collections;
import org.junit.Assert;
import org.junit.Test;

public class PlayerProfileTabFormattingTest {

    @Test
    public void identityOnlyProfileStillProvidesTagCapableTabStats() {
        CoralTag tag = new CoralTag(
            "confirmed_cheater",
            "reason",
            0L,
            false,
            null,
            null,
            null
        );
        PlayerProfile profile = PlayerProfile
            .identity("uuid", "Player")
            .withTags(TagReport.nativeTags(Collections.singletonList(tag), null, Collections.emptyMap()));

        TabStats tabStats = profile.getTabStats(StatScope.BEDWARS);

        Assert.assertNotNull(tabStats);
        Assert.assertEquals("Player", tabStats.getFormattedNameWithRank());
        Assert.assertTrue(tabStats.getTags().has("Coral"));
    }

    @Test
    public void formatTabCountForDisplayAddsCommas() {
        Assert.assertEquals("§c12,345", com.roxiun.mellow.stats.StatFormatting.formatTabCountForDisplay("§c12345"));
    }

    @Test
    public void formatTabCountForDisplayLeavesRatiosUnchanged() {
        Assert.assertEquals("§e1.23", com.roxiun.mellow.stats.StatFormatting.formatTabCountForDisplay("§e1.23"));
    }
}
