package com.roxiun.mellow.api.provider;

import com.google.gson.JsonParser;
import com.google.gson.JsonObject;
import com.roxiun.mellow.Mellow;
import com.roxiun.mellow.api.provider.model.FetchFailureReason;
import com.roxiun.mellow.api.provider.model.ProviderId;
import com.roxiun.mellow.api.provider.model.ProviderResult;
import com.roxiun.mellow.api.util.HypixelApiUtils;

public class BordicApi implements StatsProvider {

    private static final String PLAYER_ENDPOINT =
        "https://api.bordic.xyz/v3/cache/hypixel?uuid=";

    public BordicApi() {
    }

    @Override
    public ProviderId getProviderId() {
        return ProviderId.BORDIC;
    }

    @Override
    public String getDisplayName() {
        return "Bordic";
    }

    @Override
    public ProviderResult<String> fetchPlayerDataResult(String uuid) {
        if (uuid == null || uuid.trim().isEmpty()) {
            return ProviderResult.failure(
                FetchFailureReason.UUID_UNAVAILABLE,
                "Missing UUID"
            );
        }

        String cacheKey = uuid.trim();

        ProviderResult<String> result = HypixelApiUtils.fetchPlayerDataResult(
            PLAYER_ENDPOINT + cacheKey,
            "Mellow/" + Mellow.VERSION
        );
        return result;
    }

    @Override public boolean supportsBatch() { return true; }

    @Override public java.util.Map<String, ProviderResult<String>> fetchBatch(java.util.Set<String> uuids) {
        java.util.Map<String, ProviderResult<String>> results = new java.util.LinkedHashMap<>();
        com.google.gson.JsonArray ids = new com.google.gson.JsonArray();
        for (String uuid : uuids) ids.add(new com.google.gson.JsonPrimitive(uuid));
        JsonObject body = new JsonObject(); body.add("uuids", ids);
        java.net.HttpURLConnection connection = null;
        try {
            connection = (java.net.HttpURLConnection) new java.net.URL("https://api.bordic.xyz/v3/cache/hypixel").openConnection();
            connection.setRequestMethod("POST"); connection.setDoOutput(true);
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setRequestProperty("User-Agent", "Mellow/" + Mellow.VERSION);
            connection.setConnectTimeout(5000); connection.setReadTimeout(5000);
            try (java.io.OutputStream out = connection.getOutputStream()) {
                out.write(body.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8));
            }
            int status = connection.getResponseCode();
            if (status != 200) {
                for (String uuid : uuids) results.put(uuid, ProviderResult.failure(
                    status == 429 ? FetchFailureReason.RATE_LIMITED : FetchFailureReason.PROVIDER_ERROR, "HTTP " + status));
                return results;
            }
            JsonObject response;
            try (java.io.Reader reader = new java.io.InputStreamReader(connection.getInputStream(), java.nio.charset.StandardCharsets.UTF_8)) {
                response = new JsonParser().parse(reader).getAsJsonObject();
            }
            for (String uuid : uuids) results.put(uuid, response.has(uuid)
                ? ProviderResult.success(response.get(uuid).toString())
                : ProviderResult.failure(FetchFailureReason.NO_PLAYER_DATA, "No cached player data"));
        } catch (Exception error) {
            for (String uuid : uuids) results.put(uuid, ProviderResult.failure(FetchFailureReason.NETWORK_ERROR, error.getMessage()));
        } finally { if (connection != null) connection.disconnect(); }
        return results;
    }
}
