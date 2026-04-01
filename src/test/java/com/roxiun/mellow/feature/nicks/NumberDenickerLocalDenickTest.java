package com.roxiun.mellow.feature.nicks;

import com.roxiun.mellow.util.localdenick.LocalDenickManager;
import java.io.File;
import java.nio.file.Files;
import java.util.UUID;
import org.junit.Assert;
import org.junit.Test;

public class NumberDenickerLocalDenickTest {

    @Test
    public void shouldSkipLookupWhenNickIsLocallyBlocked() throws Exception {
        File tempDir = Files.createTempDirectory("number-denick-test").toFile();
        tempDir.deleteOnExit();

        LocalDenickManager manager = LocalDenickManager.createForTests(tempDir);
        manager.addPlayer(
            UUID.fromString("00000000-0000-0000-0000-000000000010"),
            "RealPlayer",
            "NickedName"
        );

        Assert.assertTrue(NumberDenicker.shouldSkipLookup("NickedName", manager));
        Assert.assertFalse(NumberDenicker.shouldSkipLookup("OtherNick", manager));
    }
}
