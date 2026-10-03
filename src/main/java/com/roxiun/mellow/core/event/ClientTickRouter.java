package com.roxiun.mellow.core.event;

import com.roxiun.mellow.api.hypixel.HypixelFeatures;
import com.roxiun.mellow.feature.replay.ReplayManager;
import org.polyfrost.oneconfig.api.event.v1.invoke.impl.Subscribe;
import com.roxiun.mellow.platform.event.TickEvent;

public class ClientTickRouter {

    private final HypixelFeatures hypixelFeatures;

    public ClientTickRouter(HypixelFeatures hypixelFeatures) {
        this.hypixelFeatures = hypixelFeatures;
    }

    @Subscribe
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.START) {
            hypixelFeatures.onClientTick();
            ReplayManager
                .getInstance()
                .onClientTick(hypixelFeatures.getGameSnapshot());
        }
    }
}
