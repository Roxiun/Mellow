package com.roxiun.mellow.autoupdate;

import org.junit.Assert;
import org.junit.Test;

public class ModrinthUpdaterTest {

    @Test
    public void detectsNewerVersions() {
        Assert.assertTrue(
            ModrinthUpdater.isRemoteVersionNewer("6.1.1", "6.1.2")
        );
        Assert.assertTrue(
            ModrinthUpdater.isRemoteVersionNewer("6.1.1", "7.0.0")
        );
        Assert.assertTrue(
            ModrinthUpdater.isRemoteVersionNewer("6.1", "6.1.1")
        );
    }

    @Test
    public void ignoresSameAndOlderVersions() {
        Assert.assertFalse(
            ModrinthUpdater.isRemoteVersionNewer("6.1.1", "6.1.1")
        );
        Assert.assertFalse(
            ModrinthUpdater.isRemoteVersionNewer("6.1.1", "6.1.0")
        );
        Assert.assertFalse(
            ModrinthUpdater.isRemoteVersionNewer("7.0.0", "6.9.9")
        );
        Assert.assertFalse(
            ModrinthUpdater.isRemoteVersionNewer("6.1.0", "6.1")
        );
    }
}
