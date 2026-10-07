package com.roxiun.mellow.feature.stats.tab;

import com.roxiun.mellow.Mellow;
import com.roxiun.mellow.mixin.PlayerTabOverlayAccessor;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.scoreboard.ScoreObjective;
import net.minecraft.util.IChatComponent;

/** Optional dependency access stays in Holder, which is never loaded without VanillaHUD. */
public final class VanillaHudTabIntegration {
    private static final boolean PRESENT = FabricLoader.getInstance().isModLoaded("vanillahud");
    private VanillaHudTabIntegration() {}

    public static boolean active() {
        return Mellow.tabOverlayRouter != null && Mellow.tabOverlayRouter.isExtendedModeActive()
            && (!PRESENT || !Holder.hud().getPreviewing());
    }
    public static boolean usesToggle() { return PRESENT && Holder.hud().getDisplayMode() != 0; }
    public static boolean isRendering() { return PRESENT && Holder.hud().isRendering(); }
    public static boolean showHeads() { return !PRESENT || Holder.hud().getShowHead(); }
    public static boolean selfAtTop() { return PRESENT && Holder.hud().getSelfAtTop(); }
    public static int playerLimit(int fallback) { return PRESENT ? Math.max(1, Holder.hud().getPlayerLimit()) : fallback; }
    public static IChatComponent header(IChatComponent text) { return PRESENT && !Holder.hud().getShowHeader() ? null : text; }
    public static IChatComponent footer(IChatComponent text) { return PRESENT && !Holder.hud().getShowFooter() ? null : text; }

    public static ExtendedStatsTabOverlay.Layout measure(int width, IChatComponent header,
                                                         IChatComponent footer, ScoreObjective objective) {
        if (!active()) return null;
        Minecraft mc = Minecraft.getMinecraft();
        ExtendedStatsTabOverlay overlay = Mellow.tabOverlayRouter.prepareOverlay();
        if (overlay == null || mc.getNetHandler() == null) return null;
        return overlay.measure(ExtendedTabStatsMode.resolveScope(), width,
            new ScaledResolution(mc).getScaledHeight(), header(header), footer(footer), objective);
    }

    public static ExtendedStatsTabOverlay.Layout measureCurrent() {
        if (!active()) return null;
        Minecraft mc = Minecraft.getMinecraft();
        PlayerTabOverlayAccessor tab = (PlayerTabOverlayAccessor) mc.ingameGUI.getTabList();
        return measure(new ScaledResolution(mc).getScaledWidth(), tab.mellow$getHeader(),
            tab.mellow$getFooter(), mc.theWorld.getScoreboard().getObjectiveInDisplaySlot(0));
    }

    private static final class Holder {
        static org.polyfrost.vanillahud.hud.TabListHud hud() {
            return org.polyfrost.vanillahud.hud.Huds.INSTANCE.getTabList();
        }
    }
}
