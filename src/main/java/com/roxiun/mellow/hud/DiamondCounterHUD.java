package com.roxiun.mellow.hud;
import org.polyfrost.oneconfig.api.hud.v1.TextHud;
import org.polyfrost.oneconfig.api.hud.v1.Hud;

import com.roxiun.mellow.util.RgbaColor;
import com.roxiun.mellow.api.hypixel.HypixelFeatures;

public class DiamondCounterHUD extends TextHud {

    public DiamondCounterHUD() {
        super("mellow_diamondcounterhud", "Diamonds", Hud.Category.getINFO(), "", "");
    }

    @Override
    public boolean shouldShow() {
        return (
            super.shouldShow() && HypixelFeatures.getInstance().isInBedwars()
        );
    }

    @Override
    protected String getText() {
        if (!HypixelFeatures.getInstance().isInBedwars()) return "§b(§f2§b): §715s";
        else {
            return HypixelFeatures.getInstance().getDiamondCounterText();
        }
    }
    @Override public kotlin.Pair<Float, Float> defaultPosition() { return new kotlin.Pair<>(5f, 45f); }
    @Override public boolean hasBackground() { return false; }
    @Override public boolean multipleInstancesAllowed() { return false; }
}
