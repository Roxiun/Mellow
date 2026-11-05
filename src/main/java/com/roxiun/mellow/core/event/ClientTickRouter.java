package com.roxiun.mellow.core.event;

import com.roxiun.mellow.api.hypixel.HypixelFeatures;
import com.roxiun.mellow.feature.replay.ReplayManager;
//? if ornithe {
import org.polyfrost.oneconfig.api.event.v1.invoke.impl.Subscribe;
import org.polyfrost.oneconfig.api.event.v1.events.TickEvent;
//?} else {
/*import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
*///?}

public class ClientTickRouter {

    private final HypixelFeatures hypixelFeatures;
    private int ticks;

    public ClientTickRouter(HypixelFeatures hypixelFeatures) {
        this.hypixelFeatures = hypixelFeatures;
    }

    //? if ornithe {
    @Subscribe
    public void onClientTick(TickEvent.Start event) {

    //?} else {
    /*@SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.START) {
    *///?}
            hypixelFeatures.onClientTick();
            if (++ticks % 20 == 0) com.roxiun.mellow.util.ping.PingProviderUtils.warmTab(
                hypixelFeatures.getGameSnapshot(), com.roxiun.mellow.Mellow.config);
            if (com.roxiun.mellow.Mellow.partyBlacklistWarningService != null)
                com.roxiun.mellow.Mellow.partyBlacklistWarningService.onSnapshotUpdate(hypixelFeatures.getGameSnapshot());
            if (com.roxiun.mellow.Mellow.inGameTabStatsSyncService != null)
                com.roxiun.mellow.Mellow.inGameTabStatsSyncService.onSnapshotUpdate(hypixelFeatures.getGameSnapshot());
            ReplayManager
                .getInstance()
                .onClientTick(hypixelFeatures.getGameSnapshot());
        //? if forge {
        /*}
        *///?}
    }
}
