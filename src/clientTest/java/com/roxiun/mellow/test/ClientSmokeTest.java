package com.roxiun.mellow.test;

import com.roxiun.mellow.Mellow;
import com.roxiun.mellow.feature.replay.*;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.network.Packet;
import net.minecraft.network.play.server.*;
import net.minecraft.util.ChatComponentText;
import org.polyfrost.oneconfig.api.event.v1.EventManager;


/** Runs inside the real Fabric/Mixin classloader; never included in the release artifact. */
public final class ClientSmokeTest implements ClientModInitializer {
    public void onInitializeClient() {
        // Run after the game's initialization event and all mod initialization listeners finish.
        final int[] ticks = {0};
        EventManager.register(org.polyfrost.oneconfig.api.event.v1.events.TickEvent.End.class, () -> {
            ++ticks[0];
            if (Boolean.getBoolean("mellow.configPreview")) {
                if (ticks[0] == 60) org.polyfrost.oneconfig.api.ui.v1.OneConfigUI.open(
                    new org.polyfrost.oneconfig.internal.ui.navigation.graph.ModConfigRoute("mellow-v1.json", System.getProperty("mellow.configPreviewCategory", "Tab Stats")));
                if (ticks[0] == 140) {
                    var mc = Minecraft.getMinecraft();
                    net.minecraft.util.ScreenShotHelper.saveScreenshot(mc.mcDataDir, "mellow-config.png", mc.displayWidth, mc.displayHeight, mc.getFramebuffer());
                    verify();
                }
            } else if (ticks[0] == 5) verify();
        });
    }
    private void verify() {
        try {
            if (Mellow.config == null) throw new AssertionError("Mellow not initialized");
            Object providerDefault = Mellow.config.getTree().getProp("statsProvider").getMetadata("default");
            if (!Integer.valueOf(3).equals(providerDefault)) throw new AssertionError("Bordic is not the reset default");
            var orderProperty = Mellow.config.getTree().getProp("bedwarsStatOrder");
            if (orderProperty == null || !Boolean.TRUE.equals(orderProperty.getMetadata("checkable")))
                throw new AssertionError("Native stat-order control was not registered");
            String[] savedOrder = Mellow.config.bedwarsStatOrder;
            try {
                orderProperty.setAs(new String[]{"Ping", "Name"});
                int[] columns = com.roxiun.mellow.feature.stats.tab.ExtendedTabStatsColumns.getConfiguredStatsForScope(
                    com.roxiun.mellow.api.provider.model.StatScope.BEDWARS, Mellow.config);
                if (!java.util.Arrays.equals(columns, new int[]{13, 2})) throw new AssertionError("UI reorder did not update renderer");
                orderProperty.setAs(new String[0]);
                if (Mellow.config.bedwarsStatOrder.length != 0) throw new AssertionError("Cannot disable all stats");
            } finally { orderProperty.setAs(savedOrder); }
            String[] classes = {
                "net.minecraft.client.network.NetHandlerPlayClient",
                "net.minecraft.network.NetworkManager",
                "net.minecraft.network.play.server.S02PacketChat",
                "net.minecraft.client.gui.GuiChat"
            };
            for (String name : classes) Class.forName(name);
            if (FabricLoader.getInstance().isModLoaded("polynametag"))
                verifyNametags();
            Packet<?> source = new S02PacketChat(new ChatComponentText("Mellow replay smoke test"));
            ReplayPacketFrame frame = ReplayPacketCodec.encode(42, source);
            if (!frame.getClassName().equals("net.minecraft.network.play.server.S02PacketChat"))
                throw new AssertionError("Runtime names leaked into replay format");
            S02PacketChat decoded = (S02PacketChat) ReplayPacketCodec.decode(frame);
            if (!decoded.getChatComponent().getUnformattedText().equals("Mellow replay smoke test"))
                throw new AssertionError("Replay roundtrip changed message");
            // Relative moves are nested packet classes and must also roundtrip across mappings.
            for (Class<?> type : S14PacketEntity.class.getDeclaredClasses()) {
                if (Packet.class.isAssignableFrom(type)) {
                    var ctor = type.getDeclaredConstructor();
                    ctor.setAccessible(true);
                    ReplayPacketFrame move = ReplayPacketCodec.encode(43, (Packet<?>) ctor.newInstance());
                    if (ReplayPacketCodec.decode(move).getClass() != type) throw new AssertionError("Nested packet mismatch");
                }
            }
            if (!com.roxiun.mellow.platform.ClientCommands.complete("/mel").contains("/mellow"))
                throw new AssertionError("Client command was not registered");
            try {
                ReplayPacketCodec.decode(new ReplayPacketFrame(0, "java.lang.String", new byte[0]));
                throw new AssertionError("Unknown packet accepted");
            } catch (IllegalArgumentException expected) { }
            java.nio.file.Files.writeString(java.nio.file.Path.of(System.getProperty("mellow.smokeResult")), "PASS");
            org.apache.logging.log4j.LogManager.getLogger("MellowTests").info("MELLOW CLIENT SMOKE PASS");
        } catch (Throwable failure) {
            org.apache.logging.log4j.LogManager.getLogger("MellowTests").error("MELLOW CLIENT SMOKE FAIL", failure);
        } finally {
            Minecraft.getMinecraft().shutdown();
        }
    }

    private void verifyNametags() {
        var config = Mellow.config;
        boolean backgrounds = config.coloredNametagBackgrounds;
        boolean compatibility = config.coloredNametagAffectPolyNametag;
        try {
            config.coloredNametagBackgrounds = true;
            config.coloredNametagAffectPolyNametag = false;
            int original = org.polyfrost.polynametag.client.NametagRenderer.backgroundColor(0x40000000);
            com.roxiun.mellow.util.nametag.NametagRenderContext.setState(
                new com.roxiun.mellow.util.RgbaColor(18, 52, 86, 255),
                com.roxiun.mellow.api.seraph.SeraphClientType.LUNAR, true, "MellowTest");
            com.roxiun.mellow.util.nametag.NametagRenderContext.beginLabel("MellowTest");
            config.coloredNametagAffectPolyNametag = true;
            int changed = org.polyfrost.polynametag.client.NametagRenderer.backgroundColor(0x40000000);
            if (changed != ((original & 0xff000000) | 0x123456))
                throw new AssertionError("PolyNametag lost team color or original opacity");
            org.polyfrost.polynametag.client.NametagRenderer.backgroundQuads(-20f, 0f, 40f);
            float[] withIcon = org.polyfrost.polynametag.client.NametagRenderer.backgroundQuadBuffer().clone();
            com.roxiun.mellow.util.nametag.NametagRenderContext.clear();
            org.polyfrost.polynametag.client.NametagRenderer.backgroundQuads(-20f, 0f, 40f);
            float[] withoutIcon = org.polyfrost.polynametag.client.NametagRenderer.backgroundQuadBuffer();
            if (withIcon[0] != withoutIcon[0] - 5f || withIcon[2] != withoutIcon[2] + 5f)
                throw new AssertionError("PolyNametag background did not reserve client icon space");
            if (org.polyfrost.polynametag.client.NametagRenderer.backgroundColor(0x40000000) != original)
                throw new AssertionError("Nametag state leaked to unrelated labels");
        } finally {
            com.roxiun.mellow.util.nametag.NametagRenderContext.clear();
            config.coloredNametagBackgrounds = backgrounds;
            config.coloredNametagAffectPolyNametag = compatibility;
        }
    }
}
