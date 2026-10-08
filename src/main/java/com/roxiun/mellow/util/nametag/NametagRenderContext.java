package com.roxiun.mellow.util.nametag;
import cc.polyfrost.oneconfig.config.core.OneColor;
public final class NametagRenderContext {
    private static final ThreadLocal<OneColor> COLOR = new ThreadLocal<>();
    private NametagRenderContext() {}
    public static void setColor(OneColor color) { COLOR.set(color); }
    public static OneColor getColor() { return COLOR.get(); }
    public static boolean isActive() { return COLOR.get() != null; }
    public static void clear() { COLOR.remove(); }
}
