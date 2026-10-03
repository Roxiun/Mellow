package com.roxiun.mellow.hud;
import org.polyfrost.oneconfig.api.hud.v1.TextHud;
import org.polyfrost.oneconfig.api.hud.v1.Hud;

import org.polyfrost.oneconfig.api.config.v1.annotations.Dropdown;
import org.polyfrost.oneconfig.api.config.v1.annotations.Switch;
import com.roxiun.mellow.util.RgbaColor;
import com.roxiun.mellow.api.hypixel.HypixelFeatures;
import com.roxiun.mellow.util.MinecraftColor;
import java.util.List;

public class BedwarsUpgradesTrapsHUD extends TextHud {

    @Switch(
        title = "Short Names",
        description = "Use short names (Sharp, Prot, FF, Haste, etc.)"
    )
    public boolean shortNames = false;

    @Switch(
        title = "Roman Numerals",
        description = "Use Roman numerals (I, II, III, IV) instead of numbers"
    )
    public boolean romanNumerals = true;

    @Dropdown(
        title = "Heading Color",
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
    public int headingColorIndex = 5; // Index for dark purple

    @Dropdown(
        title = "Text Color",
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
        super("mellow_bedwarsupgradestrapshud", "Upgrades & Traps", Hud.Category.getINFO(), "", "");
    }

    @Override
    public boolean shouldShow() {
        return (
            super.shouldShow() && HypixelFeatures.getInstance().isInBedwars()
        );
    }

    protected void getLines(List<String> lines, boolean example) {
        if (example) {
            lines.add("§d§lUpgrades:");
            lines.add("§fSharpened Swords §7II");
            lines.add("§fReinforced Armor §7III");
            lines.add("§fHeal Pool");
            lines.add("");
            lines.add("§d§lTraps:");
            lines.add("§fCounter-Offensive Trap");
            lines.add("§fBlindness Trap");
        } else {
            lines.clear();
            MinecraftColor headingColor = MinecraftColor.fromIndex(
                headingColorIndex
            );
            MinecraftColor textColor = MinecraftColor.fromIndex(textColorIndex);

            lines.addAll(
                HypixelFeatures.getInstance().getBedwarsUpgradesDisplayLines(
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
    @Override protected String getText() {
        java.util.List<String> lines = new java.util.ArrayList<>();
        getLines(lines, !HypixelFeatures.getInstance().isInBedwars());
        return String.join("\n", lines);
    }
    @Override public boolean showByDefault() { return true; }
    @Override public kotlin.Pair<Float, Float> defaultPosition() { return new kotlin.Pair<>(5f, 65f); }
    @Override public boolean hasBackground() { return false; }
    @Override public boolean multipleInstancesAllowed() { return false; }
}
