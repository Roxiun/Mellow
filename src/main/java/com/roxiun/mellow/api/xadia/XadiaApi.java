package com.roxiun.mellow.api.xadia;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.roxiun.mellow.Mellow;
import com.roxiun.mellow.util.cache.TimedValueCache;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

public class XadiaApi {

    private static final String PLAYER_TAGS_ENDPOINT =
        "https://xadia.sniped.me/v1/players";
    private static final long TAG_CACHE_TTL_MS = 120_000L;

    private final TimedValueCache<String, List<XadiaTag>> tagCache =
        new TimedValueCache<>(TAG_CACHE_TTL_MS);

    public List<XadiaTag> fetchXadiaTags(
        String uuid,
        String playerName,
        String xadiaKey,
        boolean verifiedOnly
    ) throws IOException {
        String apiKey = normalizeApiKey(xadiaKey);
        if (apiKey.isEmpty()) {
            throw new IOException("A Xadia API key is required.");
        }

        String identifier = selectIdentifier(uuid, playerName);
        if (identifier.isEmpty()) {
            throw new IOException("A player UUID or username is required.");
        }

        String cacheKey = buildTagCacheKey(identifier, apiKey) + "|" + verifiedOnly;
        if (tagCache.containsFresh(cacheKey)) {
            return copyTags(tagCache.get(cacheKey));
        }

        URL url = new URL(PLAYER_TAGS_ENDPOINT);
        HttpURLConnection connection = openConnection(url);
        connection.setRequestMethod("POST");
        connection.setDoOutput(true);
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setRequestProperty("X-API-Key", apiKey);
        connection.setRequestProperty("Accept", "application/json");
        connection.setRequestProperty(
            "User-Agent",
            Mellow.NAME + "/" + Mellow.VERSION
        );
        connection.setConnectTimeout(5000);
        connection.setReadTimeout(5000);

        List<XadiaTag> tags;
        try {
            JsonArray players = new JsonArray();
            players.add(new com.google.gson.JsonPrimitive(identifier));
            JsonObject request = new JsonObject();
            request.add("players", players);
            request.addProperty("verified_only", verifiedOnly);
            try (java.io.OutputStream output = connection.getOutputStream()) {
                output.write(request.toString().getBytes(StandardCharsets.UTF_8));
            }
            int responseCode = connection.getResponseCode();
            if (responseCode != HttpURLConnection.HTTP_OK) {
                throw buildHttpException(connection, responseCode);
            }

            try (InputStream input = connection.getInputStream()) {
                tags = parseTags(readBody(input));
            }
        } finally {
            connection.disconnect();
        }
        tagCache.put(cacheKey, copyTags(tags));
        return copyTags(tags);
    }

    protected HttpURLConnection openConnection(URL url) throws IOException {
        return (HttpURLConnection) url.openConnection();
    }

    private List<XadiaTag> parseTags(String response) throws IOException {
        try {
            JsonObject json = new JsonParser()
                .parse(response)
                .getAsJsonObject();
            JsonArray results = json.getAsJsonArray("results");
            if (results == null || results.size() != 1) {
                throw new IOException("Xadia response did not contain the requested player.");
            }
            JsonObject player = results.get(0).getAsJsonObject();
            if (!player.get("found").getAsBoolean()) {
                return new ArrayList<>();
            }
            JsonArray tagsArray = player.getAsJsonArray("tags");
            if (tagsArray == null) {
                throw new IOException("Xadia response did not contain tags.");
            }
            List<XadiaTag> tags = new ArrayList<>();
            for (JsonElement element : tagsArray) {
                JsonObject tag = element.getAsJsonObject();
                JsonElement verified = tag.get("verified");
                tags.add(new XadiaTag(
                    tag.get("type").getAsString(),
                    tag.get("label").getAsString(),
                    getNullableString(tag, "reason"),
                    verified == null || verified.isJsonNull() ? null : verified.getAsBoolean()
                ));
            }
            return tags;
        } catch (IOException e) {
            throw e;
        } catch (Exception e) {
            throw new IOException("Unable to parse Xadia tag response.", e);
        }
    }

    private String getNullableString(JsonObject object, String field) {
        JsonElement value = object.get(field);
        return value == null || value.isJsonNull()
            ? null
            : value.getAsString();
    }

    private IOException buildHttpException(
        HttpURLConnection connection,
        int responseCode
    ) {
        String detail = "";
        InputStream errorStream = connection.getErrorStream();
        if (errorStream != null) {
            try (InputStream input = errorStream) {
                JsonObject error = new JsonParser()
                    .parse(readBody(input))
                    .getAsJsonObject();
                if (error.has("error")) {
                    detail = ": " + error.get("error").getAsString();
                }
            } catch (Exception ignored) {}
        }
        return new IOException(
            "Xadia API request failed with response code " +
            responseCode +
            detail
        );
    }

    private String readBody(InputStream input) throws IOException {
        try (
            BufferedReader reader = new BufferedReader(
                new InputStreamReader(input, StandardCharsets.UTF_8)
            )
        ) {
            return reader.lines().collect(Collectors.joining());
        }
    }

    public void clearCache() {
        tagCache.clear();
    }

    public void clearPlayer(String uuid, String playerName) {
        String normalizedUuid = normalizeIdentifier(uuid);
        String normalizedName = normalizeIdentifier(playerName);
        tagCache.removeMatching(key ->
            matchesCachePrefix(key, normalizedUuid) ||
            matchesCachePrefix(key, normalizedName)
        );
    }

    private String selectIdentifier(String uuid, String playerName) {
        String identifier = normalizeIdentifier(uuid);
        if (identifier.isEmpty() || "error".equals(identifier)) {
            identifier = normalizeIdentifier(playerName);
        }
        return identifier;
    }

    private String buildTagCacheKey(String identifier, String apiKey) {
        return normalizeIdentifier(identifier) + "|" + apiKey;
    }

    private String normalizeApiKey(String apiKey) {
        return apiKey == null ? "" : apiKey.trim();
    }

    private String normalizeIdentifier(String value) {
        String normalized = value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
        if (normalized.matches("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}")) {
            return normalized.replace("-", "");
        }
        return normalized;
    }

    private boolean matchesCachePrefix(String key, String prefix) {
        return (
            key != null &&
            prefix != null &&
            !prefix.isEmpty() &&
            key.startsWith(prefix + "|")
        );
    }

    private List<XadiaTag> copyTags(List<XadiaTag> tags) {
        return tags == null ? new ArrayList<>() : new ArrayList<>(tags);
    }
}
