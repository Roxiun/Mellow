package com.roxiun.mellow.core.event;

import com.roxiun.mellow.feature.stats.tab.ExtendedStatsTabOverlay;
import net.minecraft.client.Minecraft;
import com.roxiun.mellow.platform.event.MouseEvent;
import org.polyfrost.oneconfig.api.event.v1.invoke.impl.Subscribe;

public class TabOverlayInputRouter {

    private final Minecraft mc = Minecraft.getMinecraft();
    private final TabOverlayRouter tabOverlayRouter;

    public TabOverlayInputRouter(TabOverlayRouter tabOverlayRouter) {
        this.tabOverlayRouter = tabOverlayRouter;
    }

    @Subscribe
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

        overlay.handleMouseWheel(event.dwheel, mc.getNetHandler().getPlayerInfoMap().size());
    }
}
