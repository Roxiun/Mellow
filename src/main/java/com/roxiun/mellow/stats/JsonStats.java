package com.roxiun.mellow.stats;

import com.google.gson.*;
import com.roxiun.mellow.api.model.*;

/** Tolerant accessors for the numeric/string variants returned by stats providers. */
public final class JsonStats {
    private JsonStats() {}
    public static boolean hasObject(JsonObject object, String key) {
        return object != null && object.has(key) && object.get(key).isJsonObject();
    }
    public static boolean hasKey(JsonObject object, String... keys) {
        if (object == null) return false;
        for (String key : keys) if (object.has(key) && !object.get(key).isJsonNull()) return true;
        return false;
    }
    public static String normalizeFormatting(String value) {
        if (value == null) {
            return "";
        }
        return value.replace('&', '§').trim();
    }

    public static String findFirstNonEmptyString(
        JsonObject object,
        String[] keys
    ) {
        if (object == null || keys == null || keys.length == 0) {
            return "";
        }

        for (String key : keys) {
            String value = normalizeFormatting(getString(object, key, "")).trim();
            if (!value.isEmpty()) {
                return value;
            }
        }

        return "";
    }

    public static int maxExistingInt(JsonObject object, String[] keys) {
        if (object == null || keys == null || keys.length == 0) {
            return Integer.MIN_VALUE;
        }

        int best = Integer.MIN_VALUE;
        for (String key : keys) {
            if (key == null || key.isEmpty() || !object.has(key)) {
                continue;
            }
            best = Math.max(best, getInt(object, key, 0));
        }
        return best;
    }

    public static int maxExistingIntAcrossObjects(
        JsonObject[] objects,
        String[] keys
    ) {
        if (objects == null || objects.length == 0) {
            return Integer.MIN_VALUE;
        }

        int best = Integer.MIN_VALUE;
        for (JsonObject object : objects) {
            int value = maxExistingInt(object, keys);
            if (value == Integer.MIN_VALUE) {
                continue;
            }
            best = Math.max(best, value);
        }

        return best;
    }

    public static int sumBestPerKeyAcrossObjects(
        JsonObject[] objects,
        String[] keys
    ) {
        if (
            objects == null ||
            objects.length == 0 ||
            keys == null ||
            keys.length == 0
        ) {
            return Integer.MIN_VALUE;
        }

        int sum = 0;
        boolean foundAny = false;

        for (String key : keys) {
            if (key == null || key.isEmpty()) {
                continue;
            }

            int bestForKey = Integer.MIN_VALUE;
            for (JsonObject object : objects) {
                if (object == null || !object.has(key)) {
                    continue;
                }
                bestForKey = Math.max(bestForKey, getInt(object, key, 0));
            }

            if (bestForKey != Integer.MIN_VALUE) {
                sum += bestForKey;
                foundAny = true;
            }
        }

        return foundAny ? sum : Integer.MIN_VALUE;
    }

    public static JsonObject getObject(JsonObject object, String key) {
        if (object == null || !object.has(key)) {
            return new JsonObject();
        }

        JsonElement element = object.get(key);
        if (element == null || element.isJsonNull() || !element.isJsonObject()) {
            return new JsonObject();
        }

        return element.getAsJsonObject();
    }

    public static int getInt(JsonObject object, String key, int fallback) {
        if (object == null || !object.has(key)) {
            return fallback;
        }

        JsonElement element = object.get(key);
        if (element == null || element.isJsonNull()) {
            return fallback;
        }

        try {
            if (element.getAsJsonPrimitive().isNumber()) {
                return element.getAsInt();
            }
            return (int) Double.parseDouble(element.getAsString());
        } catch (Exception e) {
            return fallback;
        }
    }

    public static Integer getNullableInt(JsonObject object, String key) {
        if (object == null || !object.has(key)) {
            return null;
        }

        JsonElement element = object.get(key);
        if (element == null || element.isJsonNull()) {
            return null;
        }

        try {
            if (element.getAsJsonPrimitive().isNumber()) {
                return element.getAsInt();
            }
            return (int) Double.parseDouble(element.getAsString());
        } catch (Exception e) {
            return null;
        }
    }

    public static String getString(JsonObject object, String key, String fallback) {
        if (object == null || !object.has(key)) {
            return fallback;
        }

        JsonElement element = object.get(key);
        if (element == null || element.isJsonNull()) {
            return fallback;
        }

        try {
            return element.getAsString();
        } catch (Exception e) {
            return fallback;
        }
    }

    public static boolean getBoolean(JsonObject object, String key, boolean fallback) {
        if (object == null || !object.has(key)) {
            return fallback;
        }

        JsonElement element = object.get(key);
        if (element == null || element.isJsonNull()) {
            return fallback;
        }

        try {
            return element.getAsBoolean();
        } catch (Exception e) {
            return fallback;
        }
    }
}
