package com.roxiun.mellow.feature.replay;

import java.util.Arrays;
import java.util.List;
import net.minecraft.nbt.NBTTagCompound;
import org.junit.Assert;
import org.junit.Test;

public class ReplayPlaybackSessionTest {

    @Test
    public void sortTeleportTargetsOrdersByTeamThenPlayerName() {
        List<ReplayPlaybackSession.TeleportTarget> sorted =
            ReplayPlaybackSession.sortTeleportTargets(
                Arrays.asList(
                    new ReplayPlaybackSession.TeleportTarget(
                        "zoe",
                        "§czoe",
                        "red",
                        "§cRed"
                    ),
                    new ReplayPlaybackSession.TeleportTarget(
                        "bob",
                        "§9bob",
                        "blue",
                        "§9Blue"
                    ),
                    new ReplayPlaybackSession.TeleportTarget(
                        "amy",
                        "§9amy",
                        "blue",
                        "§9Blue"
                    ),
                    new ReplayPlaybackSession.TeleportTarget(
                        "solo",
                        "solo",
                        "\uFFFF",
                        "§7Unassigned"
                    )
                )
            );

        Assert.assertEquals("amy", sorted.get(0).getName());
        Assert.assertEquals("bob", sorted.get(1).getName());
        Assert.assertEquals("zoe", sorted.get(2).getName());
        Assert.assertEquals("solo", sorted.get(3).getName());
    }

    @Test
    public void normalizeHypixelTeamNameCollapsesSplitHypixelBuckets() {
        Assert.assertEquals(
            "Blue",
            ReplayPlaybackSession.normalizeHypixelTeamName("Blue0", "§9")
        );
        Assert.assertEquals(
            "Green",
            ReplayPlaybackSession.normalizeHypixelTeamName("Green10", "§a")
        );
        Assert.assertEquals(
            "Pink",
            ReplayPlaybackSession.normalizeHypixelTeamName("", "§d")
        );
    }

    @Test
    public void resolveSeekModeTreatsForwardSkipsAsIncremental() {
        Assert.assertEquals(
            ReplayPlaybackSession.SeekMode.FORWARD,
            ReplayPlaybackSession.resolveSeekMode(15_000, 20_000)
        );
    }

    @Test
    public void resolveSeekModeTreatsRewindsAsRebuilds() {
        Assert.assertEquals(
            ReplayPlaybackSession.SeekMode.REBUILD,
            ReplayPlaybackSession.resolveSeekMode(20_000, 15_000)
        );
    }

    @Test
    public void resolveSeekModeTreatsSameTargetAsNoOp() {
        Assert.assertEquals(
            ReplayPlaybackSession.SeekMode.NONE,
            ReplayPlaybackSession.resolveSeekMode(20_000, 20_000)
        );
    }

    @Test
    public void withSkullOwnerTagPreservesExistingDisplayData() {
        NBTTagCompound existingTag = new NBTTagCompound();
        NBTTagCompound display = new NBTTagCompound();
        display.setString("Name", "Replay Control");
        existingTag.setTag("display", display);

        NBTTagCompound mergedTag = ReplayPlaybackSession.withSkullOwnerTag(
            existingTag,
            "http://textures.minecraft.net/texture/example"
        );

        Assert.assertEquals(
            "Replay Control",
            mergedTag.getCompoundTag("display").getString("Name")
        );
        Assert.assertTrue(mergedTag.hasKey("SkullOwner"));
    }
}
