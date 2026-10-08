package com.roxiun.mellow.core.event;

import com.roxiun.mellow.api.hypixel.HypixelFeatures;
import com.roxiun.mellow.config.MellowOneConfig;
import com.roxiun.mellow.feature.nicks.NumberDenicker;
import com.roxiun.mellow.feature.requestpopup.RequestPopupService;
import com.roxiun.mellow.feature.replay.ReplayManager;
import com.roxiun.mellow.feature.stats.PregameStats;
import com.roxiun.mellow.feature.bedwars.BedwarsChatSignalParser;
import net.minecraft.client.Minecraft;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class ChatEventRouter {

    private final Minecraft mc = Minecraft.getMinecraft();
    private final MellowOneConfig config;
    private final NumberDenicker numberDenicker;
    private final PregameStats pregameStats;
    private final RequestPopupService requestPopupService;

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

    @SubscribeEvent
    public void onChat(ClientChatReceivedEvent event) {
        HypixelFeatures.getInstance().onChat(event.message.getUnformattedText());
        numberDenicker.onChat(event);
        pregameStats.onChat(event);

        String message = event.message.getUnformattedText();
        if (requestPopupService != null) {
            requestPopupService.onChatMessage(message);
        }
        ReplayManager.getInstance().onChatReceived(event.message, event.type);

        if (
            BedwarsChatSignalParser.isBedwarsStartMessage(message) ||
            BedwarsChatSignalParser.isBedwarsRespawnMessage(message)
        ) {
            if (config.autoWho) {
                mc.thePlayer.sendChatMessage("/who");
            }
        }
    }
}
