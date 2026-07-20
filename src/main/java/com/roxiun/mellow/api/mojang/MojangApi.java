package com.roxiun.mellow.api.mojang;

import com.roxiun.mellow.util.cache.TimedValueCache;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Locale;
import net.minecraft.client.Minecraft;
import net.minecraft.client.network.NetworkPlayerInfo;

public class MojangApi {

    private static final long UUID_CACHE_TTL_MS = 300_000L;

    private final TimedValueCache<String, String> uuidCache =
        new TimedValueCache<>(UUID_CACHE_TTL_MS);
    private final TimedValueCache<String, String> nameCache =
        new TimedValueCache<>(UUID_CACHE_TTL_MS);

    public static class ProfileLookup {

        private final String uuid;
        private final String name;

        public ProfileLookup(String uuid, String name) {
            this.uuid = uuid;
            this.name = name;
        }

        public String getUuid() {
            return uuid;
        }

        public String getName() {
            return name;
        }
    }

    public String fetchUUID(String username) {
        ProfileLookup profile = fetchProfileByName(username);
        if (profile == null || profile.getUuid() == null || profile.getUuid().isEmpty()) {
            return "ERROR";
        }
        return profile.getUuid();
    }

    public ProfileLookup fetchProfileByName(String username) {
        String cacheKey = normalizeUsername(username);
        if (cacheKey.isEmpty()) {
            return null;
        }
        if (uuidCache.containsFresh(cacheKey)) {
            String cached = uuidCache.get(cacheKey);
            if (cached == null || "ERROR".equals(cached)) {
                return null;
            }
            return new ProfileLookup(cached, nameCache.get(cacheKey));
        }

        HttpURLConnection connection = null;
        try {
            String urlString =
                "https://api.minecraftservices.com/minecraft/profile/lookup/name/" +
                username;
            connection = (HttpURLConnection) new URL(
                urlString
            ).openConnection();
            connection.setRequestMethod("GET");

            int responseCode = connection.getResponseCode();
            if (responseCode == 200) {
                BufferedReader in = new BufferedReader(
                    new InputStreamReader(connection.getInputStream())
                );
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = in.readLine()) != null) response.append(line);
                in.close();
                ProfileLookup profile = extractProfile(response.toString());
                if (profile != null && profile.getUuid() != null) {
                    cacheProfile(cacheKey, profile.getUuid(), profile.getName());
                    return profile;
                }
                cacheProfile(cacheKey, "ERROR", null);
                return null;
            }

            if (responseCode == 404) {
                cacheProfile(cacheKey, "ERROR", null);
                return null;
            }

            if (responseCode == 429) {
                // Rate limited, fallback to minetools
                HttpURLConnection minetoolsConnection = null;
                try {
                    urlString = "https://api.minetools.eu/uuid/" + username;
                    minetoolsConnection = (HttpURLConnection) new URL(
                        urlString
                    ).openConnection();
                    minetoolsConnection.setRequestMethod("GET");

                    BufferedReader in = new BufferedReader(
                        new InputStreamReader(
                            minetoolsConnection.getInputStream()
                        )
                    );
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = in.readLine()) != null) response.append(
                        line
                    );
                    in.close();

                    if (
                        response.toString().contains("\"id\": null")
                    ) {
                        cacheProfile(cacheKey, "ERROR", null);
                        return null;
                    }
                    String[] parts = response.toString().split("\"id\":\"");
                    if (parts.length > 1) {
                        String uuid = parts[1].split("\"")[0];
                        cacheProfile(cacheKey, uuid, null);
                        return new ProfileLookup(uuid, null);
                    } else {
                        return null;
                    }
                } finally {
                    if (minetoolsConnection != null) {
                        minetoolsConnection.disconnect();
                    }
                }
            }
        } catch (Exception ignored) {} finally {
            if (connection != null) {
                connection.disconnect();
            }
        }

        return null;
    }

    private ProfileLookup extractProfile(String response) {
        if (response.contains("Couldn't")) {
            return null;
        }

        String uuid = extractJsonValue(response, "id");
        if (uuid == null || uuid.isEmpty()) {
            return null;
        }

        String name = extractJsonValue(response, "name");
        return new ProfileLookup(uuid, name);
    }

    private String extractJsonValue(String json, String key) {
        String token = "\"" + key + "\"";
        int keyIndex = json.indexOf(token);
        if (keyIndex < 0) {
            return null;
        }

        int colonIndex = json.indexOf(':', keyIndex + token.length());
        if (colonIndex < 0) {
            return null;
        }

        int firstQuote = json.indexOf('"', colonIndex + 1);
        if (firstQuote < 0) {
            return null;
        }

        int secondQuote = json.indexOf('"', firstQuote + 1);
        if (secondQuote < 0) {
            return null;
        }

        return json.substring(firstQuote + 1, secondQuote);
    }

    public String getUUIDFromName(String playerName) {
        for (NetworkPlayerInfo info : Minecraft.getMinecraft()
            .getNetHandler()
            .getPlayerInfoMap()) {
            if (info.getGameProfile().getName().equalsIgnoreCase(playerName)) {
                return String.valueOf(info.getGameProfile().getId());
            }
        }
        return null; // Player not found (probably not in tab list)
    }

    public void clearCache() {
        uuidCache.clear();
        nameCache.clear();
    }

    public void clearPlayer(String username) {
        String cacheKey = normalizeUsername(username);
        if (cacheKey.isEmpty()) {
            return;
        }
        uuidCache.remove(cacheKey);
        nameCache.remove(cacheKey);
    }

    private String cacheProfile(String cacheKey, String uuid, String name) {
        String resolved = uuid == null || uuid.isEmpty() ? "ERROR" : uuid;
        uuidCache.put(cacheKey, resolved);
        if (name == null || name.trim().isEmpty()) {
            nameCache.remove(cacheKey);
        } else {
            nameCache.put(cacheKey, name.trim());
        }
        return resolved;
    }

    private String normalizeUsername(String username) {
        return username == null ? "" : username.trim().toLowerCase(Locale.ROOT);
    }
}
