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

        Assert.assertTrue(message.contains("queue dodging"));
    }

    @Test
    public void localBlacklistMessageHidesImportPlaceholderReason() {
        String message = BedwarsCommand.formatLocalBlacklistMessage(
            new BlacklistedPlayer("Alpha", "Added from external file")
        );

        Assert.assertTrue(message.contains("blacklist"));
        Assert.assertFalse(message.contains("Added from external file"));
    }
}
