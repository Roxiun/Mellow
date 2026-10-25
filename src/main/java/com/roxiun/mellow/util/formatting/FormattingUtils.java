package com.roxiun.mellow.util.formatting;

import com.roxiun.mellow.api.xadia.XadiaTag;
import com.roxiun.mellow.api.coral.CoralTag;
import java.util.List;
import java.util.stream.Collectors;

public class FormattingUtils {

    /** Shared source styling for manual headings and automatic alerts. */
    public static String formatTagSource(String source, boolean bold) {
        String color = "Coral".equals(source) ? "§5" : "Xadia".equals(source) ? "§d"
            : "Watcher".equals(source) ? "§b" : "Local".equals(source) ? "§c" : "§7";
        return color + (bold ? "§l" : "") + source + "§r" + color;
    }

    public static String formatBedwarsWinstreakWithColor(int winstreak) {
        if (winstreak < 5) {
            return "§7" + winstreak;
        }
        if (winstreak < 15) {
            return "§e" + winstreak;
        }
        if (winstreak < 25) {
            return "§6" + winstreak;
        }
        if (winstreak < 40) {
            return "§c" + winstreak;
        }
        if (winstreak < 50) {
            return "§4" + winstreak;
        }
        if (winstreak < 75) {
            return "§a" + winstreak;
        }
        if (winstreak < 100) {
            return "§2" + winstreak;
        }
        if (winstreak < 250) {
            return "§b" + winstreak;
        }
        if (winstreak < 500) {
            return "§3" + winstreak;
        }
        return "§d" + winstreak;
    }

    public static boolean isHiddenOrEmptyWinstreakDisplay(String value) {
        if (value == null || value.isEmpty()) {
            return true;
        }

        String plain = value.replaceAll("§.", "").trim();
        return (
            plain.isEmpty() ||
            "0".equals(plain) ||
            "?".equals(plain) ||
            "-".equals(plain)
        );
    }

    public static String formatWinstreak(String text) {
        String color = "§r";
        int winstreak = Integer.parseInt(text);
        if (winstreak >= 20) {
            color = "§4";
        } else if (winstreak >= 10) {
            color = "§6";
        } else if (winstreak >= 5) {
            color = "§b";
        }
        return color + text;
    }

    public static String formatTagLabel(String label, String color) {
        return color + "§l" + label + "§r";
    }

    public static String formatTagBadge(String icon, String color) {
        return "§8[" + color + icon + "§8]§r";
    }

    /** Map Cubelify's RGB badge colour to the closest Minecraft 1.8 chat colour. */
    public static String legacyColor(int rgb) {
        int[] palette = {0x000000, 0x0000AA, 0x00AA00, 0x00AAAA, 0xAA0000, 0xAA00AA, 0xFFAA00, 0xAAAAAA,
            0x555555, 0x5555FF, 0x55FF55, 0x55FFFF, 0xFF5555, 0xFF55FF, 0xFFFF55, 0xFFFFFF};
        int closest = 0, distance = Integer.MAX_VALUE;
        for (int i = 0; i < palette.length; i++) {
            int r = ((rgb >> 16) & 255) - ((palette[i] >> 16) & 255);
            int g = ((rgb >> 8) & 255) - ((palette[i] >> 8) & 255);
            int b = (rgb & 255) - (palette[i] & 255);
            int candidate = r * r + g * g + b * b;
            if (candidate < distance) { closest = i; distance = candidate; }
        }
        return "§" + "0123456789abcdef".charAt(closest);
    }

    public static String formatXadiaTags(List<XadiaTag> tags) {
        return tags.stream().map(FormattingUtils::formatXadiaTag).collect(Collectors.joining(", "));
    }

    public static String formatXadiaTag(XadiaTag tag) {
        String text = formatTagLabel(tag.getLabel(), getXadiaTagColor(tag.getType()));
        if (Boolean.FALSE.equals(tag.getVerified())) text += " §e[Unverified]";
        if (Boolean.TRUE.equals(tag.getVerified())) text += " §a[Verified]";
        if (tag.getReason() != null && !tag.getReason().trim().isEmpty()) text += " §7(" + tag.getReason() + ")";
        return text;
    }

    public static String formatXadiaTagIcon(XadiaTag tag) {
        String icon;
        switch (tag.getType()) {
            case "hacker": icon = "H"; break;
            case "sniper": icon = "S"; break;
            case "caution": icon = "C"; break;
            case "possibly_cheating": icon = "R"; break;
            case "legit_sniper": icon = "LS"; break;
            case "alt": icon = "A"; break;
            default: icon = tag.getLabel();
        }
        return formatTagBadge(icon, getXadiaTagColor(tag.getType()));
    }

