package com.roxiun.mellow.util.render;

import com.roxiun.mellow.api.seraph.SeraphTag;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.ResourceLocation;

public final class SeraphTagIconRenderer {

    private static final int SOURCE_SIZE = 56;
    private static final ResourceLocation SNIPER = new ResourceLocation(
        "mellow", "textures/tags/seraph/sniper.png"
    );
    private static final ResourceLocation BLATANT_CHEATING = new ResourceLocation(
        "mellow", "textures/tags/seraph/blatant_cheating.png"
    );
    private static final ResourceLocation LEGIT_SNIPER = new ResourceLocation(
        "mellow", "textures/tags/seraph/legit_sniper.png"
    );
    private static final ResourceLocation BOT = new ResourceLocation(
        "mellow", "textures/tags/seraph/bot.png"
    );
    private static final ResourceLocation ALT = new ResourceLocation(
        "mellow", "textures/tags/seraph/alt.png"
    );
    private static final ResourceLocation ANNOYING = new ResourceLocation(
        "mellow", "textures/tags/seraph/annoying.png"
    );
    private static final ResourceLocation CAUTION = new ResourceLocation(
        "mellow", "textures/tags/seraph/caution.png"
    );
    private static final ResourceLocation CLOSET_CHEATING = new ResourceLocation(
        "mellow", "textures/tags/seraph/closet_cheating.png"
    );
    private static final ResourceLocation POTENTIAL_SNIPER = new ResourceLocation(
        "mellow", "textures/tags/seraph/potential_sniper.png"
    );

    private SeraphTagIconRenderer() {}

    public static int drawTags(List<SeraphTag> tags, int x, int y, int size, int gap) {
        if (tags == null || tags.isEmpty() || size <= 0) {
            return 0;
        }

        int drawX = x;
        int drawn = 0;
        for (SeraphTag tag : tags) {
            if (tag == null) {
                continue;
            }
            ResourceLocation texture = getTexture(tag.getTagName());
            if (texture == null) {
                continue;
            }

            drawIcon(texture, drawX, y, size);
            drawX += size + gap;
            drawn += size + gap;
        }

        return drawn > 0 ? drawn - gap : 0;
    }

    public static int measureTags(List<SeraphTag> tags, int size, int gap) {
        if (tags == null || tags.isEmpty() || size <= 0) {
            return 0;
        }

        int count = 0;
        for (SeraphTag tag : tags) {
            if (tag != null && getTexture(tag.getTagName()) != null) {
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

    private static ResourceLocation getTexture(String tagName) {
        if (tagName == null || tagName.trim().isEmpty()) {
            return null;
        }

        switch (tagName.toLowerCase()) {
            case "seraph.sniping":
                return SNIPER;
            case "seraph.blatant_cheating":
                return BLATANT_CHEATING;
            case "seraph.legit_sniping":
                return LEGIT_SNIPER;
            case "seraph.bot":
                return BOT;
            case "seraph.alt":
                return ALT;
            case "seraph.annoylist":
                return ANNOYING;
            case "seraph.caution":
                return CAUTION;
            case "seraph.closet_cheating":
                return CLOSET_CHEATING;
            case "seraph.potential_sniper":
                return POTENTIAL_SNIPER;
            // Tags without textures: potential_sniper, safelist.*, encounters, cookie, verified
            default:
                return null;
        }
    }
}

