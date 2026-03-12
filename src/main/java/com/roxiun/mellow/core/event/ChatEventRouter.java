package com.roxiun.mellow.core.event;

import com.roxiun.mellow.Mellow;
import com.roxiun.mellow.api.hypixel.HypixelFeatures;
import com.roxiun.mellow.config.MellowOneConfig;
import com.roxiun.mellow.feature.nicks.NumberDenicker;
import com.roxiun.mellow.feature.requestpopup.RequestPopupService;
import com.roxiun.mellow.feature.replay.ReplayManager;
import com.roxiun.mellow.feature.stats.PregameStats;
import com.roxiun.mellow.module.bedwars.BedwarsChatSignalParser;
import net.minecraft.client.Minecraft;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class ChatEventRouter {

    private final Minecraft mc = Minecraft.getMinecraft();
    private final MellowOneConfig config;
    private final NumberDenicker numberDenicker;
    private final PregameStats pregameStats;
    private final RequestPopupService requestPopupService;

    private boolean awaitingAutoWhoResponse;

    public ChatEventRouter(
        MellowOneConfig config,
        NumberDenicker numberDenicker,
        PregameStats pregameStats,
        RequestPopupService requestPopupService
    ) {
        this.config = config;
        this.numberDenicker = numberDenicker;
        this.pregameStats = pregameStats;
        this.requestPopupService = requestPopupService;
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public void onChat(ClientChatReceivedEvent event) {
        String message = event.message.getUnformattedText();

        // Game state tracking and replay always run regardless of mod toggle
        HypixelFeatures.getInstance().onChat(message);
        ReplayManager.getInstance().onChatReceived(event.message, event.type);

        if (!Mellow.isEnabled()) {
            return;
        }

        // Hide the ONLINE: response from auto /who
        if (awaitingAutoWhoResponse && message.startsWith("ONLINE: ")) {
            awaitingAutoWhoResponse = false;
            if (config.hideAutoWhoResponse) {
                event.setCanceled(true);
                return;
            }
        }

        numberDenicker.onChat(event);
        pregameStats.onChat(event);

        if (requestPopupService != null) {
            requestPopupService.onChatMessage(message);
        }

        if (
            BedwarsChatSignalParser.isBedwarsStartMessage(message) ||
            BedwarsChatSignalParser.isBedwarsRespawnMessage(message)
        ) {
            if (config.autoWho) {
                awaitingAutoWhoResponse = true;
                mc.thePlayer.sendChatMessage("/who");
            }
        }
    }
}
