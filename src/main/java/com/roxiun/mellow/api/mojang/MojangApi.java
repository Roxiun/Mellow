package com.roxiun.mellow.api.mojang;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.roxiun.mellow.Mellow;
import com.roxiun.mellow.api.seraph.SeraphRequestLimiter;
import com.roxiun.mellow.util.UUIDUtils;
import com.roxiun.mellow.util.cache.TimedValueCache;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;
import net.minecraft.client.Minecraft;
import net.minecraft.client.network.NetworkPlayerInfo;

public class MojangApi {

    private static final long UUID_CACHE_TTL_MS = 300_000L;
    private static final long FAILURE_CACHE_TTL_MS = 30_000L;
    private static final String MINECRAFT_PROFILE_URL =
        "https://api.minecraftservices.com/minecraft/profile/lookup/name/";
    private static final String SERAPH_MOJANG_URL =
        "https://mowojang.seraph.si/";
    private static final String MINETOOLS_UUID_URL =
        "https://api.minetools.eu/uuid/";

    private final TimedValueCache<String, String> uuidCache =
        new TimedValueCache<>(UUID_CACHE_TTL_MS);
    private final TimedValueCache<String, Boolean> uuidFailureCache =
        new TimedValueCache<>(FAILURE_CACHE_TTL_MS);
    private final TimedValueCache<String, MojangProfile> seraphMojangCache =
        new TimedValueCache<>(UUID_CACHE_TTL_MS);
    private final TimedValueCache<String, Boolean> seraphMojangFailureCache =
        new TimedValueCache<>(FAILURE_CACHE_TTL_MS);
    private final Map<String, CompletableFuture<String>> uuidLookupsInProgress =
        new ConcurrentHashMap<>();
    private final Map<String, CompletableFuture<MojangProfile>>
        seraphMojangLookupsInProgress = new ConcurrentHashMap<>();
    private final SeraphRequestLimiter seraphRequestLimiter =
        SeraphRequestLimiter.getInstance();

    public String fetchUUID(String username) {
        String cacheKey = normalizeUsername(username);
        if (cacheKey.isEmpty()) {
            return "ERROR";
        }
        if (uuidCache.containsFresh(cacheKey)) {
            String cached = uuidCache.get(cacheKey);
            return cached == null ? "ERROR" : cached;
        }
        if (uuidFailureCache.containsFresh(cacheKey)) {
            return "ERROR";
        }

        CompletableFuture<String> lookup = new CompletableFuture<>();
        CompletableFuture<String> existing = uuidLookupsInProgress.putIfAbsent(
            cacheKey,
            lookup
        );
        if (existing != null) {
            return awaitUuidLookup(existing);
        }

        String result;
        try {
            result = fetchUuidUncached(username, cacheKey);
            lookup.complete(result);
        } catch (Exception ignored) {
            uuidFailureCache.put(cacheKey, true);
            result = "ERROR";
            lookup.complete(result);
        } finally {
            uuidLookupsInProgress.remove(cacheKey, lookup);
        }
        return result;
    }

    private String fetchUuidUncached(String username, String cacheKey) {
        try {
            HttpResult result = executeGetRequest(
                new URL(MINECRAFT_PROFILE_URL + username)
            );
            if (result.statusCode == HttpURLConnection.HTTP_OK) {
                String uuid = extractUuid(result.body);
                if (!uuid.isEmpty()) {
                    return cacheUuid(cacheKey, uuid);
                }
            }
            if (result.statusCode == HttpURLConnection.HTTP_NOT_FOUND) {
                return cacheUuid(cacheKey, "ERROR");
            }
        } catch (Exception ignored) {}

        MojangProfile seraphProfile = fetchSeraphMojang(username);
        if (seraphProfile != null) {
            return cacheUuid(cacheKey, toUndashedUuid(seraphProfile.uuid));
        }

        try {
            HttpResult result = executeGetRequest(
                new URL(MINETOOLS_UUID_URL + username)
            );
            if (result.statusCode == HttpURLConnection.HTTP_OK) {
                String uuid = extractUuid(result.body);
                if (!uuid.isEmpty()) {
                    return cacheUuid(cacheKey, uuid);
                }
            }
        } catch (Exception ignored) {}

        uuidFailureCache.put(cacheKey, true);
        return "ERROR";
    }

