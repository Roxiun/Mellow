package com.roxiun.mellow.platform;

import java.util.Locale;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.network.NetHandlerPlayClient;

public final class HypixelServer {
    private static NetHandlerPlayClient confirmedConnection;

    private HypixelServer() {}

    /** Location packets identify the connection even when a proxy changes its address or brand. */
    public static void confirmConnection() {
        confirmedConnection = Minecraft.getMinecraft().getNetHandler();
    }

    public static boolean isHypixel() {
        Minecraft mc = Minecraft.getMinecraft();
        NetHandlerPlayClient connection = mc.getNetHandler();
        if (connection == null || mc.isSingleplayer()) {
            confirmedConnection = null;
            return false;
        }
        if (connection == confirmedConnection) return true;
        // A new connection must establish its own identity; world changes keep the same connection.
        confirmedConnection = null;
        String brand = mc.thePlayer == null ? null : mc.thePlayer.getClientBrand();
        ServerData server = mc.getCurrentServerData();
        return matchesIdentity(server == null ? null : server.serverIP, brand);
    }

    static boolean matchesIdentity(String address, String brand) {
        if (brand != null && brand.toLowerCase(Locale.ROOT).contains("hypixel")) return true;
        if (address == null) return false;
        String host = address.toLowerCase(Locale.ROOT).split(":", 2)[0];
        return host.equals("hypixel.net") || host.endsWith(".hypixel.net");
    }
}
