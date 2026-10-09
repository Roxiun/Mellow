package com.roxiun.mellow.util.nametag;
//? if ornithe {
import com.roxiun.mellow.util.RgbaColor;
//?} else {
/*import cc.polyfrost.oneconfig.config.core.OneColor;
*///?}
public final class NametagRenderContext {
    //? if ornithe {
    private static final ThreadLocal<RgbaColor> COLOR = new ThreadLocal<>();
    //?} else {
    /*private static final ThreadLocal<OneColor> COLOR = new ThreadLocal<>();
    *///?}
    private NametagRenderContext() {}
    //? if ornithe {
    public static void setColor(RgbaColor color) { COLOR.set(color); }
    public static RgbaColor getColor() { return COLOR.get(); }
    //?} else {
    /*public static void setColor(OneColor color) { COLOR.set(color); }
    public static OneColor getColor() { return COLOR.get(); }
    *///?}
    public static boolean isActive() { return COLOR.get() != null; }
    public static void clear() { COLOR.remove(); }
}
