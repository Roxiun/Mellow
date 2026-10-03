package com.roxiun.mellow.hud;
import org.polyfrost.oneconfig.api.hud.v1.TextHud;
import org.polyfrost.oneconfig.api.hud.v1.Hud;

import com.roxiun.mellow.util.RgbaColor;
import com.roxiun.mellow.api.hypixel.HypixelFeatures;

public class EmeraldCounterHUD extends TextHud {

    public EmeraldCounterHUD() {
        super("mellow_emeraldcounterhud", "Emeralds", Hud.Category.getINFO(), "", "");
    }

    @Override
    public boolean shouldShow() {
        return (
            super.shouldShow() && HypixelFeatures.getInstance().isInBedwars()
        );
    }

    @Override
    protected String getText() {
        if (!HypixelFeatures.getInstance().isInBedwars()) return "§2(§f2§2): §715s";
        else {
            return HypixelFeatures.getInstance().getEmeraldCounterText();
        }
    }
    @Override public kotlin.Pair<Float, Float> defaultPosition() { return new kotlin.Pair<>(5f, 25f); }
    @Override public boolean hasBackground() { return false; }
    @Override public boolean multipleInstancesAllowed() { return false; }
}
