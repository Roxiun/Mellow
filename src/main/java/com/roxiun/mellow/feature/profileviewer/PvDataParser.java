package com.roxiun.mellow.feature.profileviewer;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.roxiun.mellow.api.hypixel.provider.model.ProviderId;
import com.roxiun.mellow.feature.profileviewer.model.PvSourceData;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public final class PvDataParser {

    private PvDataParser() {}

    public static PvSourceData parse(String rawJson, ProviderId providerId) {
        if (rawJson == null || rawJson.trim().isEmpty()) {
            return PvSourceData.empty();
        }

        try {
            JsonObject root = new JsonParser().parse(rawJson).getAsJsonObject();
            JsonObject player = getPlayerObject(root, providerId);
            if (player == null) {
                return PvSourceData.empty();
            }

            JsonObject stats = getObject(player, "stats");
            JsonObject bedwars = getObject(stats, "Bedwars");
            JsonObject achievements = getObject(player, "achievements");
            JsonObject giftingMeta = getObject(player, "giftingMeta");
            JsonObject socialMedia = getObject(player, "socialMedia");
            JsonObject socialLinks = getObject(socialMedia, "links");

            Map<String, Integer> bedwarsStats = new HashMap<>();
            for (Map.Entry<String, JsonElement> entry : bedwars.entrySet()) {
                String key = entry.getKey();
                int value = safeInt(entry.getValue(), 0);
                bedwarsStats.put(key, value);
            }

            int bedwarsLevel = getInt(achievements, "bedwars_level", 0);
            int networkLevel = (int) Math.floor(
                getLevelFromNetworkExp(getDouble(player, "networkExp", 0.0))
            );
            int karma = getInt(player, "karma", 0);
            int gifted = getInt(giftingMeta, "ranksGiven", 0);
            int achievementPoints = getInt(player, "achievementPoints", 0);
            int tokens = getInt(bedwars, "coins", getInt(player, "coins", 0));
            int bedwarsExperience = getInt(bedwars, "Experience", 0);
            int slumberTickets = getNestedInt(
                bedwars,
                "slumber",
                "total_tickets_earned",
                getInt(bedwars, "slumber.total_tickets_earned", 0)
            );

            Map<String, String> socials = new HashMap<>();
            for (String social : new String[] {
                "TWITTER",
                "YOUTUBE",
                "DISCORD",
                "HYPIXEL",
                "INSTAGRAM",
                "TWITCH",
                "TIKTOK",
            }) {
                String value = getString(socialLinks, social, "");
                if (!value.isEmpty() && !"0".equals(value)) {
                    socials.put(social.toUpperCase(Locale.ROOT), value);
                }
            }

            return new PvSourceData(
                bedwarsStats,
                socials,
                bedwarsLevel,
                networkLevel,
                karma,
                gifted,
                achievementPoints,
                tokens,
                slumberTickets,
                bedwarsExperience,
                true
            );
        } catch (Exception ignored) {
            return PvSourceData.empty();
        }
    }


    private static JsonObject getPlayerObject(JsonObject root, ProviderId providerId) {
        if (root == null) {
            return null;
        }

        if (providerId == ProviderId.NADESHIKO) {
            JsonObject maybePlayer = getObject(root, "player");
            if (!maybePlayer.entrySet().isEmpty()) {
                return maybePlayer;
            }
            return root;
        }

        if (root.has("success") && !getBoolean(root, "success", false)) {
            return null;
        }
        JsonObject player = getObject(root, "player");
        return player.entrySet().isEmpty() ? null : player;
    }

    private static JsonObject getObject(JsonObject object, String key) {
        if (object == null || !object.has(key)) {
            return new JsonObject();
        }

        JsonElement element = object.get(key);
        if (element == null || element.isJsonNull() || !element.isJsonObject()) {
            return new JsonObject();
        }
        return element.getAsJsonObject();
    }

    private static int getInt(JsonObject object, String key, int fallback) {
        if (object == null || !object.has(key)) {
            return fallback;
        }
        return safeInt(object.get(key), fallback);
    }

    private static int safeInt(JsonElement element, int fallback) {
        if (element == null || element.isJsonNull()) {
            return fallback;
        }

        try {
            if (element.getAsJsonPrimitive().isNumber()) {
                return element.getAsInt();
            }
            return (int) Double.parseDouble(element.getAsString());
        } catch (Exception ignored) {
            return fallback;
        }
    }

    private static double getDouble(JsonObject object, String key, double fallback) {
        if (object == null || !object.has(key)) {
            return fallback;
        }

        JsonElement element = object.get(key);
        if (element == null || element.isJsonNull()) {
            return fallback;
        }

        try {
            if (element.getAsJsonPrimitive().isNumber()) {
                return element.getAsDouble();
            }
            return Double.parseDouble(element.getAsString());
        } catch (Exception ignored) {
            return fallback;
        }
    }

    private static String getString(JsonObject object, String key, String fallback) {
        if (object == null || !object.has(key)) {
            return fallback;
        }

        JsonElement element = object.get(key);
        if (element == null || element.isJsonNull()) {
            return fallback;
        }

        try {
            return element.getAsString();
        } catch (Exception ignored) {
            return fallback;
        }
    }

    private static boolean getBoolean(JsonObject object, String key, boolean fallback) {
        if (object == null || !object.has(key)) {
            return fallback;
        }

        JsonElement element = object.get(key);
        if (element == null || element.isJsonNull()) {
            return fallback;
        }

        try {
            return element.getAsBoolean();
        } catch (Exception ignored) {
            return fallback;
        }
    }

    private static int getNestedInt(
        JsonObject object,
        String parent,
        String key,
        int fallback
    ) {
        JsonObject parentObj = getObject(object, parent);
        int nestedValue = getInt(parentObj, key, Integer.MIN_VALUE);
        if (nestedValue != Integer.MIN_VALUE) {
            return nestedValue;
        }
        return fallback;
    }

    private static double getLevelFromNetworkExp(double exp) {
        if (exp < 0) {
            return 1.0;
        }

        double growth = 2500.0;
        double base = 10000.0;
        double reversePrefix = -(base - 0.5 * growth) / growth;
        double reverseConst = reversePrefix * reversePrefix;
        double growthDivides2 = 2.0 / growth;

        return Math.floor(
            1.0 + reversePrefix + Math.sqrt(reverseConst + growthDivides2 * exp)
        );
    }
}
