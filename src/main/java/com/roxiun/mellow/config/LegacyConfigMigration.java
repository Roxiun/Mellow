package com.roxiun.mellow.config;

import com.google.gson.*;
import java.lang.reflect.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import org.polyfrost.oneconfig.api.config.v1.ConfigManager;

/** Imports matching scalar settings once. The v0 file is never changed. */
public final class LegacyConfigMigration {
    private LegacyConfigMigration() {}

    public static boolean importIfNeeded(MellowOneConfig config, boolean existedBeforeInitialization) {
        if (existedBeforeInitialization) return false;
        Path folder = ConfigManager.active().getFolder();
        Path source = folder.resolve("mellow.json");
        if (!Files.isRegularFile(source)) source = Paths.get("config", "mellow.json");
        if (!Files.isRegularFile(source)) return false;
        try (java.io.Reader reader = Files.newBufferedReader(source, StandardCharsets.UTF_8)) {
            JsonObject old = new JsonParser().parse(reader).getAsJsonObject();
            Gson gson = new Gson();
            java.util.Map<Field, Object> pending = new java.util.LinkedHashMap<>();
            for (Field field : MellowOneConfig.class.getDeclaredFields()) {
                if (Modifier.isStatic(field.getModifiers()) || Modifier.isFinal(field.getModifiers())) continue;
                if (!(field.getType().isPrimitive() || field.getType() == String.class)) continue;
                JsonElement value = old.get(field.getName());
                if (value == null || value.isJsonNull() || !value.isJsonPrimitive()) continue;
                field.setAccessible(true);
                pending.put(field, gson.fromJson(value, field.getType()));
            }
            for (var entry : pending.entrySet()) entry.getKey().set(config, entry.getValue());
            return !pending.isEmpty();
        } catch (Exception e) {
            org.apache.logging.log4j.LogManager.getLogger("Mellow").warn("Could not import legacy settings from " + source, e);
            return false;
        }
    }
}
