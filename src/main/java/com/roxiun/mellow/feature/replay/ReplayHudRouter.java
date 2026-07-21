package com.roxiun.mellow.feature.replay;

import com.roxiun.mellow.Mellow;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class ReplayHudRouter {

    private final ReplayManager replayManager;

    public ReplayHudRouter(ReplayManager replayManager) {
        this.replayManager = replayManager;
    }

    @SubscribeEvent
    public void onRenderOverlay(RenderGameOverlayEvent.Text event) {
        if (!Mellow.isEnabled() || !replayManager.isPlaybackActive()) {
            return;
        }

        Minecraft mc = Minecraft.getMinecraft();
        if (mc == null || mc.fontRendererObj == null || mc.gameSettings.hideGUI) {
            return;
        }

        List<String> lines = replayManager.getHudLines();
        if (lines.isEmpty()) {
            return;
        }

        ScaledResolution resolution = new ScaledResolution(mc);
        int y = 8;
        for (String line : lines) {
            int width = mc.fontRendererObj.getStringWidth(line);
            int x = resolution.getScaledWidth() - width - 8;
            mc.fontRendererObj.drawStringWithShadow(line, (float) x, (float) y, 0xFFFFFF);
            y += 10;
        }
    }
}
