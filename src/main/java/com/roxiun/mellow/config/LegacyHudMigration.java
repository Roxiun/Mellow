package com.roxiun.mellow.config;

import com.google.gson.*;
import com.roxiun.mellow.hud.MellowTextHud;
import java.util.function.Consumer;
import org.polyfrost.compose.render.PolyColor;
import org.polyfrost.oneconfig.api.hud.v1.HudManager;

/** Translates the v0 HUD settings which have direct equivalents in the v1 editor. */
public final class LegacyHudMigration {
    private LegacyHudMigration() {}

    public static void apply(MellowTextHud hud, JsonObject old) {
        LegacyConfigMigration.importScalars(hud, old);
        bool(old, "enabled", value -> hud.setHidden(!value));
        bool(old, "locked", hud::setLocked);
        bool(old, "background", hud::setShowBackground);
        bool(old, "showInChat", hud::setShowInChat);
        bool(old, "showInDebug", hud::setShowInF3);
        bool(old, "showInGuis", hud::setShowInScreens);
        number(old, "scale", value -> hud.setCustomScale(Math.max(0.1f, Math.min(10f, value))));
        number(old, "paddingX", value -> { hud.setPadLeft(Math.max(0, value)); hud.setPadRight(Math.max(0, value)); });
        number(old, "paddingY", value -> { hud.setPadTop(Math.max(0, value)); hud.setPadBottom(Math.max(0, value)); });
        number(old, "textType", value -> hud.setShowShadow(value != 0));
        if (old.has("rounded") && old.get("rounded").isJsonPrimitive() && old.get("rounded").getAsBoolean()) {
            number(old, "cornerRadius", value -> hud.setBgRadius(Math.max(0, value)));
        } else hud.setBgRadius(0);
        PolyColor text = color(old.get("color"));
        if (text != null) {
            hud.setTextColor(text.getRawArgb());
            hud.setTextChroma(text.getChroma());
            hud.setTextChromaSpeed(text.getChromaSpeed());
        }
        PolyColor background = color(old.get("bgColor"));
        if (background != null) {
            hud.setBgColor(background.getRawArgb());
            hud.setBgChroma(background.getChroma());
            hud.setBgChromaSpeed(background.getChromaSpeed());
        }
        if (old.has("position") && old.get("position").isJsonObject()) {
            JsonObject position = old.getAsJsonObject("position");
            try {
                int anchor = Math.max(0, Math.min(8, position.get("anchor").getAsInt()));
                float x = position.get("x").getAsFloat();
                float y = position.get("y").getAsFloat();
                float width = position.get("width").getAsFloat();
                float height = position.get("height").getAsFloat();
                x += (HudManager.guiScreenWidth - width) * (anchor % 3) / 2f;
                y += (HudManager.guiScreenHeight - height) * (anchor / 3) / 2f;
                if (Float.isFinite(x) && Float.isFinite(y)) hud.setAbsolutePosition(x, y);
            } catch (RuntimeException ignored) { /* Keep the native default if the old position is incomplete. */ }
        }
    }

    static PolyColor color(JsonElement value) {
        if (value == null || !value.isJsonObject()) return null;
        try {
            JsonObject old = value.getAsJsonObject();
            JsonArray hsba = old.getAsJsonArray("hsba");
            int rgb = java.awt.Color.HSBtoRGB(hsba.get(0).getAsFloat() / 360f,
                hsba.get(1).getAsFloat() / 100f, hsba.get(2).getAsFloat() / 100f);
            int alpha = Math.max(0, Math.min(255, hsba.get(3).getAsInt()));
            int period = old.has("dataBit") ? old.get("dataBit").getAsInt() : -1;
            return new PolyColor((alpha << 24) | (rgb & 0xffffff), period > 0,
                period > 0 ? Math.max(0.1f, Math.min(10f, 10000f / period)) : 1f);
        } catch (RuntimeException ignored) { return null; }
    }

    private static void bool(JsonObject old, String name, Consumer<Boolean> setter) {
        JsonElement value = old.get(name);
        if (value != null && value.isJsonPrimitive() && value.getAsJsonPrimitive().isBoolean()) setter.accept(value.getAsBoolean());
    }

    private static void number(JsonObject old, String name, Consumer<Float> setter) {
        JsonElement value = old.get(name);
        if (value != null && value.isJsonPrimitive() && value.getAsJsonPrimitive().isNumber()) {
            float number = value.getAsFloat();
            if (Float.isFinite(number)) setter.accept(number);
        }
    }
}
