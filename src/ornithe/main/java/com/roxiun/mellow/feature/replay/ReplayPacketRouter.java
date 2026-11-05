package com.roxiun.mellow.feature.replay;

import org.polyfrost.oneconfig.api.event.v1.events.PacketEvent;
import org.polyfrost.oneconfig.api.event.v1.invoke.impl.Subscribe;

public final class ReplayPacketRouter {
    private final ReplayManager replayManager;

    public ReplayPacketRouter(ReplayManager replayManager) {
        this.replayManager = replayManager;
    }

    // Record the inbound stream before ordinary handlers can cancel packets, as the old HEAD hook did.
    @Subscribe(priority = Integer.MAX_VALUE)
    public void onReceive(PacketEvent.Receive event) {
        replayManager.onInboundPacket(event.getPacket());
    }
}
