package com.strawberry.statsify.task;

import com.strawberry.statsify.api.HypixelApi;
import com.strawberry.statsify.api.UrchinApi;
import com.strawberry.statsify.config.StatsifyOneConfig;
import com.strawberry.statsify.util.NickUtils;
import com.strawberry.statsify.util.Utils;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import net.minecraft.client.Minecraft;
import net.minecraft.util.ChatComponentText;

public class StatsChecker {

    private final HypixelApi hypixelApi;
    private final UrchinApi urchinApi;
    private final NickUtils nickUtils;
    private final StatsifyOneConfig config;
    private final Map<String, List<String>> playerSuffixes;
    private final Minecraft mc = Minecraft.getMinecraft();

    public StatsChecker(
        HypixelApi hypixelApi,
        UrchinApi urchinApi,
        NickUtils nickUtils,
        StatsifyOneConfig config,
        Map<String, List<String>> playerSuffixes
    ) {
        this.hypixelApi = hypixelApi;
        this.urchinApi = urchinApi;
        this.nickUtils = nickUtils;
        this.config = config;
        this.playerSuffixes = playerSuffixes;
    }

    public void checkUrchinTags(List<String> onlinePlayers) {
        ExecutorService executor = Executors.newFixedThreadPool(5);
        for (String playerName : onlinePlayers) {
            if (nickUtils.isNicked(playerName)) continue;
            executor.submit(() -> {
                try {
                    String tags = urchinApi
                        .fetchUrchinTags(playerName, config.urchinKey)
                        .replace("sniper", "§4§lSniper")
                        .replace("blatant_cheater", "§4§lBlatant Cheater")
                        .replace("closet_cheater", "§e§lCloset Cheater")
                        .replace(
                            "confirmed_cheater",
                            "§4§lConfirmed Cheater"
                        );
                    if (!tags.isEmpty()) {
                        mc.addScheduledTask(() ->
                            mc.thePlayer.addChatMessage(
                                new ChatComponentText(
                                    "§r[§bF§r] §c\u26a0 §r" +
                                    Utils.getTabDisplayName(playerName) +
                                    " §ris §ctagged§r for: " +
                                    tags
                                )
                            )
                        );
                    }
                } catch (IOException e) {
                    mc.addScheduledTask(() ->
                        mc.thePlayer.addChatMessage(
                            new ChatComponentText(
                                "§r[§bF§r] Failed to fetch tags for: " +
                                playerName +
                                " | " +
                                e.getMessage()
                            )
                        )
                    );
                }
            });
        }
        executor.shutdown();
    }

    public void checkStatsRatelimitless(List<String> onlinePlayers) {
        final int MAX_THREADS = 20;
        int poolSize = Math.min(onlinePlayers.size(), MAX_THREADS);
        ExecutorService executor = Executors.newFixedThreadPool(poolSize);

        for (String playerName : onlinePlayers) {
            if (nickUtils.isNicked(playerName)) continue;
            executor.submit(() -> {
                try {
                    String stats = hypixelApi.fetchBedwarsStats(
                        playerName,
                        config.minFkdr,
                        config.tags,
                        config.tabStats,
                        playerSuffixes
                    );
                    if (!stats.isEmpty() && config.printStats) {
                        mc.addScheduledTask(() ->
                            mc.thePlayer.addChatMessage(
                                new ChatComponentText("§r[§bF§r] " + stats)
                            )
                        );
                    }
                } catch (IOException e) {
                    mc.addScheduledTask(() ->
                        mc.thePlayer.addChatMessage(
                            new ChatComponentText(
                                "§r[§bF§r] Failed to fetch stats for: " +
                                playerName +
                                " | [UpstreamCSR] "
                            )
                        )
                    );
                }
            });
        }

        executor.shutdown();

        new Thread(() -> {
            try {
                if (executor.awaitTermination(60, TimeUnit.SECONDS)) {
                    mc.addScheduledTask(() ->
                        mc.thePlayer.addChatMessage(
                            new ChatComponentText(
                                "§r[§bF§r]§a Checks completed."
                            )
                        )
                    );
                } else {
                    mc.addScheduledTask(() ->
                        mc.thePlayer.addChatMessage(
                            new ChatComponentText(
                                "§r[§bF§r]§c Timeout waiting for completion."
                            )
                        )
                    );
                }
            } catch (InterruptedException e) {
                mc.addScheduledTask(() ->
                    mc.thePlayer.addChatMessage(
                        new ChatComponentText(
                            "§r[§bF§r] §cError while waiting: " +
                            e.getMessage()
                        )
                    )
                );
            }
        })
            .start();
    }
}
