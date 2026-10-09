package com.roxiun.mellow.api.hypixel.provider;

import com.roxiun.mellow.Mellow;
import com.roxiun.mellow.api.model.FetchFailureReason;
import com.roxiun.mellow.api.hypixel.provider.model.ProviderId;
import com.roxiun.mellow.api.model.ProviderResult;
import com.roxiun.mellow.api.hypixel.provider.PlayerHttp;
import com.roxiun.mellow.config.MellowOneConfig;
import java.util.HashMap;
import java.util.Map;

public class HypixelPublicApi implements StatsProvider {

    private static final String PLAYER_ENDPOINT = "https://api.hypixel.net/v2/player?uuid=";

    private final MellowOneConfig config;

    public HypixelPublicApi(MellowOneConfig config) {
        this.config = config;
    }

    @Override
    public ProviderId getProviderId() {
        return ProviderId.HYPIXEL_PUBLIC;
    }

    @Override
    public String getDisplayName() {
        return "Hypixel Public API";
    }

    @Override
    public boolean requiresApiKey() {
        return true;
    }

    @Override
    public boolean isConfigured() {
        return config != null && config.hypixelApiKey != null && !config.hypixelApiKey.trim().isEmpty();
    }

    @Override
    public ProviderResult<String> fetchPlayerDataResult(String uuid) {
        if (uuid == null || uuid.isEmpty() || !isConfigured()) {
            return ProviderResult.failure(
                isConfigured()
                    ? FetchFailureReason.UUID_UNAVAILABLE
                    : FetchFailureReason.MISSING_API_KEY,
                isConfigured() ? "Missing UUID" : "Missing API key"
            );
        }

        String cacheKey = normalizeApiKey(config.hypixelApiKey) + ":" + uuid.trim();

        Map<String, String> headers = new HashMap<>();
        headers.put("API-Key", config.hypixelApiKey.trim());

        ProviderResult<String> result = PlayerHttp.fetchPlayerDataResult(
            PLAYER_ENDPOINT + uuid,
            Mellow.NAME + "/" + Mellow.VERSION,
            headers
        );
        return result;
    }

    private String normalizeApiKey(String apiKey) {
        return apiKey == null ? "" : apiKey.trim();
    }

}
