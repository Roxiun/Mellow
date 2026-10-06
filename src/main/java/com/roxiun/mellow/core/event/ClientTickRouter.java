package com.roxiun.mellow.core.event;

import com.roxiun.mellow.api.hypixel.HypixelFeatures;
import com.roxiun.mellow.feature.replay.ReplayManager;
import org.polyfrost.oneconfig.api.event.v1.invoke.impl.Subscribe;
import org.polyfrost.oneconfig.api.event.v1.events.TickEvent;

public class ClientTickRouter {

    private final HypixelFeatures hypixelFeatures;

    public ClientTickRouter(HypixelFeatures hypixelFeatures) {
        this.hypixelFeatures = hypixelFeatures;
    }

    @Subscribe
    public void onClientTick(TickEvent.Start event) {
        hypixelFeatures.onClientTick();
        ReplayManager
            .getInstance()
            .onClientTick(hypixelFeatures.getGameSnapshot());
    }
}
