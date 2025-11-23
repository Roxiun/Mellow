package com.strawberry.statsify.api;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.strawberry.statsify.util.FormattingUtils;
import com.strawberry.statsify.util.PlayerUtils;
import com.strawberry.statsify.util.TagUtils;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.List;
import java.util.Map;
import net.minecraft.util.EnumChatFormatting;

public class HypixelApi {

    private final StatsProvider statsProvider;
    private final TagUtils tagUtils;

    public HypixelApi(StatsProvider statsProvider, TagUtils tagUtils) {
        this.statsProvider = statsProvider;
        this.tagUtils = tagUtils;
    }

    public String fetchBedwarsStats(
        String playerName,
        int minFkdr,
        boolean tags,
        boolean tabstats,
        Map<String, List<String>> playerSuffixes
    ) throws IOException {
        try {
            String uuid = PlayerUtils.getUUIDFromPlayerName(playerName);
            if (uuid == null) {
                return (
                    "§cCould not find " +
                    PlayerUtils.getTabDisplayName(playerName) +
                    " in the current lobby."
                );
            }

            String stjson = statsProvider.fetchPlayerData(uuid);
            if (stjson == null || stjson.isEmpty()) {
                return (
                    "§cFailed to get stats for " +
                    PlayerUtils.getTabDisplayName(playerName)
                );
            }

            JsonObject rootObject = new JsonParser()
                .parse(stjson)
                .getAsJsonObject();
            JsonObject profile = rootObject.getAsJsonObject("profile");
            String displayedName = profile.has("tagged_name")
                ? profile.get("tagged_name").getAsString()
                : playerName;
            JsonObject ach = rootObject.getAsJsonObject("achievements");
            String levelStr = ach.has("bedwars_level")
                ? ach.get("bedwars_level").getAsString()
                : "0";
            String formattedStars = FormattingUtils.formatStars(levelStr);

            JsonObject bedwarsStats = rootObject
                .getAsJsonObject("stats")
                .getAsJsonObject("Bedwars");

            String finalKillsStr = bedwarsStats.has("final_kills_bedwars")
                ? bedwarsStats.get("final_kills_bedwars").getAsString()
                : "0";
            String finalDeathsStr = bedwarsStats.has("final_deaths_bedwars")
                ? bedwarsStats.get("final_deaths_bedwars").getAsString()
                : "0";
            int wins = bedwarsStats.has("wins_bedwars")
                ? bedwarsStats.get("wins_bedwars").getAsInt()
                : 0;
            int losses = bedwarsStats.has("losses_bedwars")
                ? bedwarsStats.get("losses_bedwars").getAsInt()
                : 0;
            double wlr = losses == 0 ? wins : (double) wins / losses;
            DecimalFormat dfm = new DecimalFormat("#.##");
            String wlrStr = dfm.format(wlr);
            String wsStr = bedwarsStats.has("winstreak")
                ? bedwarsStats.get("winstreak").getAsString()
                : "0";

            int finalKills = Integer.parseInt(finalKillsStr.replace(",", ""));
            int finalDeaths = Integer.parseInt(finalDeathsStr.replace(",", ""));
            double fkdrValue = finalDeaths == 0
                ? finalKills
                : (double) finalKills / finalDeaths;

            if (fkdrValue < minFkdr) {
                return "";
            }

            String fkdrColor = "§7";
            if (fkdrValue >= 1 && fkdrValue < 3) fkdrColor = "§f";
            if (fkdrValue >= 3 && fkdrValue < 8) fkdrColor = "§a";
            if (fkdrValue >= 8 && fkdrValue < 16) fkdrColor = "§6";
            if (fkdrValue >= 16 && fkdrValue < 25) fkdrColor = "§d";
            if (fkdrValue > 25) fkdrColor = "§4";

            DecimalFormat df = new DecimalFormat("#.##");
            String formattedFkdr = df.format(fkdrValue);
            String formattedWinstreak = "";
            int winstreak = Integer.parseInt(wsStr.replace(",", "").trim());
            if (winstreak > 0) {
                formattedWinstreak = FormattingUtils.formatWinstreak(wsStr);
            }

            String tabfkdr = fkdrColor + formattedFkdr;
            if (tabstats) {
                java.util.List<String> suffixes = new java.util.ArrayList<>();
                suffixes.add(formattedStars);
                suffixes.add(tabfkdr);

                if (winstreak > 0) {
                    String wsColor = "§7";
                    if (winstreak >= 20) {
                        wsColor = "§d";
                    } else if (winstreak >= 10) {
                        wsColor = "§6";
                    } else if (winstreak >= 5) {
                        wsColor = "§a";
                    } else {
                        wsColor = "§f";
                    }
                    suffixes.add(wsColor + wsStr);
                }
                playerSuffixes.put(playerName, suffixes);
            }

            if (tags) {
                String tagsValue = tagUtils.buildTags(
                    playerName,
                    uuid,
                    Integer.parseInt(levelStr),
                    fkdrValue,
                    winstreak,
                    finalKills,
                    finalDeaths
                );
                if (tagsValue.endsWith(" ")) {
                    tagsValue = tagsValue.substring(0, tagsValue.length() - 1);
                }
                if (formattedWinstreak.isEmpty()) {
                    return (
                        PlayerUtils.getTabDisplayName(playerName) +
                        " §r" +
                        formattedStars +
                        "§r§7 |§r FKDR: " +
                        fkdrColor +
                        formattedFkdr +
                        " §r§7|§r [ " +
                        tagsValue +
                        " ]"
                    );
                } else {
                    return (
                        PlayerUtils.getTabDisplayName(playerName) +
                        " §r" +
                        formattedStars +
                        "§r§7 |§r FKDR: " +
                        fkdrColor +
                        formattedFkdr +
                        " §r§7|§r WS: " +
                        formattedWinstreak +
                        "§r [ " +
                        tagsValue +
                        " ]"
                    );
                }
            } else {
                if (formattedWinstreak.isEmpty()) {
                    return (
                        PlayerUtils.getTabDisplayName(playerName) +
                        " §r" +
                        formattedStars +
                        "§r§7 |§r FKDR: " +
                        fkdrColor +
                        formattedFkdr +
                        "§r"
                    );
                } else {
                    return (
                        PlayerUtils.getTabDisplayName(playerName) +
                        " §r" +
                        formattedStars +
                        "§r§7 |§r FKDR: " +
                        fkdrColor +
                        formattedFkdr +
                        " §r§7|§r WS: " +
                        formattedWinstreak +
                        "§r"
                    );
                }
            }
        } catch (Exception e) {
            return (
                EnumChatFormatting.RED + "Failed to get stats for " + playerName
            );
        }
    }
}