    private static String getXadiaTagColor(String type) {
        // Closest legacy Minecraft colours to Xadia's tag badges.
        if (type == null) return "§d";
        switch (type) {
            case "hacker": return "§c";
            case "sniper": return "§4";
            case "legit_sniper": return "§3";
            case "alt": return "§d";
            case "caution": return "§e";
            case "possibly_cheating": return "§6";
            default: return "§d";
        }
    }

    public static String formatCoralTags(List<CoralTag> tags) {
        return tags
            .stream()
            .map(FormattingUtils::formatCoralTag)
            .collect(Collectors.joining(", "));
    }

    public static String formatCoralTag(CoralTag tag) {
        if (tag == null || tag.getType() == null) {
            return "";
        }

        String type = tag.getType();
        String formattedType;
        String reason = tag.getReason() != null && !tag.getReason().isEmpty() ? tag.getReason() : "No reason provided";

        // Use exact string matches to avoid substring replacement issues
        switch (type.toLowerCase()) {
            case "sniper":
                formattedType = "§4§lSniper";
                break;
            case "blatant_cheater":
                formattedType = "§4§lBlatant Cheater";
                break;
            case "closet_cheater":
                formattedType = "§e§lCloset Cheater";
                break;
            case "confirmed_cheater":
                formattedType = "§4§lConfirmed Cheater";
                break;
            case "replays_needed":
                formattedType = "§7§lReplays Needed";
                break;
            case "caution":
                formattedType = "§e§lCaution";
                break;
            case "possible_sniper":
                formattedType = "§e§lPossible Sniper";
                break;
            case "legit_sniper":
                formattedType = "§e§lLegit Sniper";
                break;
            case "account":
                formattedType = "§e§lAccount";
                break;
            case "info":
                formattedType = "§f§lInfo";
                break;
            default:
                // For unknown types, use the original type as-is
                formattedType = type;
                break;
        }

        return formattedType + " §7(" + reason + ")";
    }

    public static String formatCoralTagIcon(CoralTag tag) {
        String type = tag.getType().toLowerCase();
        switch (type) {
            case "sniper":
                return "§8[§4S§8]";
            case "blatant_cheater":
                return "§8[§4BC§8]";
            case "closet_cheater":
                return "§8[§6CC§8]";
            case "confirmed_cheater":
                return "§8[§cCC§8]";
            case "replays_needed":
                return "§8[§7RN§8]";
            case "possible_sniper":
                return "§8[§ePS§8]";
            case "legit_sniper":
                return "§8[§3LS§8]";
            case "caution":
                return "§8[§eC§8]";
            default:
                return "";
        }
    }

    public static String formatStars(String text) {
        try {
            return BedwarsStarFormatter.format(Integer.parseInt(text));
        } catch (NumberFormatException e) {
            return "§7[0✫]";
        }
    }

    public static String formatRank(String rank) {
        return rank
            .replace("[VIP", "§a[VIP")
            .replace("[MVP+", "§b[MVP+")
            .replace("[MVP++", "§6[MVP++");
    }

    public static String formatNickedPlayerName(String playerName) {
        net.minecraft.client.Minecraft mc =
            net.minecraft.client.Minecraft.getMinecraft();
        if (mc.theWorld == null) {
            return playerName;
        }

        net.minecraft.scoreboard.ScorePlayerTeam playerTeam = mc.theWorld
            .getScoreboard()
            .getPlayersTeam(playerName);
        String[] tabData =
            com.roxiun.mellow.util.player.PlayerUtils.getTabDisplayName2(
                playerName
            );
        String nickedPlayerDisplay;

        if (playerTeam != null && playerTeam.getColorPrefix().length() >= 2) {
            String teamName = playerTeam.getRegisteredName();
            String teamInitial = teamName.substring(0, 1).toUpperCase();
            String teamColor = com.roxiun.mellow.util.player.PlayerUtils.getTeamColor(playerTeam.getColorPrefix());

            String teamInfo = teamColor + "§l" + teamInitial + " §r";
            String coloredPlayerName = teamColor + tabData[1] + tabData[2];
            nickedPlayerDisplay = teamInfo + coloredPlayerName;
        } else {
            nickedPlayerDisplay = tabData[0] + tabData[1] + tabData[2];
        }
        return nickedPlayerDisplay;
    }

    private static String capitalizeWords(String input) {
        String[] words = input.split("_");
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < words.length; i++) {
            if (i > 0) result.append(" ");
            if (words[i].length() > 0) {
                result
                    .append(Character.toUpperCase(words[i].charAt(0)))
                    .append(words[i].substring(1).toLowerCase());
            }
        }
        return result.toString();
    }
}
