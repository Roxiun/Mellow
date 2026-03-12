package com.roxiun.mellow.feature.replay;

import com.roxiun.mellow.Mellow;
import net.minecraft.client.Minecraft;
import net.minecraftforge.client.event.MouseEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class ReplayInputRouter {

    private final Minecraft mc = Minecraft.getMinecraft();
    private final ReplayManager replayManager;

    public ReplayInputRouter(ReplayManager replayManager) {
        this.replayManager = replayManager;
    }

    @SubscribeEvent
    public void onMouse(MouseEvent event) {
        if (!Mellow.isEnabled()) {
            return;
        }
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
