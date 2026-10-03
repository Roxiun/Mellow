package com.roxiun.mellow.core.event;

import com.roxiun.mellow.api.hypixel.HypixelFeatures;
import com.roxiun.mellow.config.MellowOneConfig;
import com.roxiun.mellow.feature.nicks.NumberDenicker;
import com.roxiun.mellow.feature.requestpopup.RequestPopupService;
import com.roxiun.mellow.feature.replay.ReplayManager;
import com.roxiun.mellow.feature.stats.PregameStats;
import com.roxiun.mellow.module.bedwars.BedwarsChatSignalParser;
import net.minecraft.client.Minecraft;
import com.roxiun.mellow.platform.event.ClientChatReceivedEvent;
import org.polyfrost.oneconfig.api.event.v1.invoke.impl.Subscribe;

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

    @Subscribe
    public void onChat(ClientChatReceivedEvent event) {
        numberDenicker.onChat(event);
        pregameStats.onChat(event);

        String message = event.message.getUnformattedText();
        if (requestPopupService != null) {
            requestPopupService.onChatMessage(message);
        }
        HypixelFeatures.getInstance().onChat(message);
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
