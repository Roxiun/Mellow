package com.strawberry.statsify.util.formatting;

import com.strawberry.statsify.api.urchin.UrchinTag;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

public class FormattingUtils {

    private static final TreeMap<Integer, String> starBrackets =
        new TreeMap<>();
    private static final TreeMap<Integer, String[]> starColors =
        new TreeMap<>();

    static {
        starBrackets.put(0, "§7[STARS]\u272b");
        starBrackets.put(100, "§f[STARS]\u272b");
        starBrackets.put(200, "§6[STARS]\u272b");
        starBrackets.put(300, "§b[STARS]\u272b");
        starBrackets.put(400, "§2[STARS]\u272b");
        starBrackets.put(500, "§3[STARS]\u272b");
        starBrackets.put(600, "§4[STARS]\u272b");
        starBrackets.put(700, "§d[STARS]\u272b");
        starBrackets.put(800, "§9[STARS]\u272b");
        starBrackets.put(900, "§5[STARS]\u272b");

        starColors.put(
            1000,
            new String[] { "§6", "§e", "§a", "§b", "§d", "✫" }
        );
        starColors.put(
            1100,
            new String[] { "§f", "§f", "§f", "§f", "§f", "✫" }
        );
        starColors.put(
            1200,
            new String[] { "§e", "§e", "§e", "§e", "§6", "✫" }
        );
        starColors.put(
            1300,
            new String[] { "§b", "§b", "§b", "§b", "§3", "✫" }
        );
        starColors.put(
            1400,
            new String[] { "§a", "§a", "§a", "§a", "§2", "✫" }
        );
        starColors.put(
            1500,
            new String[] { "§3", "§3", "§3", "§3", "§9", "✫" }
        );
        starColors.put(
            1600,
            new String[] { "§c", "§c", "§c", "§c", "§4", "✫" }
        );
        starColors.put(
            1700,
            new String[] { "§d", "§d", "§d", "§d", "§5", "✫" }
        );
        starColors.put(
            1800,
            new String[] { "§9", "§9", "§9", "§9", "§1", "✫" }
        );
        starColors.put(
            1900,
            new String[] { "§5", "§5", "§5", "§5", "§8", "✫" }
        );

        starColors.put(
            2000,
            new String[] { "§7", "§f", "§f", "§7", "", "\u269d" }
        );
        starColors.put(
            2100,
            new String[] { "§f", "§e", "§e", "§6", "", "\u269d" }
        );
        starColors.put(
            2200,
            new String[] { "§6", "§f", "§b", "§3", "", "\u269d" }
        );
        starColors.put(
            2300,
            new String[] { "§5", "§d", "§d", "§6", "§e", "\u269d" }
        );
        starColors.put(
            2400,
            new String[] { "§b", "§f", "§f", "§7", "", "\u269d" }
        );
        starColors.put(
            2500,
            new String[] { "§f", "§a", "§a", "§2", "", "\u269d" }
        );
        starColors.put(
            2600,
            new String[] { "§4", "§c", "§c", "§d", "", "\u269d" }
        );
        starColors.put(
            2700,
            new String[] { "§e", "§f", "§f", "§8", "", "\u269d" }
        );
        starColors.put(
            2800,
            new String[] { "§a", "§2", "§2", "§6", "", "\u269d" }
        );
        starColors.put(
            2900,
            new String[] { "§b", "§3", "§3", "§9", "", "\u269d" }
        );
        starColors.put(
            3000,
            new String[] { "§e", "§6", "§6", "§c", "", "\u269d" }
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

    public static String formatUrchinTags(List<UrchinTag> tags) {
        return tags
            .stream()
            .map(tag -> {
                String formattedType = tag
                    .getType()
                    .replace("sniper", "§4§lSniper")
                    .replace("blatant_cheater", "§4§lBlatant Cheater")
                    .replace("closet_cheater", "§e§lCloset Cheater")
                    .replace("confirmed_cheater", "§4§lConfirmed Cheater")
                    .replace("possible_sniper", "§e§lPossible Sniper")
                    .replace("legit_sniper", "§e§lLegit Sniper")
                    .replace("caution", "§e§lCaution")
                    .replace("account", "§e§lAccount")
                    .replace("info", "§f§lInfo");
                return formattedType + " §7(" + tag.getReason() + ")";
            })
            .collect(Collectors.joining(", "));
    }

    public static String formatUrchinTagIcon(UrchinTag tag) {
        String type = tag.getType().toLowerCase();
        switch (type) {
            case "sniper":
                return "§8[§4S§8]";
            case "confirmed_cheater":
                return "§8[§cCC§8]";
            case "blatant_cheater":
                return "§8[§4BC§8]";
            case "closet_cheater":
                return "§8[§cCC§8]";
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
            int stars = Integer.parseInt(text);
            if (stars < 1000) {
                return starBrackets
                    .floorEntry(stars)
                    .getValue()
                    .replace("[STARS]", text);
            }

            Map.Entry<Integer, String[]> entry = starColors.floorEntry(stars);
            if (entry != null) {
                String[] colors = entry.getValue();
                char[] digits = text.toCharArray();

                if (digits.length < 4) {
                    return text;
                }

                if (stars < 2000) {
                    return (
                        colors[0] +
                        digits[0] +
                        colors[1] +
                        digits[1] +
                        colors[2] +
                        digits[2] +
                        colors[3] +
                        digits[3] +
                        colors[4] +
                        colors[5]
                    );
                } else if (stars < 3000) {
                    if (entry.getKey() == 2300) {
                        // Special case for 2300
                        return (
                            colors[0] +
                            digits[0] +
                            colors[1] +
                            digits[1] +
                            colors[2] +
                            digits[2] +
                            colors[3] +
                            digits[3] +
                            colors[4] +
                            colors[5]
                        );
                    }
                    return (
                        colors[0] +
                        digits[0] +
                        colors[1] +
                        digits[1] +
                        colors[2] +
                        digits[2] +
                        colors[3] +
                        digits[3] +
                        colors[5]
                    );
                } else {
                    // 3000+
                    return (
                        colors[0] +
                        digits[0] +
                        colors[1] +
                        digits[1] +
                        colors[2] +
                        digits[2] +
                        colors[3] +
                        digits[3] +
                        colors[5]
                    );
                }
            }
        } catch (NumberFormatException e) {
            // Fallback for non-integer text
        }
        return "NaN";
    }

    public static String formatRank(String rank) {
        return rank
            .replace("[VIP", "§a[VIP")
            .replace("[MVP+", "§b[MVP+")
            .replace("[MVP++", "§6[MVP++");
    }
}
