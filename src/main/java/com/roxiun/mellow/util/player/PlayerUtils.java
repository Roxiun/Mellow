package com.roxiun.mellow.util.player;

import com.roxiun.mellow.api.mojang.MojangApi;
import com.roxiun.mellow.util.UUIDUtils;
import java.util.Collection;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.scoreboard.ScorePlayerTeam;

public class PlayerUtils {

    public static String getUUIDFromPlayerName(String playerName) {
        UUID uuid = getTabUuid(playerName);
        if (uuid == null) {
            return null;
        }
        return uuid.toString().replace("-", "");
    }

    public static UUID getTabUuid(String playerName) {
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
                return networkPlayerInfo.getGameProfile().getId();
            }
        }
        return null; // Player not found in tab list
    }

    public static UUID getTrustedTabUuid(String playerName) {
        UUID uuid = getTabUuid(playerName);
        return uuid != null && uuid.version() == 4 ? uuid : null;
    }

    public static UUID resolveLookupUuid(String playerName, MojangApi mojangApi) {
        if (playerName == null || playerName.trim().isEmpty() || mojangApi == null) {
            return null;
        }

        UUID trustedTabUuid = getTrustedTabUuid(playerName);
        if (trustedTabUuid != null) {
            return trustedTabUuid;
        }

        String uuid = mojangApi.fetchUUID(playerName);
        if (uuid == null || uuid.isEmpty() || "ERROR".equals(uuid)) {
            return null;
        }

        try {
            return UUIDUtils.fromString(uuid);
        } catch (Exception ignored) {
            return null;
        }
    }

    public static boolean hasTrustedTabUuid(String playerName) {
        return getTrustedTabUuid(playerName) != null;
    }

    public static boolean isNickedOrNpc(String playerName) {
        UUID uuid = getTabUuid(playerName);
        if (uuid == null) {
            return false;
        }

        int version = uuid.version();
        return version == 1 || version == 3;
    }

    public static String getRawTabListName(NetworkPlayerInfo info) {
        if (info == null || info.getGameProfile() == null) {
            return "";
        }

        if (info.getDisplayName() != null) {
            return info.getDisplayName().getFormattedText();
        }

        return ScorePlayerTeam.formatPlayerName(
            info.getPlayerTeam(),
            info.getGameProfile().getName()
        );
    }

    public static boolean hasObfuscatedFormatting(String value) {
        if (value == null || value.isEmpty()) {
            return false;
        }

        for (int i = 0; i < value.length() - 1; i++) {
            if (value.charAt(i) != '\u00A7') {
                continue;
            }

            char code = value.charAt(i + 1);
            if (code == 'k' || code == 'K') {
                return true;
            }
        }

        return false;
    }

    public static boolean isObfuscatedTabEntry(NetworkPlayerInfo info) {
        return hasObfuscatedFormatting(getRawTabListName(info));
    }

    /** Extract a color, never a style such as bold, from the team label prefix. */
    public static String getTeamColor(String prefix) {
        String color = "§f";
        if (prefix == null) {
            return color;
        }
        for (int i = 0; i + 1 < prefix.length(); i += 2) {
            if (prefix.charAt(i) != '§') {
                break;
            }
            char code = Character.toLowerCase(prefix.charAt(i + 1));
            if ("0123456789abcdef".indexOf(code) >= 0) {
                color = "§" + code;
            } else if (code == 'r') {
                color = "§f";
            }
        }
        return color;
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
