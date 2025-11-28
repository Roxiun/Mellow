package com.roxiun.mellow.api.bedwars;

import com.roxiun.mellow.Mellow;
import com.roxiun.mellow.util.formatting.FormattingUtils;
import com.roxiun.mellow.util.player.PlayerUtils;
import com.roxiun.mellow.util.tags.TagUtils;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.minecraft.util.EnumChatFormatting;

public class HypixelApi {

    private final Mellow mellow;
    private final TagUtils tagUtils;

    public HypixelApi(Mellow mellow, TagUtils tagUtils) {
        this.mellow = mellow;
        this.tagUtils = tagUtils;
    }

    public String fetchBedwarsStats(
        String playerName,
        int minFkdr,
        boolean tags,
        boolean tabstats,
        Map<String, List<String>> playerSuffixes
    ) {
        try {
            BedwarsPlayer player = mellow
                .getStatsProvider()
                .fetchPlayerStats(playerName);

            if (player == null) {
                return (
                    "§cFailed to get stats for " +
                    PlayerUtils.getTabDisplayName(playerName)
                );
            }

            if (player.getFkdr() < minFkdr) {
                return "";
            }

            if (tabstats) {
                populateTabStats(playerName, player, playerSuffixes);
            }

            return formatPlayerStats(playerName, player, tags);
        } catch (Exception e) {
            // It's good practice to log the exception.
            // e.g., mellow.getLogger().error("Failed to fetch/process stats for " + playerName, e);
            return (
                EnumChatFormatting.RED + "Failed to get stats for " + playerName
            );
        }
    }

    /**
     * Populates the playerSuffixes map with stats for the tab list.
     */
    private void populateTabStats(
        String playerName,
        BedwarsPlayer player,
        Map<String, List<String>> playerSuffixes
    ) {
        List<String> suffixes = new ArrayList<>();
        suffixes.add(player.getStars());
        suffixes.add(player.getFkdrColor() + player.getFormattedFkdr());

        if (player.getWinstreak() > 0) {
            String wsColor = getWinstreakColor(player.getWinstreak());
            suffixes.add(wsColor + player.getWinstreak());
        }
        playerSuffixes.put(playerName, suffixes);
    }

    /**
     * Determines the chat color for a given winstreak for tab stats.
     */
    private String getWinstreakColor(int winstreak) {
        if (winstreak >= 20) {
            return "§d";
        } else if (winstreak >= 10) {
            return "§6";
        } else if (winstreak >= 5) {
            return "§a";
        } else {
            return "§f"; // For winstreaks 1-4
        }
    }

    /**
     * Formats the full player stats string for chat display.
     */
    private String formatPlayerStats(
        String playerName,
        BedwarsPlayer player,
        boolean withTags
    ) throws IOException {
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

        if (withTags) {
            String tagsValue = buildTagsValue(playerName, player);
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
                return String.format("%s§r", base);
            } else {
                return String.format("%s §r§7|§r WS: %s§r", base, winstreak);
            }
        }
    }

    /**
     * Builds the combined tags string for a player.
     */
    private String buildTagsValue(String playerName, BedwarsPlayer player)
        throws IOException {
        String uuid = PlayerUtils.getUUIDFromPlayerName(playerName);
        int stars = parseStars(player.getStars());

        String tagsValue = tagUtils.buildTags(
            playerName,
            uuid,
            stars,
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

    /**
     * Parses the integer star level from its formatted string representation.
     * e.g., "§7[§b123✫§7]" -> 123
     */
    private int parseStars(String starString) {
        try {
            String cleaned = starString
                .replaceAll("§.", "") // Remove color codes
                .replace("[", "")
                .replace("]", "")
                .replace("✫", "");
            return Integer.parseInt(cleaned.trim());
        } catch (NumberFormatException e) {
            // Log this error if a logger is available.
            return 0;
        }
    }
}
