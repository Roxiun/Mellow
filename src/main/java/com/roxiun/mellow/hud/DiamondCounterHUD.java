package com.roxiun.mellow.hud;

import com.roxiun.mellow.api.hypixel.HypixelFeatures;
import org.polyfrost.oneconfig.api.hud.v1.HudManager;

public class DiamondCounterHUD extends MellowTextHud {
    public DiamondCounterHUD() {
        super("mellow_diamondcounterhud", "Diamonds", "Diamonds ");
        setTextColor(0xff55ffff);
    }

    @Override public boolean shouldShow() { return HypixelFeatures.getInstance().isInBedwars(); }

    @Override protected String getText() {
        HypixelFeatures features = HypixelFeatures.getInstance();
        if (HudManager.INSTANCE.isEditing() || !features.isInBedwars()) return "(2): 15s";
        return "(" + features.getDiamondSpawnCount() + "): " + Math.max(0, features.getDiamondCounterTime()) + "s";
    }

    @Override public kotlin.Pair<Float, Float> defaultPosition() { return new kotlin.Pair<>(5f, 45f); }
}
