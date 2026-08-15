package com.roxiun.mellow.feature.replay;

import org.junit.Assert;
import org.junit.Test;

public class ReplayLocalPlayerPacketRecorderTest {

    @Test
    public void poseFlagsReflectClientSneakingAndSprintingState() {
        byte unrelatedFlags = (byte) ((1 << 0) | (1 << 4) | (1 << 5));

        byte both = ReplayLocalPlayerPacketRecorder.withLocalPlayerPoseFlags(
            unrelatedFlags,
            true,
            true
        );
        Assert.assertEquals(unrelatedFlags | (1 << 1) | (1 << 3), both & 0xFF);

        byte neither = ReplayLocalPlayerPacketRecorder.withLocalPlayerPoseFlags(
            (byte) 0xFF,
            false,
            false
        );
        Assert.assertEquals(0xF5, neither & 0xFF);
    }
}
