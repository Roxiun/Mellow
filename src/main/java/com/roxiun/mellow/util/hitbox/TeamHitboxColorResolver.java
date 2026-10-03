package com.roxiun.mellow.util.hitbox;

import com.roxiun.mellow.util.RgbaColor;
import com.roxiun.mellow.config.MellowOneConfig;
import com.roxiun.mellow.util.MinecraftColor;
import java.awt.Color;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.scoreboard.ScorePlayerTeam;
import net.minecraft.scoreboard.Team;

public final class TeamHitboxColorResolver {

    private static final String MC_COLOR_CODES = "0123456789abcdef";

    private TeamHitboxColorResolver() {}

    public static RgbaColor resolveTeamHitboxColor(
        Entity entity,
        MellowOneConfig config,
        int alpha
    ) {
        if (entity == null || config == null) {
            return null;
        }
        if (!(entity instanceof EntityPlayer)) {
            return null;
        }

        Team team = ((EntityPlayer) entity).getTeam();
        if (!(team instanceof ScorePlayerTeam)) {
            return null;
        }

        String colorPrefix = ((ScorePlayerTeam) team).getColorPrefix();
        char colorCode = extractMinecraftColorCode(colorPrefix);
        int colorIndex = MC_COLOR_CODES.indexOf(colorCode);
        if (colorIndex < 0) {
            return null;
        }

        MinecraftColor base = MinecraftColor.fromIndex(colorIndex);
        int[] rgb = applyHsvAdjustments(
            base.getRed(),
            base.getGreen(),
            base.getBlue(),
            config
        );

        return new RgbaColor(rgb[0], rgb[1], rgb[2], clamp(alpha, 0, 255));
    }

    private static int[] applyHsvAdjustments(
        int red,
        int green,
        int blue,
        MellowOneConfig config
    ) {
        float[] hsv = Color.RGBtoHSB(red, green, blue, null);

        int hue = Math.round(hsv[0] * 360.0f);
        int saturation = Math.round(hsv[1] * 100.0f);
        int brightness = Math.round(hsv[2] * 100.0f);

        hue = config.hitboxHueMode == 0
            ? wrapDegrees(hue + config.hitboxHueOffset)
            : clamp(config.hitboxHueValue, 0, 360);
        saturation = config.hitboxSaturationMode == 0
            ? clamp(saturation + config.hitboxSaturationOffset, 0, 100)
            : clamp(config.hitboxSaturationValue, 0, 100);
        brightness = config.hitboxBrightnessMode == 0
            ? clamp(brightness + config.hitboxBrightnessOffset, 0, 100)
            : clamp(config.hitboxBrightnessValue, 0, 100);

        int rgb = Color.HSBtoRGB(
            hue / 360.0f,
            saturation / 100.0f,
            brightness / 100.0f
        );
        return new int[] { (rgb >> 16) & 0xFF, (rgb >> 8) & 0xFF, rgb & 0xFF };
    }

    private static char extractMinecraftColorCode(String input) {
        if (input == null || input.length() < 2) {
            return '\0';
        }

        for (int i = 0; i < input.length() - 1; i++) {
            if (input.charAt(i) == '\u00A7') {
                char maybeColor = Character.toLowerCase(input.charAt(i + 1));
                if (MC_COLOR_CODES.indexOf(maybeColor) >= 0) {
                    return maybeColor;
                }
            }
        }
        return '\0';
    }

    private static int wrapDegrees(int value) {
        int wrapped = value % 360;
        return wrapped < 0 ? wrapped + 360 : wrapped;
    }

    private static int clamp(int value, int min, int max) {
        if (value < min) {
            return min;
        }
        return value > max ? max : value;
    }
}
