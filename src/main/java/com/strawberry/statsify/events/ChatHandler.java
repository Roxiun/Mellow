package com.strawberry.statsify.events;

import com.strawberry.statsify.api.PlanckeApi;
import com.strawberry.statsify.config.StatsifyOneConfig;
import com.strawberry.statsify.task.StatsChecker;
import com.strawberry.statsify.util.NickUtils;
import com.strawberry.statsify.util.NumberDenicker;
import com.strawberry.statsify.util.PregameStats;
import com.strawberry.statsify.util.Utils;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.util.ChatComponentText;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class ChatHandler {

    private final Minecraft mc = Minecraft.getMinecraft();
    private final StatsifyOneConfig config;
    private final NickUtils nickUtils;
    private final NumberDenicker numberDenicker;
    private final PregameStats pregameStats;
    private final PlanckeApi planckeApi;
    private final StatsChecker statsChecker;

    public ChatHandler(
        StatsifyOneConfig config,
        NickUtils nickUtils,
        NumberDenicker numberDenicker,
        PregameStats pregameStats,
        PlanckeApi planckeApi,
        StatsChecker statsChecker
    ) {
        this.config = config;
        this.nickUtils = nickUtils;
        this.numberDenicker = numberDenicker;
        this.pregameStats = pregameStats;
        this.planckeApi = planckeApi;
        this.statsChecker = statsChecker;
    }

    @SubscribeEvent
    public void onChat(ClientChatReceivedEvent event) {
        numberDenicker.onChat(event);
        pregameStats.onChat(event);
        String message = event.message.getUnformattedText();
        if (config.autoWho) {
            if (
                message.contains(
                    "Protect your bed and destroy the enemy beds."
                ) &&
                !(message.contains(":")) &&
                !(message.contains("SHOUT"))
            ) {
                mc.thePlayer.sendChatMessage("/who");
            }
        }
        if (message.startsWith("ONLINE:")) {
            String playersString = message.substring("ONLINE:".length()).trim();
            String[] players = playersString.split(",\\s*");
            List<String> onlinePlayers = new ArrayList<>(
                Arrays.asList(players)
            );
            nickUtils.updateNickedPlayers(onlinePlayers);
            statsChecker.checkStatsRatelimitless(onlinePlayers);
            if (config.urchin) {
                statsChecker.checkUrchinTags(onlinePlayers);
            }
        }

        if (message.startsWith(" ") && message.contains("Opponent:")) {
            String username = Utils.parseUsername(message);
            new Thread(() -> {
                try {
                    String stats = planckeApi.checkDuels(username);
                    mc.thePlayer.addChatMessage(
                        new ChatComponentText("§r[§bF§r] " + stats)
                    );
                } catch (IOException e) {
                    mc.thePlayer.addChatMessage(
                        new ChatComponentText(
                            "§r[§bF§r] §cFailed to get stats for " + username
                        )
                    );
                }
            })
                .start();
        }
    }
}
