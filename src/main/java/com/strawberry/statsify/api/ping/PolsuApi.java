package com.strawberry.statsify.api.ping;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.strawberry.statsify.Statsify;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.util.ChatComponentText;
import org.apache.commons.lang3.tuple.Pair;

public class PolsuApi {

    private final Map<String, Pair<Integer, Long>> pingCache =
        new ConcurrentHashMap<>();

    // Prevent duplicate fetches for the same player
    private final Set<String> fetchInProgress = ConcurrentHashMap.newKeySet();

    private static final long CACHE_DURATION_MS = 7_200_000; // 2 hours

    private final Minecraft mc = Minecraft.getMinecraft();

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

    /**
     * Returns true if fetch started, false if it was blocked
     * (already in progress).
     */
    public boolean tryStartFetch(String uuid) {
        return fetchInProgress.add(uuid);
    }

    public void finishFetch(String uuid) {
        fetchInProgress.remove(uuid);
    }

    public int fetchPingBlocking(String uuid) {
        try {
            String apiKey = Statsify.config.polsuApiKey;
            if (apiKey == null || apiKey.isEmpty()) return -1;

            URL url = new URL("https://api.polsu.xyz/polsu/ping?uuid=" + uuid);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("API-Key", apiKey);
            conn.setRequestProperty("User-Agent", "Statsify");
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);

            if (conn.getResponseCode() == HttpURLConnection.HTTP_OK) {
                BufferedReader in = new BufferedReader(
                    new InputStreamReader(conn.getInputStream())
                );
                StringBuilder response = new StringBuilder();
                String line;

                while ((line = in.readLine()) != null) response.append(line);

                in.close();

                JsonObject json = new JsonParser()
                    .parse(response.toString())
                    .getAsJsonObject();

                if (json.has("success") && json.get("success").getAsBoolean()) {
                    JsonObject stats = json
                        .getAsJsonObject("data")
                        .getAsJsonObject("stats");
                    int ping = stats.get("avg").getAsInt();

                    updateCache(uuid, ping);
                    return ping;
                }
            }
        } catch (Exception ignored) {}

        return -1;
    }
}
