package com.roxiun.mellow.test;

import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraft.client.Minecraft;

/** Test-only mod, excluded from release jars. */
@Mod(modid = "mellow_client_tests", name = "Mellow client tests", version = "1", dependencies = "after:mellow")
public final class ForgeClientSmokeTest {
    private int ticks;
    @Mod.EventHandler public void init(FMLInitializationEvent e) { MinecraftForge.EVENT_BUS.register(this); }
    @SubscribeEvent public void tick(TickEvent.ClientTickEvent e) {
        if (e.phase != TickEvent.Phase.END || ++ticks != 10) return;
        try {
            if (Boolean.getBoolean("mellow.compatTest") && !net.minecraftforge.fml.common.Loader.isModLoaded("vanillahud"))
                throw new AssertionError("VanillaHUD was not fully initialized");
            TabIntegrationSmokeTest.verify();
            java.nio.file.Files.write(java.nio.file.Paths.get(System.getProperty("mellow.smokeResult")), "PASS".getBytes(java.nio.charset.StandardCharsets.UTF_8));
            System.out.println("MELLOW CLIENT SMOKE PASS");
        } catch (Throwable failure) { failure.printStackTrace(); }
        finally { Minecraft.getMinecraft().shutdown(); }
    }
}
