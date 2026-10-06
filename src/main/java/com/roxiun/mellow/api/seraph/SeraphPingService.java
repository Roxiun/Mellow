package com.roxiun.mellow.api.seraph;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.roxiun.mellow.Mellow;
import com.roxiun.mellow.core.async.AsyncExecutor;
import com.roxiun.mellow.core.async.MainThreadDispatcher;
import com.roxiun.mellow.util.ChatUtils;
import com.roxiun.mellow.util.ping.SessionPingFetchGate;
import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

public class SeraphPingService {

    private static final String PING_URL =
        "https://api.seraph.si/private-access/ping";
    private static final Pattern PING_PATTERN = Pattern.compile("(\\d+)\\s*ms");

    private final OkHttpClient client = new OkHttpClient();
    private final SeraphRequestLimiter requestLimiter =
        SeraphRequestLimiter.getInstance();
    private final Map<String, Integer> pingCache = new ConcurrentHashMap<>();
    private final SessionPingFetchGate sessionFetchGate =
        new SessionPingFetchGate();
    private final Set<String> fetchInProgress = ConcurrentHashMap.newKeySet();
    private final AtomicBoolean errorShownThisSession = new AtomicBoolean(false);

    public int getCachedPing(String uuid) {
        return pingCache.getOrDefault(uuid, -1);
    }

    public boolean tryStartFetch(String uuid) {
        return fetchInProgress.add(uuid);
    }

    public void finishFetch(String uuid) {
        fetchInProgress.remove(uuid);
    }

    public void storeInCache(String uuid, int ping) {
        pingCache.put(uuid, ping);
    }

    public void clearPlayer(String uuid) {
        if (uuid == null || uuid.isEmpty()) {
            return;
        }

        pingCache.remove(uuid);
        sessionFetchGate.clearPlayer(uuid);
        fetchInProgress.remove(uuid);
    }

    public void clearCache() {
        pingCache.clear();
        sessionFetchGate.clear();
        fetchInProgress.clear();
    }

    public void fetchAsync(String uuid, String apiKey) {
        if (!SeraphAvailability.isEnabled()) return;
        if (uuid == null || uuid.isEmpty() || apiKey == null || apiKey.trim().isEmpty()) {
            return;
        }
        if (!sessionFetchGate.tryMarkRequested(uuid)) {
            return;
        }
        if (!tryStartFetch(uuid)) {
            return;
        }

        AsyncExecutor.getInstance().supplementalIo(() -> {
            try {
                int ping = fetchPingBlocking(uuid, apiKey);
                if (ping >= 0) {
                    storeInCache(uuid, ping);
                }
            } catch (Exception e) {
                showErrorOnce("Seraph Ping API error", e);
            } finally {
                finishFetch(uuid);
            }
        });
    }

    public int fetchPingBlocking(String uuid, String apiKey) throws IOException {
        if (!SeraphAvailability.isEnabled()) return -1;
        if (apiKey == null || apiKey.trim().isEmpty()) {
            return -1;
        }
        if (!requestLimiter.tryAcquire()) {
            throw new IOException(
                "Seraph request budget exhausted; retry in " +
                requestLimiter.getRetryAfterMillis() +
                "ms"
            );
        }

        String url = PING_URL + "?key=" + apiKey.trim() + "&id=" + uuid;
        Request request = new Request.Builder()
            .url(url)
            .header("User-Agent", "Mellow/" + Mellow.VERSION)
            .build();

        try (Response response = client.newCall(request).execute()) {
            requestLimiter.recordResponse(
                response.code(),
                response.header("Retry-After")
            );
            if (!response.isSuccessful()) {
                throw new IOException("HTTP " + response.code());
            }

            ResponseBody body = response.body();
            if (body == null) {
                return -1;
            }

            JsonObject json = new JsonParser()
                .parse(body.string())
                .getAsJsonObject();
            if (!json.has("tags") || !json.get("tags").isJsonArray()) {
                return -1;
            }

            JsonArray tags = json.getAsJsonArray("tags");
            for (JsonElement element : tags) {
                if (!element.isJsonObject()) {
                    continue;
                }

                JsonObject tag = element.getAsJsonObject();
                if (
                    tag.has("tag_name") &&
                    !tag.get("tag_name").isJsonNull() &&
                    "ping.ping_value".equals(tag.get("tag_name").getAsString())
                ) {
                    int ping = parsePingTagValue(tag);
                    if (ping >= 0) {
                        return ping;
                    }
                }
            }

            for (JsonElement element : tags) {
                if (!element.isJsonObject()) {
                    continue;
                }

                int ping = parsePingTagValue(element.getAsJsonObject());
                if (ping >= 0) {
                    return ping;
                }
            }

            return -1;
        }
    }

    private int parsePingTagValue(JsonObject tag) {
        if (tag == null) {
            return -1;
        }

        if (tag.has("text") && !tag.get("text").isJsonNull()) {
            int ping = parsePingText(tag.get("text").getAsString());
            if (ping >= 0) {
                return ping;
            }
        }

        if (tag.has("tooltip") && !tag.get("tooltip").isJsonNull()) {
            return parsePingText(tag.get("tooltip").getAsString());
        }

        return -1;
    }

    private int parsePingText(String value) {
        if (value == null || value.trim().isEmpty()) {
            return -1;
        }

        Matcher matcher = PING_PATTERN.matcher(value);
        if (!matcher.find()) {
            return -1;
        }

        try {
            return Integer.parseInt(matcher.group(1));
        } catch (NumberFormatException ignored) {
            return -1;
        }
    }

    private void showErrorOnce(String prefix, Exception error) {
        if (!errorShownThisSession.compareAndSet(false, true)) {
            return;
        }

        final String detail = error == null || error.getMessage() == null
            ? "unknown"
            : error.getMessage();
        MainThreadDispatcher.run(() ->
            ChatUtils.sendMessage("§c" + prefix + ": §6" + detail)
        );
    }
}
