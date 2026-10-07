package com.roxiun.mellow.util.formatting;

import com.roxiun.mellow.api.xadia.XadiaTag;
import com.roxiun.mellow.api.seraph.SeraphTag;
import com.roxiun.mellow.api.coral.CoralTag;
import java.util.List;
import java.util.stream.Collectors;

public class FormattingUtils {

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

    public static String formatXadiaTags(List<XadiaTag> tags) {
        return tags.stream().map(FormattingUtils::formatXadiaTag).collect(Collectors.joining(", "));
    }

    public static String formatXadiaTag(XadiaTag tag) {
        String text = getXadiaTagColor(tag.getType()) + "§l" + tag.getLabel() + "§r";
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
        return "§8[" + getXadiaTagColor(tag.getType()) + icon + "§8]§r";
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

    public static String formatSeraphTags(List<SeraphTag> tags) {
    boolean hasVerifiedTag = tags.stream()
        .anyMatch(tag -> "seraph.verified".equalsIgnoreCase(tag.getTagName()));

    String verificationStatus = hasVerifiedTag ? "§a(Verified)" : "§c(Not Verified)";

    String formattedTags = tags.stream()
        .map(tag -> {
            // Don't skip unmapped tags - show them using tag name and tooltip
            if (
                tag.getTagName() != null &&
                !tag.getTagName().isEmpty() &&
                !"seraph.verified".equals(tag.getTagName()) &&
                !"seraph.advertisement".equals(tag.getTagName())
            ) {
                // Format mapped tags nicely, or show unmapped ones with nice formatting
                String formattedTag = formatSeraphTag(tag.getTagName());
                if (formattedTag != null && !formattedTag.isEmpty()) {
                    return (
                        formattedTag + " §7(" + tag.getTooltip() + ")"
                    );
                } else {
                    // For unmapped tags, create a nicely formatted display name
                    String baseName = tag
                        .getTagName()
                        .replace("seraph.", "");
                    String displayName = capitalizeWords(baseName);
                    return (
                        "§7" +
                        displayName +
                        " §7(" +
                        tag.getTooltip() +
                        ")"
                    );
                }
            } else if (
                tag.getTagName() == null || tag.getTagName().isEmpty()
            ) {
                // If tag has no tag_name but has tooltip, show it with generic label
                if (
                    tag.getTooltip() != null &&
                    !tag.getTooltip().isEmpty()
                ) {
                    return "§7Other §7(" + tag.getTooltip() + ")";
                } else {
                    return null;
                }
            } else {
                // This is seraph.verified or seraph.advertisement - skip it in the list
                return null;
            }
        })
        .filter(tag -> tag != null && !tag.trim().isEmpty())
        .collect(Collectors.joining(" §7| "));

    if (formattedTags.isEmpty()) {
        return verificationStatus;
    }
    return formattedTags + " " + verificationStatus;
}

public static String formatSeraphTag(String tagName) {
    if (tagName == null) return "";

    switch (tagName.toLowerCase()) {
        case "seraph.sniping":
        case "seraph.blatant_cheating":
            return "§4§lSniping/Cheating"; // darkred as specified
        case "seraph.legit_sniping":
            return "§c§lLegit Sniper"; // lightred as specified
        case "seraph.potential_sniper":
            return "§e§lPotential Sniper"; // yellow as specified
        case "seraph.bot":
            return "§8§lBot"; // grey as specified
        case "seraph.alt":
            return "§d§lAlt"; // pink as specified
        case "seraph.safelist.personal":
        case "seraph.safelist.group":
        case "seraph.safelist.global":
            return "§a§lSafelist"; // green as specified
        case "seraph.annoylist":
            return "§e§lAnnoying"; // yellow as specified
        case "seraph.encounters":
            return "§c§lEncounters"; // lightred as specified
        case "seraph.cookie":
            return "§c§lEncounters"; // lightred as specified
        case "seraph.caution":
            return "§6§lCaution"; // 0xffc107 = §6 as specified
        case "seraph.closet_cheating":
            return "§e§lCloset Cheater"; // yellow/lightred-like
        case "seraph.verified":
            return "§a§lVerified";
        default:
            // Skip unmapped tags
            return "";
    }
}

public static String formatSeraphTagIcon(SeraphTag tag) {
    String tagName = tag.getTagName().toLowerCase();
    switch (tagName) {
        case "seraph.sniping":
            return "§8[§4S§8]";
        case "seraph.blatant_cheating":
            return "§8[§4BC§8]";
        case "seraph.legit_sniping":
            return "§8[§cLS§8]";
        case "seraph.potential_sniper":
            return "§8[§ePS§8]";
        case "seraph.bot":
            return "§8[§8BOT§8]";
        case "seraph.alt":
            return "§8[§dALT§8]";
        case "seraph.safelist.personal":
        case "seraph.safelist.group":
        case "seraph.safelist.global":
            return "§8[§2§l✓§8]";
        case "seraph.annoylist":
            return "§8[§eAN§8]";
        case "seraph.encounters":
            return "§8[§eSEEN§8]";
        case "seraph.cookie":
            return "§8[§cCOOKIE§8]";
        case "seraph.caution":
            return "§8[§6C§8]";
        case "seraph.closet_cheating":
            return "§8[§eCC§8]";
        case "seraph.verified":
            return "§8[§2§l✓§8]";
        default:
            // Skip unmapped tags
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
