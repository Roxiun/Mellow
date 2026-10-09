package com.roxiun.mellow.feature.replay;

import net.minecraft.client.Minecraft;
//? if ornithe {
import com.roxiun.mellow.platform.event.MouseEvent;
import org.polyfrost.oneconfig.api.event.v1.invoke.impl.Subscribe;
//?} else {
/*import net.minecraftforge.client.event.MouseEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
*///?}

public class ReplayInputRouter {

    private final Minecraft mc = Minecraft.getMinecraft();
    private final ReplayManager replayManager;

    public ReplayInputRouter(ReplayManager replayManager) {
        this.replayManager = replayManager;
    }

    //? if ornithe {
    @Subscribe
    //?} else {
    /*@SubscribeEvent
    *///?}
    public void onMouse(MouseEvent event) {
        if (event.dwheel != 0 || !event.buttonstate) {
            return;
        }

        if (event.button != 0 && event.button != 1) {
            return;
        }

        if (mc == null || mc.currentScreen != null || mc.thePlayer == null) {
            return;
        }

        if (replayManager.handlePlaybackControlClick(event.button)) {
            event.setCanceled(true);
        }
    }
}
