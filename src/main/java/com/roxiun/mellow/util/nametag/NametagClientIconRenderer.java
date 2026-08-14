package com.roxiun.mellow.util.nametag;

import com.roxiun.mellow.api.seraph.SeraphClientType;
import com.roxiun.mellow.util.render.SeraphIconRenderer;
import net.minecraft.client.gui.FontRenderer;

public final class NametagClientIconRenderer {

    private static final int ICON_SIZE = 8;
    private static final int ICON_GAP = 2;

    private NametagClientIconRenderer() {}

    public static int adjustWidth(String text, int originalWidth) {
        if (!shouldDecorate(text)) {
            return originalWidth;
        }
        return originalWidth + getReservedWidth();
    }

    public static int adjustTextX(String text, int originalX) {
        if (!shouldDecorate(text) || !NametagRenderContext.isClientIconLeft()) {
            return originalX;
        }
        return originalX + getReservedWidth();
    }

    public static float adjustTextX(String text, float originalX) {
        if (!shouldDecorate(text) || !NametagRenderContext.isClientIconLeft()) {
            return originalX;
        }
        return originalX + getReservedWidth();
    }

    public static void drawActiveIcon(
        FontRenderer fontRenderer,
        String text,
        int textX,
        int y,
        int color
    ) {
        drawActiveIcon(fontRenderer, text, (float) textX, (float) y, color);
    }

    public static void drawActiveIcon(
        FontRenderer fontRenderer,
        String text,
        float textX,
        float y,
        int color
    ) {
        SeraphClientType clientType = NametagRenderContext.getClientType();
        if (clientType == null || fontRenderer == null || !shouldDecorate(text)) {
            return;
        }

        int textWidth = fontRenderer.getStringWidth(text == null ? "" : text);
        float iconX = NametagRenderContext.isClientIconLeft()
            ? textX - getReservedWidth()
            : textX + textWidth + ICON_GAP;
        SeraphIconRenderer.drawClientIcon(
            clientType,
            Math.round(iconX),
            Math.round(y),
            ICON_SIZE,
            resolveAlpha(color)
        );
    }

    private static int getReservedWidth() {
        return NametagRenderContext.getClientType() == null ? 0 : ICON_SIZE + ICON_GAP;
    }

    private static boolean shouldDecorate(String text) {
        SeraphClientType clientType = NametagRenderContext.getClientType();
        String primaryLabelText = NametagRenderContext.getPrimaryLabelText();
        return (
            clientType != null &&
            primaryLabelText != null &&
            primaryLabelText.equals(text)
        );
    }

    private static float resolveAlpha(int color) {
        int alpha = color >>> 24;
        if (alpha == 0) {
            alpha = 255;
        }
        return alpha / 255.0F;
    }
}
