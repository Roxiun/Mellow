package com.roxiun.mellow.util.bedwars;

import com.roxiun.mellow.api.hypixel.HypixelFeatures;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class BedwarsUpgradesTrapsManager {

    private static final BedwarsUpgradesTrapsManager INSTANCE =
        new BedwarsUpgradesTrapsManager();

    // Track team upgrades
    private int sharpSwords = 0; // Sharpened Swords
    private int reinforcedArmor = 0; // Reinforced Armor
    private int maniacMiner = 0; // Maniac Miner
    private int cushionedBoots = 0; // Cushioned Boots
    private int forgeLevel = 0; // Forge upgrades (Iron, Golden, Emerald, Molten)
    private boolean healPool = false; // Heal Pool
    private final List<String> activeTraps = new ArrayList<>();

    // Forge level mapping
    private static final String[] FORGE_LEVELS = {
        "", // 0
        "Iron Forge", // 1
        "Golden Forge", // 2
        "Emerald Forge", // 3
        "Molten Forge", // 4
    };

    public static BedwarsUpgradesTrapsManager getInstance() {
        return INSTANCE;
    }

    private BedwarsUpgradesTrapsManager() {
        // Singleton
    }

    public void resetUpgradesAndTraps() {
        sharpSwords = 0;
        reinforcedArmor = 0;
        maniacMiner = 0;
        cushionedBoots = 0;
        forgeLevel = 0;
        healPool = false;
        activeTraps.clear();
    }

    public void processPurchaseMessage(String message) {
        String cleaned = cleanMessage(message);

        // Check if it's a purchase message
        if (!cleaned.toLowerCase().contains("purchased")) {
            return;
        }

        // Extract the item name
        String[] parts = cleaned.split("purchased");
        if (parts.length < 2) {
            return;
        }

        String item = parts[1].trim();
        item = item.replaceAll("^[\\s\"'`]+|[\\s\"'`]+$", ""); // Remove leading/trailing quotes
        item = item.replaceAll("[.!?]+$", "").trim(); // Remove trailing punctuation

        // Process different upgrade types

        // Trap detection
        if (item.toLowerCase().contains("trap")) {
            activeTraps.add(item.trim());
            return;
        }

        // Sharpened Swords
        if (item.toLowerCase().startsWith("sharpened swords")) {
            int level = extractLevel(item);
            sharpSwords = Math.max(sharpSwords, level > 0 ? level : 1);
            return;
        }

        // Reinforced Armor
        if (item.toLowerCase().startsWith("reinforced armor")) {
            int level = extractLevel(item);
            reinforcedArmor = Math.max(reinforcedArmor, level > 0 ? level : 1);
            return;
        }

        // Maniac Miner
        if (item.toLowerCase().startsWith("maniac miner")) {
            int level = extractLevel(item);
            maniacMiner = Math.max(maniacMiner, level > 0 ? level : 1);
            return;
        }

        // Cushioned Boots
        if (item.toLowerCase().startsWith("cushioned boots")) {
            int level = extractLevel(item);
            cushionedBoots = Math.max(cushionedBoots, level > 0 ? level : 1);
            return;
        }

        // Forge upgrades
        for (int i = 1; i < FORGE_LEVELS.length; i++) {
            if (item.equalsIgnoreCase(FORGE_LEVELS[i])) {
                forgeLevel = Math.max(forgeLevel, i);
                return;
            }
        }

        // Heal Pool
        if (item.toLowerCase().startsWith("heal pool")) {
            healPool = true;
            return;
        }
    }

    public void processTrapTriggeredMessage(String message) {
        String cleaned = cleanMessage(message);

        // Check for trap triggered messages
        Pattern trapPattern = Pattern.compile(
            "^(.+?)\\s+Trap was set off!$",
            Pattern.CASE_INSENSITIVE
        );
        Matcher matcher = trapPattern.matcher(cleaned);

        if (matcher.matches()) {
            String trapName = matcher.group(1).trim() + " Trap";
            removeTrapFromQueue(trapName);
            return;
        }

        // Handle Reveal trap (different message format)
        if (cleaned.toLowerCase().contains("reveal trap set off")) {
            removeTrapFromQueue("Reveal Trap");
            return;
        }

        // Handle trap removal messages
        Pattern removePattern = Pattern.compile(
            "^Removed\\s+(.+?)\\s+Trap from the queue!$",
            Pattern.CASE_INSENSITIVE
        );
        Matcher removeMatcher = removePattern.matcher(cleaned);

        if (removeMatcher.matches()) {
            String trapName = removeMatcher.group(1).trim() + " Trap";
            removeTrapFromQueue(trapName);
        }
    }

    private void removeTrapFromQueue(String trapName) {
        activeTraps.removeIf(trap -> trap.equalsIgnoreCase(trapName));
    }

    private int extractLevel(String item) {
        // Look for Roman numerals or numbers in the item name
        if (item.contains("I")) {
            // Count occurrences of I, V, X to determine level
            if (item.contains("IV")) return 4;
            if (item.contains("III")) return 3;
            if (item.contains("II")) return 2;
            if (
                item.contains("I") && !item.contains("V") && !item.contains("X")
            ) return 1;
        }

        // Look for numeric levels
        Pattern numberPattern = Pattern.compile("([IV1-4])$");
        Matcher matcher = numberPattern.matcher(item.trim());
        if (matcher.find()) {
            String levelStr = matcher.group(1);
            switch (levelStr) {
                case "I":
                    return 1;
                case "II":
                    return 2;
                case "III":
                    return 3;
                case "IV":
                    return 4;
                default:
                    try {
                        return Integer.parseInt(levelStr);
                    } catch (NumberFormatException e) {
                        return 1;
                    }
            }
        }

        return 1; // Default to level 1 if no level found
    }

    private String cleanMessage(String message) {
        // Remove Minecraft formatting codes (§ + character)
        return message.replaceAll("§[0-9A-FK-OR]", "").trim();
    }

    public List<String> getDisplayLines() {
        return getDisplayLinesWithFormatting(
            false,
            false,
            221,
            0,
            255,
            255,
            255,
            255,
            255,
            255
        );
    }

    public List<String> getDisplayLinesWithFormatting(
        boolean useShortNames,
        boolean useRomanNumerals,
        int headingRed,
        int headingGreen,
        int headingBlue,
        int headingAlpha,
        int textRed,
        int textGreen,
        int textBlue,
        int textAlpha
    ) {
        List<String> lines = new ArrayList<>();

        // Create color codes for headings and text
        String headingColorCode = String.format(
            "§%s",
            formatColorCode(headingRed, headingGreen, headingBlue, headingAlpha)
        );
        String textColorCode = String.format(
            "§%s",
            formatColorCode(textRed, textGreen, textBlue, textAlpha)
        );

        // Add Upgrades heading
        lines.add(headingColorCode + "§lUpgrades:");

        // Add upgrade lines with short names support
        if (sharpSwords == 2) {
            String name = useShortNames
                ? getShortName("Sharpened Swords")
                : "Sharpened Swords";
            String level = useRomanNumerals ? "II" : "2";
            lines.add(textColorCode + name + " §7" + level);
        } else if (sharpSwords == 1) {
            String name = useShortNames
                ? getShortName("Sharpened Swords")
                : "Sharpened Swords";
            lines.add(textColorCode + name);
        }

        if (reinforcedArmor > 0) {
            String name = useShortNames
                ? getShortName("Reinforced Armor")
                : "Reinforced Armor";
            String level = useRomanNumerals
                ? getRomanNumeral(reinforcedArmor)
                : String.valueOf(reinforcedArmor);
            lines.add(textColorCode + name + " §7" + level);
        }

        if (maniacMiner > 0) {
            String name = useShortNames
                ? getShortName("Maniac Miner")
                : "Maniac Miner";
            String level = useRomanNumerals
                ? getRomanNumeral(maniacMiner)
                : String.valueOf(maniacMiner);
            lines.add(textColorCode + name + " §7" + level);
        }

        if (cushionedBoots > 0) {
            String name = useShortNames
                ? getShortName("Cushioned Boots")
                : "Cushioned Boots";
            String level = useRomanNumerals
                ? getRomanNumeral(cushionedBoots)
                : String.valueOf(cushionedBoots);
            lines.add(textColorCode + name + " §7" + level);
        }

        if (forgeLevel > 0 && forgeLevel < FORGE_LEVELS.length) {
            lines.add(textColorCode + FORGE_LEVELS[forgeLevel]);
        }

        if (healPool) {
            lines.add(textColorCode + "Heal Pool");
        }

        if (
            sharpSwords == 0 &&
            reinforcedArmor == 0 &&
            maniacMiner == 0 &&
            cushionedBoots == 0 &&
            forgeLevel == 0 &&
            !healPool
        ) {
            lines.add(textColorCode + "None");
        }

        // Add empty line before traps
        lines.add("");

        // Add Traps heading
        lines.add(headingColorCode + "§lTraps:");

        // Add trap lines with short names support
        if (!activeTraps.isEmpty()) {
            for (String trap : activeTraps) {
                String formattedTrap = useShortNames
                    ? getShortTrapName(trap)
                    : trap;
                lines.add(textColorCode + formattedTrap);
            }
        } else {
            lines.add(textColorCode + "None");
        }

        return lines;
    }

    private String formatColorCode(int red, int green, int blue, int alpha) {
        // Convert RGB to Minecraft color code
        // We'll use the closest matching color code based on the RGB values
        if (red == 221 && green == 0 && blue == 255 && alpha == 255) return "d"; // &d (purple)
        if (
            red == 255 && green == 255 && blue == 255 && alpha == 255
        ) return "f"; // &f (white)
        if (
            red == 127 && green == 127 && blue == 127 && alpha == 255
        ) return "7"; // &7 (gray)

        // If not one of the common ones, return white as default
        return "d";
    }

    private String getShortName(String fullName) {
        switch (fullName) {
            case "Sharpened Swords":
                return "Sharp";
            case "Reinforced Armor":
                return "Prot";
            case "Cushioned Boots":
                return "FF";
            case "Maniac Miner":
                return "Haste";
            default:
                return fullName;
        }
    }

    private String getShortTrapName(String fullName) {
        String name = fullName;

        // Remove "Trap" from the end if it exists
        if (name.toLowerCase().endsWith(" trap")) {
            name = name.substring(0, name.length() - 5).trim();
        }

        // Map to short names
        switch (name) {
            case "Miner Fatigue":
            case "Mining Fatigue":
                return "Fatigue";
            case "Reveal":
            case "Alarm":
                return "Reveal";
            case "Counter-Offensive":
            case "Counter Offensive":
            case "Counter Offense":
                return "Jump";
            case "Blindness":
                return "Blind";
            case "It's a Trap!":
            case "It's a Trap":
                return "Trap";
            default:
                // For other cases, try to match common patterns
                String lower = name.toLowerCase();
                if (lower.contains("fatigue")) return "Fatigue";
                if (
                    lower.contains("reveal") || lower.contains("alarm")
                ) return "Reveal";
                if (lower.contains("counter")) return "Jump";
                if (lower.contains("blind")) return "Blind";
                return name;
        }
    }

    private String getRomanNumeral(int number) {
        switch (number) {
            case 1:
                return "I";
            case 2:
                return "II";
            case 3:
                return "III";
            case 4:
                return "IV";
            default:
                return String.valueOf(number);
        }
    }

    // Getters for individual values (useful for testing or other features)
    public int getSharpSwords() {
        return sharpSwords;
    }

    public int getReinforcedArmor() {
        return reinforcedArmor;
    }

    public int getManiacMiner() {
        return maniacMiner;
    }

    public int getCushionedBoots() {
        return cushionedBoots;
    }

    public int getForgeLevel() {
        return forgeLevel;
    }

    public boolean hasHealPool() {
        return healPool;
    }

    public List<String> getActiveTraps() {
        return Collections.unmodifiableList(activeTraps);
    }
}
