package com.roxiun.mellow.hud;

import com.roxiun.mellow.api.hypixel.HypixelFeatures;
import org.polyfrost.oneconfig.api.hud.v1.HudManager;

public class EmeraldCounterHUD extends MellowTextHud {
    public EmeraldCounterHUD() {
        super("mellow_emeraldcounterhud", "Emeralds", "Emeralds ");
        setTextColor(0xff00aa00);
    }

    @Override public boolean shouldShow() { return HypixelFeatures.getInstance().isInBedwarsMatch(); }

    @Override protected String getText() {
        HypixelFeatures features = HypixelFeatures.getInstance();
        if (HudManager.INSTANCE.isEditing() || !features.isInBedwarsMatch()) return "(2): 15s";
        return "(" + features.getEmeraldSpawnCount() + "): " + Math.max(0, features.getEmeraldCounterTime()) + "s";
    }

    @Override public kotlin.Pair<Float, Float> defaultPosition() { return new kotlin.Pair<>(5f, 25f); }
}