    public MojangProfile fetchSeraphMojang(String nameOrId) {
        if (!com.roxiun.mellow.api.seraph.SeraphAvailability.isEnabled()) return null;
        if (nameOrId == null || nameOrId.trim().isEmpty()) {
            return null;
        }

        String cacheKey = nameOrId.trim().toLowerCase(Locale.ROOT);
        if (seraphMojangCache.containsFresh(cacheKey)) {
            return seraphMojangCache.get(cacheKey);
        }
        if (seraphMojangFailureCache.containsFresh(cacheKey)) {
            return null;
        }

        CompletableFuture<MojangProfile> lookup = new CompletableFuture<>();
        CompletableFuture<MojangProfile> existing =
            seraphMojangLookupsInProgress.putIfAbsent(cacheKey, lookup);
        if (existing != null) {
            return awaitSeraphMojangLookup(existing);
        }

        MojangProfile profile = null;
        try {
            if (!seraphRequestLimiter.tryAcquire()) {
                seraphMojangFailureCache.put(cacheKey, true);
                return null;
            }

            HttpResult result = executeGetRequest(
                new URL(SERAPH_MOJANG_URL + nameOrId.trim())
            );
            seraphRequestLimiter.recordResponse(
                result.statusCode,
                result.retryAfter
            );
            if (result.statusCode != HttpURLConnection.HTTP_OK) {
                seraphMojangFailureCache.put(cacheKey, true);
                return null;
            }

            JsonObject json = new JsonParser()
                .parse(result.body)
                .getAsJsonObject();
            String name = getJsonString(json, "name");
            String uuid = extractUuid(json);
            if (name.isEmpty() || uuid.isEmpty()) {
                seraphMojangFailureCache.put(cacheKey, true);
                return null;
            }
            profile = new MojangProfile(name, UUIDUtils.fromString(uuid));
            seraphMojangFailureCache.remove(cacheKey);
            seraphMojangCache.put(cacheKey, profile);
            return profile;
        } catch (Exception ignored) {
            seraphMojangFailureCache.put(cacheKey, true);
            return null;
        } finally {
            lookup.complete(profile);
            seraphMojangLookupsInProgress.remove(cacheKey, lookup);
        }
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
        uuidFailureCache.clear();
        seraphMojangCache.clear();
        seraphMojangFailureCache.clear();
    }

    public void clearPlayer(String username) {
        String cacheKey = normalizeUsername(username);
        if (cacheKey.isEmpty()) {
            return;
        }
        uuidCache.remove(cacheKey);
        uuidFailureCache.remove(cacheKey);
        seraphMojangCache.remove(cacheKey);
        seraphMojangFailureCache.remove(cacheKey);
    }

    private String cacheUuid(String cacheKey, String uuid) {
        String resolved = uuid == null || uuid.isEmpty() ? "ERROR" : uuid;
        uuidFailureCache.remove(cacheKey);
        uuidCache.put(cacheKey, resolved);
        return resolved;
    }

    private String awaitUuidLookup(CompletableFuture<String> lookup) {
        try {
            String result = lookup.get();
            return result == null || result.isEmpty() ? "ERROR" : result;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return "ERROR";
        } catch (ExecutionException e) {
            return "ERROR";
        }
    }

    private MojangProfile awaitSeraphMojangLookup(
        CompletableFuture<MojangProfile> lookup
    ) {
        try {
            return lookup.get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return null;
        } catch (ExecutionException e) {
            return null;
        }
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
