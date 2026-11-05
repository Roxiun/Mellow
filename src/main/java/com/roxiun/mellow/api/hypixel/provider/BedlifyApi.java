package com.roxiun.mellow.api.hypixel.provider;

import com.roxiun.mellow.Mellow;
import com.roxiun.mellow.api.model.FetchFailureReason;
import com.roxiun.mellow.api.hypixel.provider.model.ProviderId;
import com.roxiun.mellow.api.model.ProviderResult;
import com.roxiun.mellow.api.hypixel.provider.PlayerHttp;

public class BedlifyApi implements StatsProvider {

    private static final String PLAYER_ENDPOINT =
        "https://api.bedlify.xyz/v1/player/cache?uuid=";

    public BedlifyApi() {
    }

    @Override
    public ProviderId getProviderId() {
        return ProviderId.BEDLIFY;
    }

    @Override
    public String getDisplayName() {
        return "Bedlify";
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

        ProviderResult<String> result = PlayerHttp.fetchPlayerDataResult(
            PLAYER_ENDPOINT + cacheKey,
            "Mellow/" + Mellow.VERSION
        );
        return result;
    }

}
