package com.roxiun.mellow.feature.stats;

import com.roxiun.mellow.config.MellowOneConfig;
import com.roxiun.mellow.util.ChatUtils;
import java.util.concurrent.atomic.AtomicBoolean;
import net.minecraft.client.Minecraft;
//? if ornithe {
import org.polyfrost.oneconfig.api.event.v1.EventManager;
import com.roxiun.mellow.platform.event.EntityJoinWorldEvent;
import org.polyfrost.oneconfig.api.event.v1.invoke.impl.Subscribe;
//?} else {
/*import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
*///?}

public final class ProviderHealthWarningService {

    private static final AtomicBoolean INITIALIZED = new AtomicBoolean(false);
    private static final ProviderHealthWarningService INSTANCE =
        new ProviderHealthWarningService();

    private static volatile MellowOneConfig config;
    private static volatile boolean hasWarnedThisLaunch = false;

    private ProviderHealthWarningService() {}

    public static void init(MellowOneConfig configInstance) {
        config = configInstance;
        if (!INITIALIZED.compareAndSet(false, true)) {
            return;
        }
        //? if ornithe {
        EventManager.INSTANCE.register(INSTANCE);
        //?} else {
        /*MinecraftForge.EVENT_BUS.register(INSTANCE);
        *///?}
    }

    //? if ornithe {
    @Subscribe
    //?} else {
    /*@SubscribeEvent
    *///?}
    public void onEntityJoinWorld(EntityJoinWorldEvent event) {
        if (hasWarnedThisLaunch) {
            return;
        }

        Minecraft mc = Minecraft.getMinecraft();
        if (mc == null || mc.thePlayer == null || event.entity != mc.thePlayer) {
            return;
        }

        MellowOneConfig currentConfig = config;
        if (currentConfig == null) {
            hasWarnedThisLaunch = true;
            return;
        }

        String warningMessage = getWarningMessage(
            currentConfig.statsProvider,
            currentConfig.hypixelApiKey
        );
        if (warningMessage != null) {
            ChatUtils.sendMessage(warningMessage);
            hasWarnedThisLaunch = true;
        }
    }

    static String getWarningMessage(int statsProvider, String hypixelApiKey) {
        if (statsProvider == 1 || statsProvider == 2) {
            String providerName = statsProvider == 1 ? "Nadeshiko" : "Abyss";
            return (
                "§e" +
                providerName +
                " is deprecated. Switch your Stats Provider to §bBordic§e for keyless stats, or use §bHypixel Public API§e and add a key in §bAPI Keys > Hypixel§e."
            );
        }

        if (
            statsProvider == 0 &&
            (hypixelApiKey == null || hypixelApiKey.trim().isEmpty())
        ) {
            return "§eHypixel Public API is selected but no API key is set. Switch to §bBordic§e for keyless stats, or add a key in §bAPI Keys > Hypixel§e.";
        }

        return null;
    }
}
