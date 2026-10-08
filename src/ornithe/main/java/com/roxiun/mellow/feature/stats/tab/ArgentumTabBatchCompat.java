package com.roxiun.mellow.feature.stats.tab;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiPlayerTabOverlay;
import net.minecraft.util.ResourceLocation;

/**
 * Argentum's tab batch stores positions, not matrices. End its batch before changing
 * the matrix for our body, draw captured hearts before popping, then resume for the
 * vanilla footer. Reflection keeps this optional integration independent of its jar
 * and tolerates Argentum installations with the HUD batching feature disabled.
 */
public final class ArgentumTabBatchCompat {
    private static final ResourceLocation ICONS = new ResourceLocation("textures/gui/icons.png");
    private static final ClassValue<Access> ACCESS = new ClassValue<>() {
        @Override protected Access computeValue(Class<?> type) {
            if (!FabricLoader.getInstance().isModLoaded("argentum")) return Access.NONE;
            try {
                Field text = type.getDeclaredField("argentum$textBatch");
                Field icons = type.getDeclaredField("argentum$iconBatch");
                text.setAccessible(true);
                icons.setAccessible(true);
                return new Access(text, icons, text.getType().getMethod("isDrawing"),
                    text.getType().getMethod("draw"), text.getType().getMethod("begin"),
                    icons.getType().getMethod("draw"));
            } catch (NoSuchFieldException disabledFeature) {
                return Access.NONE;
            } catch (ReflectiveOperationException failure) {
                throw new IllegalStateException("Unsupported Argentum tab batching API", failure);
            }
        }
    };

    private ArgentumTabBatchCompat() {}

    public static Boundary pause(GuiPlayerTabOverlay tab) {
        Access access = ACCESS.get(tab.getClass());
        if (access == Access.NONE) return Boundary.NONE;
        try {
            Object text = access.text().get(tab);
            if (text == null || !(boolean) access.isDrawing().invoke(text)) return Boundary.NONE;
            access.drawText().invoke(text); // Flush header and background in the parent matrix.
            return new Boundary(access, text, access.icons().get(tab));
        } catch (ReflectiveOperationException failure) {
            throw new IllegalStateException("Could not pause Argentum tab batch", failure);
        }
    }

    private record Access(Field text, Field icons, Method isDrawing, Method drawText,
                          Method beginText, Method drawIcons) {
        private static final Access NONE = new Access(null, null, null, null, null, null);
    }

    public static final class Boundary {
        private static final Boundary NONE = new Boundary(null, null, null);
        private final Access access;
        private final Object text;
        private final Object icons;

        private Boundary(Access access, Object text, Object icons) {
            this.access = access;
            this.text = text;
            this.icons = icons;
        }

        /** Native scoreboard hearts are also deferred by Argentum. */
        public void flushIcons() {
            if (access == null) return;
            try {
                Minecraft.getMinecraft().getTextureManager().bindTexture(ICONS);
                access.drawIcons().invoke(icons);
            } catch (ReflectiveOperationException failure) {
                throw new IllegalStateException("Could not draw Argentum tab icons", failure);
            }
        }

        public void resume() {
            if (access == null) return;
            try {
                access.beginText().invoke(text);
            } catch (ReflectiveOperationException failure) {
                throw new IllegalStateException("Could not resume Argentum tab batch", failure);
            }
        }
    }
}
