package com.roxiun.mellow.api.hypixel;

import com.google.gson.*;
import com.roxiun.mellow.api.model.*;
import com.roxiun.mellow.api.hypixel.provider.model.ProviderId;
import static com.roxiun.mellow.stats.JsonStats.*;

/** A provider response decoded once; game parsers do not depend on provider identity. */
public final class HypixelPlayerData {
    private final JsonObject player;
    private final String name;
    private final String formattedName;

    private HypixelPlayerData(JsonObject player, String name, String formattedName) {
        this.player = player;
        this.name = name;
        this.formattedName = formattedName;
    }
    public JsonObject player() { return player; }
    public String name() { return name; }
    public String formattedName() { return formattedName; }

    /** Validate the transport envelope before placing a successful response in the shared cache. */
    public static ProviderResult<JsonObject> parseResponse(String json, ProviderId providerId) {
        try {
            JsonObject root = new JsonParser().parse(json).getAsJsonObject();
            if (providerId != ProviderId.NADESHIKO) {
                if (!root.has("success") || !root.get("success").getAsBoolean())
                    return ProviderResult.failure(FetchFailureReason.PROVIDER_ERROR,
                        root.has("cause") ? root.get("cause").getAsString() : "Provider returned success=false");
                if (!root.has("player") || root.get("player").isJsonNull())
                    return ProviderResult.failure(FetchFailureReason.NO_PLAYER_DATA, "No player data");
            }
            return ProviderResult.success(root);
        } catch (RuntimeException error) {
            return ProviderResult.failure(FetchFailureReason.PARSE_ERROR, error.getMessage());
        }
    }

    public static ProviderResult<HypixelPlayerData> decode(String json, ProviderId providerId) {
        try {
            return decode(new JsonParser().parse(json).getAsJsonObject(), providerId);
        } catch (Exception e) {
            return ProviderResult.failure(FetchFailureReason.PARSE_ERROR, e.getMessage());
        }
    }
    public static ProviderResult<HypixelPlayerData> decode(JsonObject rootObject, ProviderId providerId) {
        try {
            JsonObject playerObject = getPlayerObject(rootObject, providerId);
            if (isMissingPlayerObject(playerObject)) {
                return ProviderResult.failure(FetchFailureReason.NO_PLAYER_DATA, "Provider returned no player data");
            }
            String name = getString(
                playerObject,
                "displayname",
                getString(rootObject, "name", "[]")
            );
            if (providerId == ProviderId.NADESHIKO) {
                JsonObject profile = getObject(rootObject, "profile");
                name =
                    getString(
                        profile,
                        "hypixel_displayname",
                        getString(rootObject, "name", name)
                    );
            }
            String formattedName = getFormattedNameWithRank(
                rootObject,
                playerObject,
                providerId,
                name
            );

            return ProviderResult.success(new HypixelPlayerData(playerObject, name, formattedName));
        } catch (Exception e) {
            return ProviderResult.failure(FetchFailureReason.PARSE_ERROR, e.getMessage());
        }
    }
    private static String getFormattedNameWithRank(
        JsonObject rootObject,
        JsonObject playerObject,
        ProviderId providerId,
        String plainName
    ) {
        if (plainName == null || plainName.isEmpty() || "[]".equals(plainName)) {
            plainName = getString(rootObject, "name", "Unknown");
        }

        if (providerId == ProviderId.NADESHIKO) {
            JsonObject profile = getObject(rootObject, "profile");
            String tagged = getString(profile, "tagged_name", "");
            if (!tagged.isEmpty()) {
                return tagged;
            }

            String tag = getString(profile, "tag", "");
            if (!tag.isEmpty()) {
                return tag + " " + plainName;
            }

            return plainName;
        }

        String prefix = normalizeFormatting(getString(playerObject, "prefix", ""));
        if (!prefix.isEmpty()) {
            return prefix + " " + plainName;
        }

        String staffRank = getString(playerObject, "rank", "");
        if (!staffRank.isEmpty() && !"NONE".equalsIgnoreCase(staffRank)) {
            return formatStaffRank(staffRank) + " " + plainName;
        }

        String newPackageRank = getString(playerObject, "newPackageRank", "");
        String packageRank = getString(playerObject, "packageRank", "");
        String monthlyRank = getString(playerObject, "monthlyPackageRank", "");

        String plusColor = colorForHypixelRank(
            getString(playerObject, "rankPlusColor", "RED")
        );
        String monthlyColor = colorForHypixelRank(
            getString(playerObject, "monthlyRankColor", "GOLD")
        );

        if ("SUPERSTAR".equalsIgnoreCase(monthlyRank)) {
            return monthlyColor + "[MVP" + plusColor + "++" + monthlyColor + "] " + plainName;
        }
        if (
            "MVP_PLUS".equalsIgnoreCase(newPackageRank) ||
            "MVP_PLUS".equalsIgnoreCase(packageRank)
        ) {
            return "§b[MVP" + plusColor + "+§b] " + plainName;
        }
        if (
            "MVP".equalsIgnoreCase(newPackageRank) ||
            "MVP".equalsIgnoreCase(packageRank)
        ) {
            return "§b[MVP] " + plainName;
        }
        if (
            "VIP_PLUS".equalsIgnoreCase(newPackageRank) ||
            "VIP_PLUS".equalsIgnoreCase(packageRank)
        ) {
            return "§a[VIP§6+§a] " + plainName;
        }
        if (
            "VIP".equalsIgnoreCase(newPackageRank) ||
            "VIP".equalsIgnoreCase(packageRank)
        ) {
            return "§a[VIP] " + plainName;
        }

        return plainName;
    }

    private static String formatStaffRank(String rank) {
        if (rank == null || rank.isEmpty()) {
            return "";
        }

        switch (rank.toUpperCase()) {
            case "ADMIN":
                return "§c[ADMIN]";
            case "MODERATOR":
                return "§2[MOD]";
            case "HELPER":
                return "§9[HELPER]";
            case "YOUTUBER":
                return "§c[§fYOUTUBE§c]";
            case "GAME_MASTER":
                return "§2[GM]";
            case "OWNER":
                return "§c[OWNER]";
            case "NORMAL":
            case "NONE":
                return "";
            default:
                return "§f[" + rank + "]";
        }
    }

    private static String colorForHypixelRank(String color) {
        if (color == null || color.isEmpty()) {
            return "§f";
        }

        switch (color.toUpperCase()) {
            case "BLACK":
                return "§0";
            case "DARK_BLUE":
                return "§1";
            case "DARK_GREEN":
                return "§2";
            case "DARK_AQUA":
                return "§3";
            case "DARK_RED":
                return "§4";
            case "DARK_PURPLE":
                return "§5";
            case "GOLD":
                return "§6";
            case "GRAY":
                return "§7";
            case "DARK_GRAY":
                return "§8";
            case "BLUE":
                return "§9";
            case "GREEN":
                return "§a";
            case "AQUA":
                return "§b";
            case "RED":
                return "§c";
            case "LIGHT_PURPLE":
                return "§d";
            case "YELLOW":
                return "§e";
            case "WHITE":
            default:
                return "§f";
        }
    }

    private static JsonObject getPlayerObject(JsonObject root, ProviderId providerId) {
        if (providerId == ProviderId.NADESHIKO) return root;
        return getBoolean(root, "success", false) ? getObject(root, "player") : null;
    }

    private static boolean isMissingPlayerObject(JsonObject playerObject) {
        return playerObject == null || playerObject.entrySet().isEmpty();
    }
}
