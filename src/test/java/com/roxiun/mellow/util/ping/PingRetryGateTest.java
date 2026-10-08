package com.roxiun.mellow.util.ping;

import org.junit.Assert;
import org.junit.Test;

public class PingRetryGateTest {

    @Test
    public void blocksRepeatedRequestsDuringBackoff() {
        PingRetryGate gate = new PingRetryGate();

        Assert.assertTrue(gate.tryMarkRequested("player-uuid"));
        Assert.assertFalse(gate.tryMarkRequested("player-uuid"));
    }

    @Test
    public void clearPlayerAllowsAnotherRequest() {
        PingRetryGate gate = new PingRetryGate();

        Assert.assertTrue(gate.tryMarkRequested("player-uuid"));
        gate.clearPlayer("player-uuid");

        Assert.assertTrue(gate.tryMarkRequested("player-uuid"));
    }

    @Test
    public void clearAllowsAnotherRequest() {
        PingRetryGate gate = new PingRetryGate();

        Assert.assertTrue(gate.tryMarkRequested("player-uuid"));
        gate.clear();

        Assert.assertTrue(gate.tryMarkRequested("player-uuid"));
    }
}
