package com.roxiun.mellow.platform;
import net.fabricmc.loader.api.FabricLoader;
public final class PolyNametagIntegration {
    public static float textY(int y) {
        return FabricLoader.getInstance().isModLoaded("polynametag")
            ? org.polyfrost.polynametag.client.NametagRenderer.translateY(y) : y;
    }
}
