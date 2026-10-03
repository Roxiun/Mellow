package com.roxiun.mellow.feature.replay;

import net.minecraft.client.Minecraft;
import com.roxiun.mellow.platform.event.MouseEvent;
import org.polyfrost.oneconfig.api.event.v1.invoke.impl.Subscribe;

public class ReplayInputRouter {

    private final Minecraft mc = Minecraft.getMinecraft();
    private final ReplayManager replayManager;

    public ReplayInputRouter(ReplayManager replayManager) {
        this.replayManager = replayManager;
    }

    @Subscribe
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
