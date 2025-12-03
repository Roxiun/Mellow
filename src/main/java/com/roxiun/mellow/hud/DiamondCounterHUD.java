package com.roxiun.mellow.hud;

import cc.polyfrost.oneconfig.config.core.OneColor;
import cc.polyfrost.oneconfig.hud.SingleTextHud;
import com.roxiun.mellow.api.hypixel.HypixelFeatures;

public class DiamondCounterHUD extends SingleTextHud {

    public DiamondCounterHUD() {
        super(
            "§bDiamonds", // title is actually useful now
            true, // enabled obviously
            5, // x
            45, // y - placed below emerald counter
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
    protected String getText(boolean example) {
        if (example) return "(§f2§b): §715s";
        else {
            return HypixelFeatures.getInstance().getDiamondCounterText();
        }
    }
}
