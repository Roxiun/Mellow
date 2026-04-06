package com.roxiun.mellow.feature.nicks;

import com.roxiun.mellow.util.localdenick.LocalDenickManager;
import java.io.File;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import org.junit.Assert;
import org.junit.Test;

public class NickUtilsLocalDenickTest {

    @Test
    public void resolvesLocalDenickNameFromManager() throws Exception {
        File tempDir = Files.createTempDirectory("nickutils-localdenick-test").toFile();
        tempDir.deleteOnExit();

        LocalDenickManager manager = LocalDenickManager.createForTests(tempDir);
        manager.addPlayer(
            UUID.fromString("00000000-0000-0000-0000-000000000020"),
            "RealPlayer",
            "NickedName"
        );

        Assert.assertEquals(
            "RealPlayer",
            NickUtils.resolveLocalDenickName("NickedName", manager)
        );
        Assert.assertNull(NickUtils.resolveLocalDenickName("OtherNick", manager));
    }

    @Test
    public void shouldResolveLocalNickOnlyOncePerNick() {
        Set<String> nickedPlayers = new HashSet<>();

        Assert.assertTrue(
            NickUtils.shouldResolveLocalNick(nickedPlayers, "NickedName")
        );
        Assert.assertFalse(
            NickUtils.shouldResolveLocalNick(nickedPlayers, "NickedName")
        );
    }

    @Test
    public void localDenickShouldNotPrintStatsMessage() {
        Assert.assertFalse(NickUtils.shouldPrintDenickStats(false));
        Assert.assertTrue(NickUtils.shouldPrintDenickStats(true));
    }

    @Test
    public void shouldMatchVisibleNickIgnoringCase() {
        Assert.assertTrue(
            NickUtils.isNickVisibleInTabList(
                "NickedName",
                Arrays.asList("nickedname", "OtherPlayer")
            )
        );
        Assert.assertFalse(
            NickUtils.isNickVisibleInTabList(
                "MissingNick",
                Arrays.asList("nickedname", "OtherPlayer")
            )
        );
    }

    @Test
    public void shouldRefreshLocalNickOnlyWhenRealNameDiffers() {
        Assert.assertTrue(NickUtils.shouldRefreshLocalNick("NickedName", "RealPlayer"));
        Assert.assertFalse(
            NickUtils.shouldRefreshLocalNick("NickedName", "NickedName")
        );
        Assert.assertFalse(NickUtils.shouldRefreshLocalNick("", "RealPlayer"));
        Assert.assertFalse(NickUtils.shouldRefreshLocalNick("NickedName", ""));
    }

    @Test
    public void normalizeNickKeyTrimsAndLowercases() {
        Assert.assertEquals("nickedname", NickUtils.normalizeNickKey(" NickedName "));
        Assert.assertEquals("", NickUtils.normalizeNickKey("   "));
        Assert.assertEquals("", NickUtils.normalizeNickKey(null));
    }
}
