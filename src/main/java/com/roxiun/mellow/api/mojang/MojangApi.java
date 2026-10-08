package com.roxiun.mellow.api.mojang;

import com.roxiun.mellow.util.cache.RequestCache;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.roxiun.mellow.Mellow;
import com.roxiun.mellow.api.mojang.MowojangRequestLimiter;
import com.roxiun.mellow.util.UUIDUtils;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.UUID;
import java.util.stream.Collectors;
import net.minecraft.client.Minecraft;
import net.minecraft.client.network.NetworkPlayerInfo;

public class MojangApi {

    private static final long UUID_CACHE_TTL_MS = 300_000L;
    private static final long FAILURE_CACHE_TTL_MS = 30_000L;
    private static final String MINECRAFT_PROFILE_URL =
        "https://api.minecraftservices.com/minecraft/profile/lookup/name/";
    private static final String MOWOJANG_URL =
        "https://mowojang.seraph.si/";
    private static final String ALTERNATIVE_MOWOJANG_URL =
        "https://mowojang.matdoes.dev/";
    private static final String MINETOOLS_UUID_URL =
        "https://api.minetools.eu/uuid/";

    private final RequestCache<String, String> uuidCache =
        new RequestCache<>(4096, UUID_CACHE_TTL_MS, FAILURE_CACHE_TTL_MS, value -> value != null && !"ERROR".equals(value));
    private final RequestCache<String, MojangProfile> mowojangCache =
        new RequestCache<>(4096, UUID_CACHE_TTL_MS, FAILURE_CACHE_TTL_MS, value -> value != null);
    private final MowojangRequestLimiter requestLimiter = MowojangRequestLimiter.getInstance();

    public String fetchUUID(String username) {
        String key = normalizeUsername(username);
        if (key.isEmpty()) return "ERROR";
        return uuidCache.get(key, () -> fetchUuidUncached(username.trim()));
    }

    private String fetchUuidUncached(String username) {
        MojangProfile mowojangProfile = fetchMowojang(username);
        if (mowojangProfile != null) {
            return toUndashedUuid(mowojangProfile.uuid);
        }

        try {
            HttpResult result = executeGetRequest(
                new URL(ALTERNATIVE_MOWOJANG_URL + username)
            );
            if (result.statusCode == HttpURLConnection.HTTP_OK) {
                String uuid = extractUuid(result.body);
                if (!uuid.isEmpty()) {
                    return uuid;
                }
            }
        } catch (Exception ignored) {}

        try {
            HttpResult result = executeGetRequest(
                new URL(MINECRAFT_PROFILE_URL + username)
            );
            if (result.statusCode == HttpURLConnection.HTTP_OK) {
                String uuid = extractUuid(result.body);
                if (!uuid.isEmpty()) {
                    return uuid;
                }
            }
            if (result.statusCode == HttpURLConnection.HTTP_NOT_FOUND) {
                return "ERROR";
            }
        } catch (Exception ignored) {}

        try {
            HttpResult result = executeGetRequest(
                new URL(MINETOOLS_UUID_URL + username)
            );
            if (result.statusCode == HttpURLConnection.HTTP_OK) {
                String uuid = extractUuid(result.body);
                if (!uuid.isEmpty()) {
                    return uuid;
                }
            }
        } catch (Exception ignored) {}

        return "ERROR";
    }

    public MojangProfile fetchMowojang(String nameOrId) {
        String key = normalizeUsername(nameOrId);
        if (key.isEmpty()) return null;
        return mowojangCache.get(key, () -> {
            try {
                if (!requestLimiter.tryAcquire()) return null;
                HttpResult result = executeGetRequest(new URL(MOWOJANG_URL + nameOrId.trim()));
                requestLimiter.recordResponse(result.statusCode, result.retryAfter);
                if (result.statusCode != HttpURLConnection.HTTP_OK) return null;
                JsonObject json = new JsonParser().parse(result.body).getAsJsonObject();
                String name = getJsonString(json, "name"), uuid = extractUuid(json);
                return name.isEmpty() || uuid.isEmpty() ? null : new MojangProfile(name, UUIDUtils.fromString(uuid));
            } catch (Exception ignored) { return null; }
        });
    }

    private String extractUuid(String response) {
        if (response == null || response.trim().isEmpty()) {
            return "";
        }
        try {
            return extractUuid(
                new JsonParser().parse(response).getAsJsonObject()
            );
        } catch (Exception ignored) {
            return "";
        }
    }

    private String extractUuid(JsonObject json) {
        String uuid = getJsonString(json, "id");
        if (uuid.isEmpty()) {
            uuid = getJsonString(json, "uuid");
        }
        try {
            return toUndashedUuid(UUIDUtils.fromString(uuid));
        } catch (Exception ignored) {
            return "";
        }
    }

    private String getJsonString(JsonObject json, String field) {
        if (json == null || !json.has(field) || json.get(field).isJsonNull()) {
            return "";
        }
        return json.get(field).getAsString().trim();
    }

    private String toUndashedUuid(UUID uuid) {
        return uuid == null ? "" : uuid.toString().replace("-", "");
    }

    protected HttpURLConnection openConnection(URL url) throws IOException {
        return (HttpURLConnection) url.openConnection();
    }

    private HttpResult executeGetRequest(URL url) throws IOException {
        HttpURLConnection connection = openConnection(url);
        try {
            connection.setRequestMethod("GET");
            connection.setRequestProperty(
                "User-Agent",
                Mellow.NAME + "/" + Mellow.VERSION
            );
            connection.setRequestProperty("Accept", "application/json");
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);

            int responseCode = connection.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                BufferedReader reader = new BufferedReader(
                    new InputStreamReader(
                        connection.getInputStream(),
                        StandardCharsets.UTF_8
                    )
                );
                try {
                    return new HttpResult(
                        responseCode,
                        reader.lines().collect(Collectors.joining()),
                        connection.getHeaderField("Retry-After")
                    );
                } finally {
                    reader.close();
                }
            }
            return new HttpResult(
                responseCode,
                "",
                connection.getHeaderField("Retry-After")
            );
        } finally {
            connection.disconnect();
        }
    }

    public String getUUIDFromName(String playerName) {
        if (Minecraft.getMinecraft().getNetHandler() == null) return null;
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
        mowojangCache.clear();
    }

    public void clearPlayer(String username) {
        String key = normalizeUsername(username);
        uuidCache.removeMatching(key::equals);
        mowojangCache.removeMatching(key::equals);
    }

    private String normalizeUsername(String username) {
        return username == null ? "" : username.trim().toLowerCase(Locale.ROOT);
    }

    private static final class HttpResult {

        private final int statusCode;
        private final String body;
        private final String retryAfter;

        private HttpResult(int statusCode, String body, String retryAfter) {
            this.statusCode = statusCode;
            this.body = body;
            this.retryAfter = retryAfter;
        }
    }

    public static final class MojangProfile {

        private final String name;
        private final UUID uuid;

        private MojangProfile(String name, UUID uuid) {
            this.name = name;
            this.uuid = uuid;
        }

        public String getName() {
            return name;
        }

        public UUID getUuid() {
            return uuid;
        }
    }
}
