package com.roxiun.mellow.config;

import com.google.gson.*;
import java.io.Reader;
import java.lang.reflect.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import org.polyfrost.oneconfig.api.config.v1.ConfigManager;

/** Imports matching scalar settings once. Legacy files are never changed. */
public final class LegacyConfigMigration {
    private LegacyConfigMigration() {}

    public static boolean importIfNeeded(MellowOneConfig config, boolean existedBeforeInitialization) {
        if (existedBeforeInitialization) return false;
        Path source = findSource(Paths.get(""), ConfigManager.active().getFolder(), ConfigManager.activeProfile());
        if (source == null) return false;
        JsonObject old = readObject(source);
        if (old == null) return false;
        boolean changed = importScalars(config, old);
        changed |= prepareHud(config.emeraldCounterHUD, old, "emeraldCounterHUD");
        changed |= prepareHud(config.diamondCounterHUD, old, "diamondCounterHUD");
        changed |= prepareHud(config.upgradesTrapsHUD, old, "upgradesTrapsHUD");
        return changed;
    }

    private static boolean prepareHud(com.roxiun.mellow.hud.MellowTextHud hud, JsonObject old, String name) {
        JsonElement value = old.get(name);
        if (value == null || !value.isJsonObject()) return false;
        // Never replace a native HUD configuration which already exists.
        if (ConfigManager.active().load("huds/" + hud.id) != null) return false;
        hud.prepareLegacySettings(value.getAsJsonObject());
        return true;
    }

    static Path findSource(Path gameDirectory, Path activeFolder, String profile) {
        Path local = activeFolder.resolve("mellow.json");
        if (Files.isRegularFile(local)) return local;
        // v0 stored profiles separately from v1's config/ and profiles/ directories.
        String legacyProfile = profile;
        if (legacyProfile.isEmpty()) {
            legacyProfile = "Default Profile";
            JsonObject settings = readObject(gameDirectory.resolve("OneConfig/OneConfig.json"));
            if (settings != null && settings.has("currentProfile") && settings.get("currentProfile").isJsonPrimitive()) {
                legacyProfile = settings.get("currentProfile").getAsString();
            }
        }
        Path profiles = gameDirectory.resolve("OneConfig/profiles").toAbsolutePath().normalize();
        Path source = profiles.resolve(legacyProfile).resolve("mellow.json").normalize();
        return source.startsWith(profiles) && Files.isRegularFile(source) ? source : null;
    }

    static JsonObject readObject(Path source) {
        if (!Files.isRegularFile(source)) return null;
        try (Reader reader = Files.newBufferedReader(source, StandardCharsets.UTF_8)) {
            JsonElement parsed = new JsonParser().parse(reader);
            return parsed.isJsonObject() ? parsed.getAsJsonObject() : null;
        } catch (Exception e) {
            // Do not log parsed values: legacy configs contain API keys.
            org.apache.logging.log4j.LogManager.getLogger("Mellow").warn("Could not read legacy settings from {}", source);
            return null;
        }
    }

    static boolean importScalars(Object config, JsonObject old) {
        boolean changed = false;
        Gson gson = new Gson();
        for (Field field : config.getClass().getDeclaredFields()) {
            if (Modifier.isStatic(field.getModifiers()) || Modifier.isFinal(field.getModifiers())) continue;
            if (!(field.getType().isPrimitive() || field.getType() == String.class)) continue;
            JsonElement value = old.get(field.getName());
            if (value == null || !value.isJsonPrimitive()) continue;
            try {
                JsonPrimitive primitive = value.getAsJsonPrimitive();
                if (field.getType() == boolean.class && !primitive.isBoolean()) continue;
                if (field.getType() == String.class && !primitive.isString()) continue;
                if (field.getType() != String.class && field.getType() != boolean.class && !primitive.isNumber()) continue;
                field.setAccessible(true);
                field.set(config, gson.fromJson(value, field.getType()));
                changed = true;
            } catch (Exception ignored) {
                // A single malformed setting must not discard every other valid option.
            }
        }
        return changed;
    }
}
