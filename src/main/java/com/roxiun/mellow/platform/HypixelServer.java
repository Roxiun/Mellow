package com.roxiun.mellow.platform;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ServerData;
import java.util.Locale;
public final class HypixelServer {
    public static boolean isHypixel() {
        ServerData server = Minecraft.getMinecraft().getCurrentServerData();
        if (server == null) return false;
        String host = server.serverIP.toLowerCase(Locale.ROOT).split(":", 2)[0];
        return host.equals("hypixel.net") || host.endsWith(".hypixel.net");
    }
}
