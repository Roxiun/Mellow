package com.roxiun.mellow.commands;

import com.roxiun.mellow.util.blacklist.BlacklistedPlayer;
import org.junit.Assert;
import org.junit.Test;

public class BedwarsCommandTest {

    @Test
    public void localBlacklistMessageIncludesSavedReason() {
        String message = BedwarsCommand.formatLocalBlacklistMessage(
            new BlacklistedPlayer("Alpha", "queue dodging")
        );

        Assert.assertEquals(
            "§6§lLocal§r§6: This player is on your blacklist: queue dodging",
            message
        );
    }

    @Test
    public void localBlacklistMessageHidesImportPlaceholderReason() {
        String message = BedwarsCommand.formatLocalBlacklistMessage(
            new BlacklistedPlayer("Alpha", "Added from external file")
        );

        Assert.assertEquals(
            "§6§lLocal§r§6: This player is on your blacklist",
            message
        );
    }
}
