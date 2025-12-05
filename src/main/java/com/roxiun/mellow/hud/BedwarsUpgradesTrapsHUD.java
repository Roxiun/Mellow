package com.roxiun.mellow.hud;

import cc.polyfrost.oneconfig.config.annotations.Dropdown;
import cc.polyfrost.oneconfig.config.annotations.Switch;
import cc.polyfrost.oneconfig.config.core.OneColor;
import cc.polyfrost.oneconfig.hud.TextHud;
import com.roxiun.mellow.api.hypixel.HypixelFeatures;
import com.roxiun.mellow.util.MinecraftColor;
import com.roxiun.mellow.util.bedwars.BedwarsUpgradesTrapsManager;
import java.util.List;

public class BedwarsUpgradesTrapsHUD extends TextHud {

    @Switch(
        name = "Short Names",
        description = "Use short names (Sharp, Prot, FF, Haste, etc.)"
    )
    public boolean shortNames = false;

    @Switch(
        name = "Roman Numerals",
        description = "Use Roman numerals (I, II, III, IV) instead of numbers"
    )
    public boolean romanNumerals = false;

    @Dropdown(
        name = "Heading Color",
        description = "Color for section headings (Upgrades/Traps)",
        options = {
            "Black",
            "Dark Blue",
            "Dark Green",
            "Dark Aqua",
            "Dark Red",
            "Dark Purple",
            "Gold",
            "Gray",
            "Dark Gray",
            "Blue",
            "Green",
            "Aqua",
            "Red",
            "Light Purple",
            "Yellow",
            "White",
        }
    )
    public int headingColorIndex = 13; // Index for Light Purple (matches original purple &d)

    @Dropdown(
        name = "Text Color",
        description = "Color for upgrade and trap names",
        options = {
            "Black",
            "Dark Blue",
            "Dark Green",
            "Dark Aqua",
            "Dark Red",
            "Dark Purple",
            "Gold",
            "Gray",
            "Dark Gray",
            "Blue",
            "Green",
            "Aqua",
            "Red",
            "Light Purple",
            "Yellow",
            "White",
        }
    )
    public int textColorIndex = 15; // Index for White (matches original white &f)

    public BedwarsUpgradesTrapsHUD() {
        super(
            false, // enabled by default
            5, // x
            65, // y - placed below emerald and diamond counters
            1, // normal size
            false, // no background it's ugly
            false, // no rounded corners it's also ugly
            0, // NO rounded corners
            0, // no x padding why would i want it
            0, // no y padding for the same reason
            new OneColor(0, 0, 0, 0), // no background color
            false, // no border
            0, // NO border
            new OneColor(0, 0, 0, 0) // no border color
        );
        textType = 1;
    }

    @Override
    public boolean shouldShow() {
        return (
            super.shouldShow() && HypixelFeatures.getInstance().isInBedwars()
        );
    }

    @Override
    protected void getLines(List<String> lines, boolean example) {
        if (example) {
            lines.add("§d§lUpgrades:");
            lines.add("§fSharpened Swords §7II");
            lines.add("§fReinforced Armor §7III");
            lines.add("§fHeal Pool");
            lines.add("");
            lines.add("§d§lTraps:");
            lines.add("§fCounter-Offensive Trap");
            lines.add("§fIt's a Trap! Trap");
        } else {
            lines.clear();
            MinecraftColor headingColor = MinecraftColor.fromIndex(
                headingColorIndex
            );
            MinecraftColor textColor = MinecraftColor.fromIndex(textColorIndex);

            lines.addAll(
                BedwarsUpgradesTrapsManager.getInstance().getDisplayLinesWithFormatting(
                    shortNames,
                    romanNumerals,
                    headingColor.getRed(),
                    headingColor.getGreen(),
                    headingColor.getBlue(),
                    255, // alpha is always 255 for Minecraft colors
                    textColor.getRed(),
                    textColor.getGreen(),
                    textColor.getBlue(),
                    255 // alpha is always 255 for Minecraft colors
                )
            );
        }
    }
}
