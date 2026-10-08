package com.roxiun.mellow.api.aurora;

import com.roxiun.mellow.util.cache.TimedValueCache;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.roxiun.mellow.Mellow;
import com.roxiun.mellow.core.async.AsyncExecutor;
import com.roxiun.mellow.core.async.MainThreadDispatcher;
import com.roxiun.mellow.util.ChatUtils;
import com.roxiun.mellow.util.ping.PingRetryGate;
import java.io.IOException;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

public class AuroraPingService {

    private static final String PING_URL =
        "https://bordic.xyz/api/v2/resources/ping";

    private volatile long generation;
    private final OkHttpClient client;
    private final TimedValueCache<String, Integer> pingCache = new TimedValueCache<>(120_000L);
    private final PingRetryGate sessionFetchGate =
        new PingRetryGate();
    private final Set<String> fetchInProgress = ConcurrentHashMap.newKeySet();
    private final AtomicBoolean errorShownThisSession = new AtomicBoolean(false);

    public AuroraPingService() {
        this(new OkHttpClient());
    }

    AuroraPingService(OkHttpClient client) {
        this.client = client == null ? new OkHttpClient() : client;
    }

    public int getCachedPing(String compactUuid) {
        Integer ping = pingCache.get(compactUuid);
        return ping == null ? -1 : ping;
    }

    public boolean tryStartFetch(String compactUuid) {
        return fetchInProgress.add(compactUuid);
    }

    public void finishFetch(String compactUuid) {
        fetchInProgress.remove(compactUuid);
    }

    public void storeInCache(String compactUuid, int ping) {
        pingCache.put(compactUuid, ping);
    }

    public void clearPlayer(String compactUuid) {
        if (compactUuid == null || compactUuid.isEmpty()) {
            return;
        }

        generation++;
        fetchInProgress.clear();
        pingCache.remove(compactUuid);
        sessionFetchGate.clearPlayer(compactUuid);
        fetchInProgress.remove(compactUuid);
    }

    public void clearCache() {
        generation++;
        pingCache.clear();
        sessionFetchGate.clear();
        fetchInProgress.clear();
    }

    public boolean hasShownError() {
        return errorShownThisSession.get();
    }

    public void markErrorShown() {
        errorShownThisSession.set(true);
    }

    public void fetchAsync(String compactUuid) {
        if (compactUuid == null || compactUuid.isEmpty()) {
            return;
        }
        if (!sessionFetchGate.tryMarkRequested(compactUuid)) {
            return;
        }
        if (!tryStartFetch(compactUuid)) {
            return;
        }

        long requestGeneration = generation;
        AsyncExecutor.getInstance().supplementalIo(() -> {
            try {
                int ping = fetchPingBlocking(compactUuid);
                if (ping >= 0 && generation == requestGeneration) {
                    storeInCache(compactUuid, ping);
                }
            } catch (Exception e) {
                showErrorOnce("Aurora Ping API error", e);
            } finally {
                if (generation == requestGeneration) finishFetch(compactUuid);
            }
        });
    }

    public int fetchPingBlocking(String compactUuid) throws IOException {
        String url = PING_URL + "?uuid=" + compactUuid;
        Request request = new Request.Builder()
            .url(url)
            .header("User-Agent", "Mellow/" + Mellow.VERSION)
            .build();

        try (Response response = client.newCall(request).execute()) {
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
            if (
                !json.has("success") ||
                !json.get("success").getAsBoolean()
            ) {
                throw new IOException("success=false from Aurora Ping API");
            }

            if (!json.has("data") || !json.get("data").isJsonArray()) {
                return -1;
            }

            JsonArray data = json.getAsJsonArray("data");
            for (JsonElement element : data) {
                if (!element.isJsonObject()) {
                    continue;
                }
                JsonObject entry = element.getAsJsonObject();
                if (entry.has("avg") && !entry.get("avg").isJsonNull()) {
                    return entry.get("avg").getAsInt();
                }
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
