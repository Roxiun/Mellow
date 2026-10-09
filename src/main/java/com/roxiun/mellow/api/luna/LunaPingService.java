package com.roxiun.mellow.api.luna;

import com.roxiun.mellow.util.cache.LookupTracker;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.roxiun.mellow.Mellow;
import com.roxiun.mellow.core.async.AsyncExecutor;
import com.roxiun.mellow.core.async.MainThreadDispatcher;
import com.roxiun.mellow.util.ChatUtils;
import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

public class LunaPingService {

    private static final String PING_URL = "https://lunaaaa.net/ping/";

    private final LookupTracker<String> requests = new LookupTracker<>();
    private final OkHttpClient client = new OkHttpClient();
    private final java.util.Map<String, Integer> pingCache = new ConcurrentHashMap<>();
    private final AtomicBoolean errorShownThisSession = new AtomicBoolean(false);

    public int getCachedPing(String uuid) {
        Integer ping = pingCache.get(uuid);
        return ping == null ? -1 : ping;
    }

    public void storeInCache(String uuid, int ping) {
        pingCache.put(uuid, ping);
    }

    public synchronized void clearPlayer(String uuid) {
        if (uuid == null || uuid.isEmpty()) {
            return;
        }

        requests.remove(uuid);
        pingCache.remove(uuid);
    }

    public synchronized void clearCache() {
        requests.clear();
        pingCache.clear();
        errorShownThisSession.set(false);
    }

    public boolean hasShownError() {
        return errorShownThisSession.get();
    }

    public void markErrorShown() {
        errorShownThisSession.set(true);
    }

    public void fetchAsync(String uuid, String apiKey) {
        if (uuid == null || uuid.isEmpty() || apiKey == null || apiKey.trim().isEmpty()) {
            return;
        }
        LookupTracker.Attempt attempt = requests.begin(uuid);
        if (attempt == null) return;
        AsyncExecutor.getInstance().supplementalIo(() -> {
            try {
                int ping = fetchPingBlocking(uuid, apiKey);
                synchronized (LunaPingService.this) {
                    if (requests.isCurrent(uuid, attempt)) {
                        if (ping >= 0) storeInCache(uuid, ping);
                        requests.finish(uuid, attempt, ping >= 0);
                    }
                }
            } catch (Exception e) {
                showErrorOnce("Luna Ping API error", e);
                requests.finish(uuid, attempt, false);
            }
        });
    }

    public int fetchPingBlocking(String uuid, String apiKey) throws IOException {
        if (apiKey == null || apiKey.trim().isEmpty()) {
            return -1;
        }

        String url = PING_URL + uuid + "?key=LunaAPI-" + apiKey.trim();
        Request request = new Request.Builder()
            .url(url)
            .header("User-Agent", "Mellow/" + Mellow.VERSION)
            .build();

        try (Response response = client.newCall(request).execute()) {
            ResponseBody body = response.body();
            if (body == null) {
                return -1;
            }

            JsonObject json = new JsonParser()
                .parse(body.string())
                .getAsJsonObject();

            if (json.has("error") && !json.get("error").isJsonNull()) {
                throw new IOException(json.get("error").getAsString());
            }

            if (!response.isSuccessful()) {
                throw new IOException("HTTP " + response.code());
            }

            if (json.has("last_ping") && !json.get("last_ping").isJsonNull()) {
                return json.get("last_ping").getAsInt();
            }

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
