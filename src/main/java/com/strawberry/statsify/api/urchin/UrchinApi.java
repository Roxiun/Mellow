package com.strawberry.statsify.api.urchin;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.strawberry.statsify.api.mojang.MojangApi;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import org.apache.commons.lang3.tuple.Pair;

public class UrchinApi {

    private final Map<String, Pair<Integer, Long>> pingCache =
        new ConcurrentHashMap<>();
    private final Set<String> fetchInProgress = ConcurrentHashMap.newKeySet();
    private static final long CACHE_DURATION_MS = 7_200_000; // 2 hours

    private final MojangApi mojangApi;

    public UrchinApi(MojangApi mojangApi) {
        this.mojangApi = mojangApi;
    }

    public List<UrchinTag> fetchUrchinTags(
        String uuid,
        String playerName,
        String urchinKey
    ) throws IOException {
        try {
            // If the UUID is invalid for any reason, throw an exception to trigger the fallback.
            if (uuid == null || uuid.equals("ERROR") || uuid.isEmpty()) {
                throw new IOException(
                    "Invalid UUID provided, attempting fallback."
                );
            }

            URL url = new URL(
                "https://coral.urchin.ws/api/urchin?uuid=" + uuid
            );
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("User-Agent", "Mozilla/5.0");
            conn.setRequestProperty(
                "Referer",
                "https://coral.urchin.ws/player/" + uuid
            );
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);

            int responseCode = conn.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                BufferedReader in = new BufferedReader(
                    new InputStreamReader(conn.getInputStream())
                );
                String response = in.lines().collect(Collectors.joining());
                in.close();
                return parseTags(response);
            } else if (responseCode == HttpURLConnection.HTTP_NOT_FOUND) {
                return new ArrayList<>(); // Player has no tags, this is a success, so don't fallback.
            } else {
                // For any other error code (e.g., 500), throw to trigger the fallback.
                throw new IOException(
                    "Primary Urchin endpoint failed with code: " + responseCode
                );
            }
        } catch (IOException e) {
            // Fallback to the legacy endpoint with playerName
            try {
                String fallbackUrl = "https://urchin.ws/player/" + playerName;
                if (urchinKey != null && !urchinKey.isEmpty()) {
                    fallbackUrl += "?key=" + urchinKey;
                }
                URL url = new URL(fallbackUrl);
                HttpURLConnection conn =
                    (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setConnectTimeout(5000);
                conn.setReadTimeout(5000);

                int responseCode = conn.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    BufferedReader in = new BufferedReader(
                        new InputStreamReader(conn.getInputStream())
                    );
                    String response = in.lines().collect(Collectors.joining());
                    in.close();
                    return parseTags(response);
                } else if (responseCode == HttpURLConnection.HTTP_NOT_FOUND) {
                    return new ArrayList<>(); // No tags on fallback either.
                } else {
                    throw new IOException(
                        "Urchin fallback API request failed with response code: " +
                            responseCode
                    );
                }
            } catch (IOException fallbackException) {
                // If the fallback also fails, return an empty list.
                return new ArrayList<>();
            }
        }
    }

    private List<UrchinTag> parseTags(String response) {
        try {
            JsonObject json = new JsonParser()
                .parse(response)
                .getAsJsonObject();
            if (json.has("tags")) {
                JsonArray tagsArray = json.getAsJsonArray("tags");
                if (tagsArray.size() > 0) {
                    List<UrchinTag> tags = new ArrayList<>();
                    for (JsonElement tagElement : tagsArray) {
                        JsonObject tagObj = tagElement.getAsJsonObject();
                        String type = tagObj.has("type")
                            ? tagObj.get("type").getAsString()
                            : "unknown";
                        String reason = tagObj.has("reason")
                            ? tagObj.get("reason").getAsString()
                            : "No reason provided.";
                        tags.add(new UrchinTag(type, reason));
                    }
                    return tags;
                }
            }
        } catch (Exception e) {
            // If parsing fails, return empty list
        }
        return new ArrayList<>();
    }

    // Other methods like ping cache remain unchanged
    public int getCachedPing(String uuid) {
        Pair<Integer, Long> cached = pingCache.get(uuid);
        if (
            cached != null &&
            System.currentTimeMillis() - cached.getRight() < CACHE_DURATION_MS
        ) {
            return cached.getLeft();
        }
        return -1;
    }

    public void updateCache(String uuid, int ping) {
        pingCache.put(uuid, Pair.of(ping, System.currentTimeMillis()));
    }

    public boolean tryStartFetch(String uuid) {
        return fetchInProgress.add(uuid);
    }

    public void finishFetch(String uuid) {
        fetchInProgress.remove(uuid);
    }

    public int fetchPingBlocking(String uuid) {
        try {
            URL url = new URL("https://coral.urchin.ws/api/ping?uuid=" + uuid);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("User-Agent", "Mozilla/5.0");
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);

            if (conn.getResponseCode() == HttpURLConnection.HTTP_OK) {
                BufferedReader in = new BufferedReader(
                    new InputStreamReader(conn.getInputStream())
                );
                String response = in.lines().collect(Collectors.joining());
                in.close();

                JsonObject json = new JsonParser()
                    .parse(response)
                    .getAsJsonObject();
                if (json.has("success") && json.get("success").getAsBoolean()) {
                    JsonArray data = json.getAsJsonArray("data");
                    if (data.size() > 0) {
                        JsonObject latest = data.get(0).getAsJsonObject();
                        int ping = latest.get("avg").getAsInt();
                        updateCache(uuid, ping);
                        return ping;
                    }
                }
            }
        } catch (Exception ignored) {}
        return -1;
    }
}
