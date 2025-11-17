package com.strawberry.statsify.util;

import java.util.Collection;
import net.minecraft.client.Minecraft;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.scoreboard.ScorePlayerTeam;

public class Utils {

    public static String getUUIDFromPlayerName(String playerName) {
        if (
            Minecraft.getMinecraft().getNetHandler() == null ||
            Minecraft.getMinecraft().getNetHandler().getPlayerInfoMap() == null
        ) {
            return null;
        }
        Collection<NetworkPlayerInfo> playerInfoMap = Minecraft.getMinecraft()
            .getNetHandler()
            .getPlayerInfoMap();
        for (NetworkPlayerInfo networkPlayerInfo : playerInfoMap) {
            if (
                networkPlayerInfo
                    .getGameProfile()
                    .getName()
                    .equalsIgnoreCase(playerName)
            ) {
                return networkPlayerInfo
                    .getGameProfile()
                    .getId()
                    .toString()
                    .replace("-", "");
            }
        }
        return null; // Player not found in tab list
    }

    public static String formatWinstreak(String text) {
        String color = "\u00a7r";
        int Winstreak = Integer.parseInt(text);
        if (Winstreak >= 5 && Winstreak < 10) {
            color = "\u00a7b";
        }
        if (Winstreak >= 10 && Winstreak < 20) {
            color = "\u00a76";
        }
        if (Winstreak >= 20) {
            color = "\u00a74";
        }
        return color + text;
    }

    public static String formatStars(String text) {
        String color = "\u00a77";

        int Stars = Integer.parseInt(text);
        if (Stars < 100) {
            color = "\u00a77";
            return color + text + "\u272b";
        }

        if (Stars >= 100 && Stars < 200) {
            color = "\u00A7f";
            return color + text + "\u272b";
        }
        if (Stars >= 200 && Stars < 300) {
            color = "\u00a76";
            return color + text + "\u272b";
        }
        if (Stars >= 300 && Stars < 400) {
            color = "\u00a7b";
            return color + text + "\u272b";
        }
        if (Stars >= 400 && Stars < 500) {
            color = "\u00a72";
            return color + text + "\u272b";
        }
        if (Stars >= 500 && Stars < 600) {
            color = "\u00a73";
            return color + text + "\u272b";
        }
        if (Stars >= 600 && Stars < 700) {
            color = "\u00a74";
            return color + text + "\u272b";
        }
        if (Stars >= 700 && Stars < 800) {
            color = "\u00a7d";
            return color + text + "\u272b";
        }
        if (Stars >= 800 && Stars < 900) {
            color = "\u00a79";
            return color + text + "\u272b";
        }
        if (Stars >= 900 && Stars < 1000) {
            color = "\u00a75";
            return color + text + "\u272b";
        }
        if (Stars >= 1000 && Stars < 1100) {
            String[] digit = text.split("");
            return (
                "\u00a76" +
                digit[0] +
                "\u00a7e" +
                digit[1] +
                "\u00a7a" +
                digit[2] +
                "\u00a7b" +
                digit[3] +
                "\u00a7d" +
                "\u272a"
            );
        }
        if (Stars >= 1100 && Stars < 1200) {
            String[] digit = text.split("");
            return (
                "\u00a7f" + digit[0] + digit[1] + digit[2] + digit[3] + "\u272a"
            );
        }
        if (Stars >= 1200 && Stars < 1300) {
            String[] digit = text.split("");
            return (
                "\u00a7e" +
                digit[0] +
                digit[1] +
                digit[2] +
                digit[3] +
                "\u00a76" +
                "\u272a"
            );
        }
        if (Stars >= 1300 && Stars < 1400) {
            String[] digit = text.split("");
            return (
                "\u00a7b" +
                digit[0] +
                digit[1] +
                digit[2] +
                digit[3] +
                "\u00a73" +
                "\u272a"
            );
        }
        if (Stars >= 1400 && Stars < 1500) {
            String[] digit = text.split("");
            return (
                "\u00a7a" +
                digit[0] +
                digit[1] +
                digit[2] +
                digit[3] +
                "\u00a72" +
                "\u272a"
            );
        }
        if (Stars >= 1500 && Stars < 1600) {
            String[] digit = text.split("");
            return (
                "\u00a73" +
                digit[0] +
                digit[1] +
                digit[2] +
                digit[3] +
                "\u00a79" +
                "\u272a"
            );
        }
        if (Stars >= 1600 && Stars < 1700) {
            String[] digit = text.split("");
            return (
                "\u00a7c" +
                digit[0] +
                digit[1] +
                digit[2] +
                digit[3] +
                "\u00a74" +
                "\u272a"
            );
        }
        if (Stars >= 1700 && Stars < 1800) {
            String[] digit = text.split("");
            return (
                "\u00a7d" +
                digit[0] +
                digit[1] +
                digit[2] +
                digit[3] +
                "\u00a75" +
                "\u272a"
            );
        }
        if (Stars >= 1800 && Stars < 1900) {
            String[] digit = text.split("");
            return (
                "\u00a79" +
                digit[0] +
                digit[1] +
                digit[2] +
                digit[3] +
                "\u00a71" +
                "\u272a"
            );
        }
        if (Stars >= 1900 && Stars < 2000) {
            String[] digit = text.split("");
            return (
                "\u00a75" +
                digit[0] +
                digit[1] +
                digit[2] +
                digit[3] +
                "\u00a78" +
                "\u272a"
            );
        }
        if (Stars >= 2000 && Stars < 2100) {
            String[] digit = text.split("");
            return (
                "\u00a77" +
                digit[0] +
                "\u00a7f" +
                digit[1] +
                digit[2] +
                "\u00a77" +
                digit[3] +
                "\u269d"
            );
        }
        if (Stars >= 2100 && Stars < 2200) {
            String[] digit = text.split("");
            return (
                "\u00a7f" +
                digit[0] +
                "\u00a7e" +
                digit[1] +
                digit[2] +
                "\u00a76" +
                digit[3] +
                "\u269d"
            );
        }
        if (Stars >= 2200 && Stars < 2300) {
            String[] digit = text.split("");
            return (
                "\u00a76" +
                digit[0] +
                "\u00a7f" +
                digit[1] +
                digit[2] +
                "\u00a7b" +
                digit[3] +
                "\u00a73" +
                "\u269d"
            );
        }
        if (Stars >= 2300 && Stars < 2400) {
            String[] digit = text.split("");
            return (
                "\u00a75" +
                digit[0] +
                "\u00a7d" +
                digit[1] +
                digit[2] +
                "\u00a76" +
                digit[3] +
                "\u00a7e" +
                "\u269d"
            );
        }
        if (Stars >= 2400 && Stars < 2500) {
            String[] digit = text.split("");
            return (
                "\u00a7b" +
                digit[0] +
                "\u00a7f" +
                digit[1] +
                digit[2] +
                "\u00a77" +
                digit[3] +
                "\u269d"
            );
        }
        if (Stars >= 2500 && Stars < 2600) {
            String[] digit = text.split("");
            return (
                "\u00a7f" +
                digit[0] +
                "\u00a7a" +
                digit[1] +
                digit[2] +
                "\u00a72" +
                digit[3] +
                "\u269d"
            );
        }
        if (Stars >= 2600 && Stars < 2700) {
            String[] digit = text.split("");
            return (
                "\u00a74" +
                digit[0] +
                "\u00a7c" +
                digit[1] +
                digit[2] +
                "\u00a7d" +
                digit[3] +
                "\u269d"
            );
        }
        if (Stars >= 2700 && Stars < 2800) {
            String[] digit = text.split("");
            return (
                "\u00a7e" +
                digit[0] +
                "\u00a7f" +
                digit[1] +
                digit[2] +
                "\u00a78" +
                digit[3] +
                "\u269d"
            );
        }
        if (Stars >= 2800 && Stars < 2900) {
            String[] digit = text.split("");
            return (
                "\u00a7a" +
                digit[0] +
                "\u00a72" +
                digit[1] +
                digit[2] +
                "\u00a76" +
                digit[3] +
                "\u269d"
            );
        }
        if (Stars >= 2900 && Stars < 3000) {
            String[] digit = text.split("");
            return (
                "\u00a7b" +
                digit[0] +
                "\u00a73" +
                digit[1] +
                digit[2] +
                "\u00a79" +
                digit[3] +
                "\u269d"
            );
        }
        if (Stars >= 3000) {
            String[] digit = text.split("");
            return (
                "\u00a7e" +
                digit[0] +
                "\u00a76" +
                digit[1] +
                digit[2] +
                "\u00a7c" +
                digit[3] +
                "\u269d"
            );
        }
        return "NaN";
    }

