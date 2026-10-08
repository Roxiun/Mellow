package com.roxiun.mellow.core.event;

import com.roxiun.mellow.api.hypixel.HypixelFeatures;
import com.roxiun.mellow.feature.nicks.NickUtils;
import com.roxiun.mellow.feature.nicks.NumberDenicker;
import com.roxiun.mellow.feature.replay.ReplayManager;
import com.roxiun.mellow.feature.stats.PregameStats;
//? if ornithe {
import com.roxiun.mellow.platform.event.WorldEvent;
import org.polyfrost.oneconfig.api.event.v1.invoke.impl.Subscribe;
//?} else {
/*import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
*///?}

public class WorldLifecycleRouter {

    private final NumberDenicker numberDenicker;
    private final PregameStats pregameStats;
    private final NickUtils nickUtils;

    public WorldLifecycleRouter(
        NumberDenicker numberDenicker,
        PregameStats pregameStats,
        NickUtils nickUtils
    ) {
        this.numberDenicker = numberDenicker;
        this.pregameStats = pregameStats;
        this.nickUtils = nickUtils;
    }

    //? if ornithe {
    @Subscribe
    //?} else {
    /*@SubscribeEvent
    *///?}
    public void onWorldLoad(WorldEvent.Load event) {
        if (!event.world.isRemote) return;
        numberDenicker.onWorldChange();
        pregameStats.onWorldChange();
        nickUtils.clearNicks();

        HypixelFeatures.getInstance().onWorldChange();
        ReplayManager.getInstance().onWorldChange();
    }
    //? if ornithe {
    @Subscribe
    public void onDisconnect(WorldEvent.Disconnected event) {
    //?} else {
    /*@SubscribeEvent
    public void onDisconnect(net.minecraftforge.fml.common.network.FMLNetworkEvent.ClientDisconnectionFromServerEvent event) {
    *///?}

        net.minecraft.client.Minecraft.getMinecraft().addScheduledTask(() -> {
            numberDenicker.onWorldChange();
            pregameStats.onWorldChange();
            nickUtils.clearNicks();
            HypixelFeatures.getInstance().onDisconnect();
            ReplayManager.getInstance().onWorldChange();
        });
    }
}
