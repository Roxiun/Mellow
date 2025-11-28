package com.roxiun.mellow.util.player;

import java.util.Collection;
import net.minecraft.client.Minecraft;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.scoreboard.ScorePlayerTeam;

public class PlayerUtils {

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
}
