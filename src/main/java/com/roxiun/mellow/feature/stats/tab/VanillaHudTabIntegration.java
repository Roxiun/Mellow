package com.roxiun.mellow.feature.stats.tab;

import com.roxiun.mellow.Mellow;
import com.roxiun.mellow.mixin.PlayerTabOverlayAccessor;
//? if ornithe {
import net.fabricmc.loader.api.FabricLoader;
//?}
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.scoreboard.ScoreObjective;
import net.minecraft.util.IChatComponent;
//? if forge {
/*import net.minecraftforge.fml.common.Loader;
*///?}

/** Optional dependency access is isolated so VanillaHUD remains optional. */
public final class VanillaHudTabIntegration {
    //? if ornithe {
    private static final boolean PRESENT = FabricLoader.getInstance().isModLoaded("vanillahud");
    //?}
    private VanillaHudTabIntegration() {}
    //? if forge {
    /*private static boolean present() { return Loader.isModLoaded("vanillahud"); }
    *///?}
    public static boolean active() {
        return Mellow.tabOverlayRouter != null && Mellow.tabOverlayRouter.isExtendedModeActive()
            //? if ornithe {
            && (!PRESENT || !Holder.hud().getPreviewing());
            //?} else {
            /*&& (!present() || (!Holder.editing() && !Holder.compact()));
            *///?}
    }
    //? if ornithe {
    public static boolean usesToggle() { return PRESENT && Holder.hud().getDisplayMode() != 0; }
    public static boolean isRendering() { return PRESENT && Holder.hud().isRendering(); }
    public static boolean showHeads() { return !PRESENT || Holder.hud().getShowHead(); }
    public static boolean selfAtTop() { return PRESENT && Holder.hud().getSelfAtTop(); }
    public static int playerLimit(int fallback) { return PRESENT ? Math.max(1, Holder.hud().getPlayerLimit()) : fallback; }
    public static IChatComponent header(IChatComponent text) { return PRESENT && !Holder.hud().getShowHeader() ? null : text; }
    public static IChatComponent footer(IChatComponent text) { return PRESENT && !Holder.hud().getShowFooter() ? null : text; }

    //?} else {
    /*public static boolean usesToggle() { return present() && Holder.toggle(); }
    public static boolean isRendering() { return present() && Holder.rendering(); }
    public static boolean measuring() { return present() && Holder.measuring(); }
    public static boolean showHeads() { return !present() || Holder.heads(); }
    public static boolean selfAtTop() { return present() && Holder.self(); }
    public static int playerLimit(int fallback) { return present() ? Holder.limit() : fallback; }
    public static IChatComponent header(IChatComponent text) { return present() && !Holder.header() ? null : text; }
    public static IChatComponent footer(IChatComponent text) { return present() && !Holder.footer() ? null : text; }
    *///?}
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
        //? if ornithe {
        static org.polyfrost.vanillahud.hud.TabListHud hud() {
            return org.polyfrost.vanillahud.hud.Huds.INSTANCE.getTabList();
        }
        //?} else {
        /*private static final java.lang.reflect.Field EDITING = editingField();
        private static java.lang.reflect.Field editingField() {
            try { return Class.forName("cc.polyfrost.oneconfig.internal.hud.HudCore").getField("editing"); }
            catch (ReflectiveOperationException e) { throw new IllegalStateException("Cannot read OneConfig HUD editor state", e); }
        }
        static boolean editing() {
            try { return EDITING.getBoolean(null); }
            catch (IllegalAccessException e) { throw new IllegalStateException(e); }
        }
        static boolean compact() {
            return org.polyfrost.vanillahud.VanillaHUD.isForceDisableCompactTab()
                || org.polyfrost.vanillahud.VanillaHUD.isLegacyTablist();
        }
        static boolean toggle() { return org.polyfrost.vanillahud.config.ModConfig.tab.enabled && org.polyfrost.vanillahud.hud.TabList.TabHud.displayMode; }
        static boolean rendering() { return org.polyfrost.vanillahud.hud.TabList.animation.get() > 0 && org.polyfrost.vanillahud.hud.TabList.hud.shouldRender(); }
        static boolean measuring() { return org.polyfrost.vanillahud.hooks.TabHook.gettingSize; }
        static boolean heads() { return org.polyfrost.vanillahud.hud.TabList.TabHud.showHead; }
        static boolean self() { return org.polyfrost.vanillahud.hud.TabList.TabHud.selfAtTop; }
        static int limit() { return org.polyfrost.vanillahud.hud.TabList.TabHud.getTabPlayerLimit(); }
        static boolean header() { return org.polyfrost.vanillahud.hud.TabList.TabHud.showHeader; }
        static boolean footer() { return org.polyfrost.vanillahud.hud.TabList.TabHud.showFooter; }
        *///?}
    }
}
