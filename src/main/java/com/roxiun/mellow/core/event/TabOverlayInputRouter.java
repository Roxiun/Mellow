package com.roxiun.mellow.core.event;

import com.roxiun.mellow.feature.stats.tab.ExtendedStatsTabOverlay;
import net.minecraft.client.Minecraft;
import net.minecraftforge.client.event.MouseEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class TabOverlayInputRouter {

    private final Minecraft mc = Minecraft.getMinecraft();
    private final TabOverlayRouter tabOverlayRouter;

    public TabOverlayInputRouter(TabOverlayRouter tabOverlayRouter) {
        this.tabOverlayRouter = tabOverlayRouter;
    }

    @SubscribeEvent
    public void onMouse(MouseEvent event) {
        if (event.dwheel == 0) {
            return;
        }

        if (mc == null || mc.currentScreen != null || mc.gameSettings == null) {
            return;
        }

        if (!tabOverlayRouter.isTabOverlayInputActive()) {
            return;
        }

        if (mc.getNetHandler() == null || mc.getNetHandler().getPlayerInfoMap() == null) {
            return;
        }

        ExtendedStatsTabOverlay overlay = tabOverlayRouter.getOverlay();
        if (overlay == null) {
            return;
        }

        if (overlay.handleMouseWheel(event.dwheel)) event.setCanceled(true);
    }
}
