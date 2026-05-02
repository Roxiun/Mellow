package com.roxiun.mellow.api.frosty;

import com.google.gson.Gson;
import com.roxiun.mellow.util.cache.TimedValueCache;
import java.io.IOException;
import java.util.Locale;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

public class FrostyApi {

    private static final long QUERY_CACHE_TTL_MS = 60_000L;
    private static final String BASE_URL =
        "https://api.sukie.net/v1/bedwars/cosmetics";

    private final OkHttpClient client;
    private final Gson gson;
    private final TimedValueCache<String, String> queryCache =
        new TimedValueCache<>(QUERY_CACHE_TTL_MS);

    public FrostyApi() {
        this(new OkHttpClient(), new Gson());
    }

    FrostyApi(OkHttpClient client) {
        this(client, new Gson());
    }

    FrostyApi(OkHttpClient client, Gson gson) {
        this.client = client == null ? new OkHttpClient() : client;
        this.gson = gson == null ? new Gson() : gson;
    }

    public FrostyReponse queryStats(
        int finalKills,
        int bedsBroken,
        String apiKey
    ) throws IOException {
        return queryCosmetics(finalKills, bedsBroken, apiKey);
    }

    public FrostyReponse queryCosmetics(
        int finalKills,
        int bedsBroken,
        String apiKey
    ) throws IOException {
        if (apiKey == null || apiKey.isEmpty()) {
            return null;
        }

        if (finalKills == 0 && bedsBroken == 0) {
            return null;
        }

        String cacheKey = buildCacheKey(finalKills, bedsBroken, apiKey);
        if (queryCache.containsFresh(cacheKey)) {
            return parseResponse(queryCache.get(cacheKey));
        }

        String finalsKillsParam = finalKills == 0 ? "" : "&final_kills=" + finalKills;
        String bedsBrokenParam = bedsBroken == 0 ? "" : "&beds_broken=" + bedsBroken;
        String url = BASE_URL + "?key=" + apiKey + finalsKillsParam + bedsBrokenParam + "&rank=mvp%2B%2B";

        Request request = new Request.Builder()
            .url(url)
            .header("User-Agent", "Mellow/4.1.0")
            .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                System.err.println("Frosty API request failed: " + response);
                queryCache.put(cacheKey, null);
                return null;
            }

            ResponseBody body = response.body();
            if (body == null) {
                queryCache.put(cacheKey, null);
                return null;
            }

            String payload = body.string();
            queryCache.put(cacheKey, payload);
            return parseResponse(payload);
        }
    }

    public void clearCache() {
        queryCache.clear();
    }

    private FrostyReponse parseResponse(String payload) {
        return payload == null ? null : gson.fromJson(payload, FrostyReponse.class);
    }

    private String buildCacheKey(int finalKills, int bedsBroken, String apiKey) {
        return (
            finalKills +
            "|" +
            bedsBroken +
            "|" +
            safe(apiKey).toLowerCase(Locale.ROOT)
        );
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }
}

