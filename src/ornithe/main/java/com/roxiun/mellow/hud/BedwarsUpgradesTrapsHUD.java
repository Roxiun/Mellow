package com.roxiun.mellow.hud;

import com.roxiun.mellow.api.hypixel.HypixelFeatures;
import org.polyfrost.oneconfig.api.hud.v1.HudManager;
import com.roxiun.mellow.feature.bedwars.BedwarsUpgradesService;
import java.util.List;
import org.polyfrost.compose.render.PolyColor;
import org.polyfrost.oneconfig.api.config.v1.annotations.Color;
import org.polyfrost.oneconfig.api.config.v1.annotations.Switch;

public class BedwarsUpgradesTrapsHUD extends MellowTextHud {
    @Switch(title = "Short Names", description = "Use short names (Sharp, Prot, FF, Haste, etc.)")
    public boolean shortNames = false;

    @Switch(title = "Roman Numerals", description = "Use Roman numerals (I, II, III, IV) instead of numbers")
    public boolean romanNumerals = true;

    @Color(title = "Heading Color", description = "Color for section headings (Upgrades/Traps)")
    public PolyColor headingColor = new PolyColor(0xffaa00aa);

    public BedwarsUpgradesTrapsHUD() {
        super("mellow_bedwarsupgradestrapshud", "Upgrades & Traps", "");
    }

    @Override protected PolyColor headingColor() { return headingColor; }
    @Override protected boolean isHeading(String line) {
        return line.contains("Upgrades:") || line.contains("Traps:");
    }

    @Override public boolean shouldShow() { return HypixelFeatures.getInstance().isInBedwarsMatch(); }

    protected void getLines(List<String> lines, boolean example) {
        lines.clear();
        if (example) {
            lines.addAll(BedwarsUpgradesService.getExampleDisplayLines(shortNames, romanNumerals));
        } else {
            lines.addAll(HypixelFeatures.getInstance().getBedwarsUpgradesDisplayLines(
                shortNames, romanNumerals, 255, 255, 255, 255, 255, 255, 255, 255));
        }
        // Colours belong to the HUD renderer, not the game-state strings.
        lines.replaceAll(line -> line.replaceAll("(?i)§[0-9a-fk-or]", ""));
    }

    @Override protected String getText() {
        List<String> lines = new java.util.ArrayList<>();
        getLines(lines, HudManager.INSTANCE.isEditing() || !HypixelFeatures.getInstance().isInBedwarsMatch());
        return String.join("\n", lines);
    }

    @Override public boolean showByDefault() { return true; }
    @Override public kotlin.Pair<Float, Float> defaultPosition() { return new kotlin.Pair<>(5f, 65f); }
}
