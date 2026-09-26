package com.roxiun.mellow.data;

import com.roxiun.mellow.api.coral.CoralTag;
import com.roxiun.mellow.api.provider.model.StatScope;
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
            .withTags(Collections.singletonList(tag), null);

        TabStats tabStats = profile.getTabStats(StatScope.BEDWARS);

        Assert.assertNotNull(tabStats);
        Assert.assertEquals("Player", tabStats.getFormattedNameWithRank());
        Assert.assertTrue(tabStats.isCoralTagged());
    }

    @Test
    public void formatTabCountForDisplayAddsCommas() {
        Assert.assertEquals("§c12,345", PlayerProfile.formatTabCountForDisplay("§c12345"));
    }

    @Test
    public void formatTabCountForDisplayLeavesRatiosUnchanged() {
        Assert.assertEquals("§e1.23", PlayerProfile.formatTabCountForDisplay("§e1.23"));
    }
}