    public static String formatRank(String rank) {
        return rank
            .replace("[VIP", "\u00a7a[VIP")
            .replace("[MVP+", "\u00a7b[MVP+")
            .replace("[MVP++", "\u00a76[MVP++");
    }

    public static String parseUsername(String str) {
        str = str.trim();
        String[] words = str.split("\\s+");
        return words.length > 0 ? words[words.length - 1] : "";
    }

    public static String getTabDisplayName(String playerName) {
        ScorePlayerTeam playerTeam = Minecraft.getMinecraft()
            .theWorld.getScoreboard()
            .getPlayersTeam(playerName);
        if (playerTeam == null) {
            return playerName;
        }

        int length = playerTeam.getColorPrefix().length();
        if (length == 10) {
            return (
                playerTeam.getColorPrefix() +
                playerName +
                playerTeam.getColorSuffix()
            );
        }
        if (length == 8) {
            return playerTeam.getColorPrefix() + playerName;
        }
        return playerName;
    }

    public static String[] getTabDisplayName2(String playerName) {
        ScorePlayerTeam playerTeam = Minecraft.getMinecraft()
            .theWorld.getScoreboard()
            .getPlayersTeam(playerName);
        if (playerTeam == null) {
            return new String[] { "", playerName, "" };
        }
        int length = playerTeam.getColorPrefix().length();
        if (length == 10) {
            String val[] = new String[3];
            val[0] = playerTeam.getColorPrefix();
            val[1] = playerName;
            val[2] = playerTeam.getColorSuffix();
            return val;
        }
        if (length == 8) {
            String val[] = new String[3];
            val[0] = playerTeam.getColorPrefix();
            val[1] = playerName;
            val[2] = "";
            return val;
        }
        return new String[] { "", playerName, "" };
    }

    public static int countOccurrences(String str, String subStr) {
        int count = 0;
        int idx = 0;
        while ((idx = str.indexOf(subStr, idx)) != -1) {
            count++;
            idx += subStr.length();
        }
        return count;
    }

    public static String extractValue(
        String text,
        String startDelimiter,
        String endDelimiter
    ) {
        int startIndex = text.indexOf(startDelimiter);
        if (startIndex == -1) return "N/A"; // Not found
        startIndex += startDelimiter.length();
        int endIndex = text.indexOf(endDelimiter, startIndex);
        if (endIndex == -1) return "N/A"; // Not found
        return text.substring(startIndex, endIndex).trim();
    }
}
