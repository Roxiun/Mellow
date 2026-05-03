package com.roxiun.mellow.util.render;

import com.roxiun.mellow.api.urchin.UrchinTag;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.ResourceLocation;

public final class UrchinTagIconRenderer {

    private static final int SOURCE_SIZE = 56;
    private static final ResourceLocation SNIPER = new ResourceLocation(
        "mellow", "textures/tags/urchin/sniper.png"
    );
    private static final ResourceLocation BLATANT_CHEATER = new ResourceLocation(
        "mellow", "textures/tags/urchin/blatant_cheater.png"
    );
    private static final ResourceLocation CONFIRMED_CHEATER = new ResourceLocation(
        "mellow", "textures/tags/urchin/confirmed_cheater.png"
    );
    private static final ResourceLocation CLOSET_CHEATER = new ResourceLocation(
        "mellow", "textures/tags/urchin/closet_cheater.png"
    );
    private static final ResourceLocation POSSIBLE_SNIPER = new ResourceLocation(
        "mellow", "textures/tags/urchin/potential_sniper.png"
    );
    private static final ResourceLocation LEGIT_SNIPER = new ResourceLocation(
        "mellow", "textures/tags/urchin/legit_sniper.png"
    );
    private static final ResourceLocation CAUTION = new ResourceLocation(
        "mellow", "textures/tags/urchin/caution.png"
    );
    private static final ResourceLocation ACCOUNT = new ResourceLocation(
        "mellow", "textures/tags/urchin/account.png"
    );
    private static final ResourceLocation INFO = new ResourceLocation(
        "mellow", "textures/tags/urchin/info.png"
    );

    private UrchinTagIconRenderer() {}

    public static int drawTags(List<UrchinTag> tags, int x, int y, int size, int gap) {
        if (tags == null || tags.isEmpty() || size <= 0) {
            return 0;
        }

        int drawX = x;
        int drawn = 0;
        for (UrchinTag tag : tags) {
            if (tag == null) {
                continue;
            }
            ResourceLocation texture = getTexture(tag.getType());
            if (texture == null) {
                continue;
            }

            drawIcon(texture, drawX, y, size);
            drawX += size + gap;
            drawn += size + gap;
        }

        return drawn > 0 ? drawn - gap : 0;
    }

    public static int measureTags(List<UrchinTag> tags, int size, int gap) {
        if (tags == null || tags.isEmpty() || size <= 0) {
            return 0;
        }

        int count = 0;
        for (UrchinTag tag : tags) {
            if (tag != null && getTexture(tag.getType()) != null) {
                count++;
            }
        }
        if (count == 0) {
            return 0;
        }
        return (count * size) + ((count - 1) * gap);
    }

    private static void drawIcon(ResourceLocation texture, int x, int y, int size) {
        if (texture == null || size <= 0) {
            return;
        }

        Minecraft mc = Minecraft.getMinecraft();
        if (mc == null || mc.getTextureManager() == null) {
            return;
        }

        mc.getTextureManager().bindTexture(texture);
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        GlStateManager.enableBlend();
        GlStateManager.enableAlpha();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        Gui.drawScaledCustomSizeModalRect(
            x,
            y,
            0.0F,
            0.0F,
            SOURCE_SIZE,
            SOURCE_SIZE,
            size,
            size,
            SOURCE_SIZE,
            SOURCE_SIZE
        );
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
    }

    private static ResourceLocation getTexture(String type) {
        if (type == null || type.trim().isEmpty()) {
            return null;
        }

        switch (type.toLowerCase()) {
            case "sniper":
                return SNIPER;
            case "blatant_cheater":
                return BLATANT_CHEATER;
            case "confirmed_cheater":
                return CONFIRMED_CHEATER;
            case "closet_cheater":
                return CLOSET_CHEATER;
            case "possible_sniper":
                return POSSIBLE_SNIPER;
            case "legit_sniper":
                return LEGIT_SNIPER;
            case "caution":
                return CAUTION;
            case "account":
                return ACCOUNT;
            case "info":
                return INFO;
            default:
                return null;
        }
    }
}

