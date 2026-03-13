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

    private static final String AUTO_WHO_RESPONSE_PREFIX = "ONLINE: ";
    private static final int AUTO_WHO_RESPONSE_PREFIX_LENGTH =
        AUTO_WHO_RESPONSE_PREFIX.length();
    // Few chat lines immediately follow the server-side /who response.
    private static final int AUTO_WHO_RESPONSE_WATCH_LIMIT = 6;

    private final Minecraft mc = Minecraft.getMinecraft();
    private final MellowOneConfig config;
    private final NumberDenicker numberDenicker;
    private final PregameStats pregameStats;
    private final RequestPopupService requestPopupService;

    private int autoWhoResponseWatchRemaining;

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
            autoWhoResponseWatchRemaining = 0;
            return;
        }

        boolean autoWhoEnabled = config.autoWho;
        boolean hideAutoWhoResponse =
            autoWhoEnabled && config.hideAutoWhoResponse;

        if (!hideAutoWhoResponse) {
            autoWhoResponseWatchRemaining = 0;
        } else if (autoWhoResponseWatchRemaining > 0) {
            if (
                message.length() >= AUTO_WHO_RESPONSE_PREFIX_LENGTH &&
                message.regionMatches(
                    0,
                    AUTO_WHO_RESPONSE_PREFIX,
                    0,
                    AUTO_WHO_RESPONSE_PREFIX_LENGTH
                )
            ) {
                autoWhoResponseWatchRemaining = 0;
                event.setCanceled(true);
                return;
            }
            autoWhoResponseWatchRemaining--;
        }

        numberDenicker.onChat(event);
        pregameStats.onChat(event);

        if (requestPopupService != null) {
            requestPopupService.onChatMessage(message);
        }

        if (
            autoWhoEnabled &&
            (BedwarsChatSignalParser.isBedwarsStartMessage(message) ||
                BedwarsChatSignalParser.isBedwarsRespawnMessage(message))
        ) {
            autoWhoResponseWatchRemaining =
                hideAutoWhoResponse ? AUTO_WHO_RESPONSE_WATCH_LIMIT : 0;
            mc.thePlayer.sendChatMessage("/who");
        }
    }
}
