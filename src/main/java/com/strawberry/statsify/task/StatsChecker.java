package com.strawberry.statsify.task;

import com.strawberry.statsify.api.bedwars.BedwarsPlayer;
import com.strawberry.statsify.cache.PlayerCache;
import com.strawberry.statsify.config.StatsifyOneConfig;
import com.strawberry.statsify.data.PlayerProfile;
import com.strawberry.statsify.data.TabStats;
import com.strawberry.statsify.util.ChatUtils;
import com.strawberry.statsify.util.formatting.FormattingUtils;
import com.strawberry.statsify.util.nicks.NickUtils;
import com.strawberry.statsify.util.player.PlayerUtils;
import com.strawberry.statsify.util.tags.TagUtils;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import net.minecraft.client.Minecraft;

public class StatsChecker {

    private final PlayerCache playerCache;
    private final NickUtils nickUtils;
    private final StatsifyOneConfig config;
    private final Map<String, TabStats> tabStats;
    private final TagUtils tagUtils;
    private final Minecraft mc = Minecraft.getMinecraft();

    public StatsChecker(
        PlayerCache playerCache,
        NickUtils nickUtils,
        StatsifyOneConfig config,
        Map<String, TabStats> tabStats,
        TagUtils tagUtils
    ) {
        this.playerCache = playerCache;
        this.nickUtils = nickUtils;
        this.config = config;
        this.tabStats = tabStats;
        this.tagUtils = tagUtils;
    }

    public void checkPlayerStats(List<String> onlinePlayers) {
        tabStats.clear();
        final int MAX_THREADS = 20;
        int poolSize = Math.min(onlinePlayers.size(), MAX_THREADS);
        ExecutorService executor = Executors.newFixedThreadPool(poolSize);

        for (String playerName : onlinePlayers) {
            if (nickUtils.isNicked(playerName)) continue;

            executor.submit(() -> {
                // Force a refresh by clearing the player from the cache first
                playerCache.clearPlayer(playerName);
                PlayerProfile profile = playerCache.getProfile(playerName);

                if (profile == null || profile.getBedwarsPlayer() == null) {
                    return;
                }

                BedwarsPlayer player = profile.getBedwarsPlayer();

                if (player.getFkdr() < config.minFkdr) {
                    return;
                }

                // Populate TabStats for the tab list
                if (config.tabStats) {
                    String winstreak = "";
                    if (player.getWinstreak() > 0) {
                        winstreak = FormattingUtils.formatWinstreak(
                            String.valueOf(player.getWinstreak())
                        );
                    }
                    TabStats newTabStats = new TabStats(
                        profile.getUrchinTags(),
                        player.getStars(),
                        player.getFkdrColor() + player.getFormattedFkdr(),
                        winstreak
                    );
                    tabStats.put(playerName, newTabStats);
                }

                // Print stats to chat if enabled
                if (config.printStats) {
                    String chatMessage = formatChatStats(profile);
                    if (!chatMessage.isEmpty()) {
                        mc.addScheduledTask(() ->
                            ChatUtils.sendMessage(chatMessage)
                        );
                    }
                }

                // Print Urchin tags to chat if enabled
                if (config.urchin && profile.isUrchinTagged()) {
                    String tags = FormattingUtils.formatUrchinTags(
                        profile.getUrchinTags()
                    );
                    String urchinMessage =
                        "§c" + profile.getName() + " is tagged for: " + tags;
                    mc.addScheduledTask(() ->
                        ChatUtils.sendMessage(urchinMessage)
                    );
                }
            });
        }

        executor.shutdown();
        // The notification for completion can be added back if desired
    }

    private String formatChatStats(PlayerProfile profile) {
        BedwarsPlayer player = profile.getBedwarsPlayer();
        String playerName = profile.getName();

        String displayName = PlayerUtils.getTabDisplayName(playerName);
        String stars = player.getStars();
        String fkdr = player.getFkdrColor() + player.getFormattedFkdr();

        String winstreak = "";
        if (player.getWinstreak() > 0) {
            winstreak = FormattingUtils.formatWinstreak(
                String.valueOf(player.getWinstreak())
            );
        }

        String base = String.format(
            "%s §r%s§r§7 |§r FKDR: %s",
            displayName,
            stars,
            fkdr
        );

        if (config.tags) {
            String tagsValue = buildTagsValue(profile);
            if (winstreak.isEmpty()) {
                return String.format("%s §r§7|§r [ %s ]", base, tagsValue);
            } else {
                return String.format(
                    "%s §r§7|§r WS: %s§r [ %s ]",
                    base,
                    winstreak,
                    tagsValue
                );
            }
        } else {
            if (winstreak.isEmpty()) {
                return base;
            } else {
                return String.format("%s §r§7|§r WS: %s§r", base, winstreak);
            }
        }
    }

    private String buildTagsValue(PlayerProfile profile) {
        BedwarsPlayer player = profile.getBedwarsPlayer();
        int starsInt = 0;
        try {
            starsInt = Integer.parseInt(
                player.getStars().replaceAll("§.", "").replaceAll("[^0-9]", "")
            );
        } catch (NumberFormatException ignored) {}

        String tagsValue = tagUtils.buildTags(
            profile.getName(),
            profile.getUuid(),
            starsInt,
            player.getFkdr(),
            player.getWinstreak(),
            player.getFinalKills(),
            player.getFinalDeaths()
        );

        if (tagsValue.endsWith(" ")) {
            return tagsValue.substring(0, tagsValue.length() - 1);
        }
        return tagsValue;
    }
}
