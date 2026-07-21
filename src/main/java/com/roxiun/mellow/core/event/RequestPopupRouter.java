package com.roxiun.mellow.core.event;

import com.roxiun.mellow.Mellow;
import com.roxiun.mellow.config.MellowOneConfig;
import com.roxiun.mellow.feature.requestpopup.RequestPopupManager;
import com.roxiun.mellow.feature.requestpopup.RequestPopupPosition;
import java.awt.Color;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiChat;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.settings.GameSettings;
import net.minecraft.client.settings.KeyBinding;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.InputEvent;
import org.lwjgl.input.Keyboard;

public class RequestPopupRouter {

    private static final int MIN_WIDTH = 212;
    private static final int HEIGHT = 46;
    private static final int SCREEN_MARGIN = 12;
    private static final int TOP_CENTER_Y = 50;
    private static final int TOP_CORNER_Y = 12;
    private static final int BOTTOM_MARGIN = 14;

    private final Minecraft mc = Minecraft.getMinecraft();
    private final MellowOneConfig config;
    private final RequestPopupManager popupManager;
    private final KeyBinding acceptKeybind;
    private final KeyBinding denyKeybind;

    public RequestPopupRouter(
        MellowOneConfig config,
        RequestPopupManager popupManager,
        KeyBinding acceptKeybind,
        KeyBinding denyKeybind
    ) {
        this.config = config;
        this.popupManager = popupManager;
        this.acceptKeybind = acceptKeybind;
        this.denyKeybind = denyKeybind;
    }

    @SubscribeEvent
    public void onRenderOverlay(RenderGameOverlayEvent.Post event) {
        if (event.type != RenderGameOverlayEvent.ElementType.ALL) {
            return;
        }
        if (mc == null || mc.fontRendererObj == null || popupManager == null) {
            return;
        }
        if (!Mellow.isEnabled()) {
            return;
        }

        RequestPopupManager.ActiveRequest activeRequest = popupManager.getActiveRequest();
        if (activeRequest == null) {
            return;
        }

        renderPopup(activeRequest);
    }

    @SubscribeEvent
    public void onKeyInput(InputEvent.KeyInputEvent event) {
        if (
            config == null ||
            popupManager == null ||
            !Mellow.isEnabled() ||
            !config.requestPopupsEnabled ||
            !Keyboard.getEventKeyState() ||
            mc == null
        ) {
            return;
        }

        if (mc.currentScreen instanceof GuiChat) {
            return;
        }

        int keyCode = Keyboard.getEventKey();
        if (keyCode <= 0) {
            return;
        }

        if (
            acceptKeybind != null && keyCode == acceptKeybind.getKeyCode()
        ) {
            popupManager.handleDecision(true);
        } else if (
            denyKeybind != null && keyCode == denyKeybind.getKeyCode()
        ) {
            popupManager.handleDecision(false);
        }
    }

    private void renderPopup(RequestPopupManager.ActiveRequest activeRequest) {
        FontRenderer font = mc.fontRendererObj;
        ScaledResolution resolution = new ScaledResolution(mc);
        long now = System.currentTimeMillis();

        String title = activeRequest.getDisplayText();
        String keyHints =
            "§a[" +
            getKeyName(acceptKeybind) +
            "] Accept §c[" +
            getKeyName(denyKeybind) +
            "] Deny";

        int contentWidth = Math.max(
            font.getStringWidth(title),
            font.getStringWidth(keyHints)
        );
        int width = Math.max(MIN_WIDTH, contentWidth + 20);
        int[] anchor = resolveAnchor(resolution, width, HEIGHT);
        int x = anchor[0];
        int y = anchor[1];

        int backgroundColor = new Color(20, 20, 20, 190).getRGB();
        int borderColor = new Color(55, 55, 55, 220).getRGB();
        int progressBg = new Color(70, 70, 70, 190).getRGB();
        int progressFill = new Color(130, 220, 130, 220).getRGB();

        Gui.drawRect(x, y, x + width, y + HEIGHT, backgroundColor);
        Gui.drawRect(x, y, x + width, y + 1, borderColor);
        Gui.drawRect(x, y + HEIGHT - 1, x + width, y + HEIGHT, borderColor);
        Gui.drawRect(x, y, x + 1, y + HEIGHT, borderColor);
        Gui.drawRect(x + width - 1, y, x + width, y + HEIGHT, borderColor);

        int titleX = x + (width - font.getStringWidth(title)) / 2;
        int hintX = x + (width - font.getStringWidth(keyHints)) / 2;
        font.drawStringWithShadow(title, titleX, y + 10, 0xFFFFFF);
        font.drawStringWithShadow(keyHints, hintX, y + 24, 0xFFFFFF);

        float progress = activeRequest.getRemainingProgress(now);
        int progressWidth = width - 2;
        int fillWidth = (int) (progressWidth * progress);
        int progressStartY = y + HEIGHT - 3;

        Gui.drawRect(
            x + 1,
            progressStartY,
            x + 1 + progressWidth,
            progressStartY + 2,
            progressBg
        );
        if (fillWidth > 0) {
            Gui.drawRect(
                x + 1,
                progressStartY,
                x + 1 + fillWidth,
                progressStartY + 2,
                progressFill
            );
        }
    }

    private int[] resolveAnchor(
        ScaledResolution resolution,
        int width,
        int height
    ) {
        RequestPopupPosition position = RequestPopupPosition.fromConfigIndex(
            config == null ? 0 : config.requestPopupPosition
        );

        int x;
        int y;

        switch (position) {
            case TOP_RIGHT:
                x = resolution.getScaledWidth() - width - SCREEN_MARGIN;
                y = TOP_CORNER_Y;
                break;
            case BOTTOM_RIGHT:
                x = resolution.getScaledWidth() - width - SCREEN_MARGIN;
                y = resolution.getScaledHeight() - height - BOTTOM_MARGIN;
                break;
            case TOP_CENTER:
            default:
                x = (resolution.getScaledWidth() - width) / 2;
                y = TOP_CENTER_Y;
                break;
        }

        if (x < SCREEN_MARGIN) {
            x = SCREEN_MARGIN;
        }
        if (y < SCREEN_MARGIN) {
            y = SCREEN_MARGIN;
        }
        return new int[] { x, y };
    }

    private String getKeyName(KeyBinding keyBinding) {
        if (keyBinding == null) {
            return "?";
        }
        return GameSettings.getKeyDisplayString(keyBinding.getKeyCode());
    }
}
