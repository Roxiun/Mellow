package com.roxiun.mellow.api.provider;

import com.roxiun.mellow.api.provider.model.FetchFailureReason;
import com.roxiun.mellow.api.provider.model.ProviderId;
import com.roxiun.mellow.api.provider.model.ProviderResult;
import com.roxiun.mellow.api.util.HypixelApiUtils;

public class AbyssApi implements StatsProvider {

    public AbyssApi() {
    }

    @Override
    public ProviderId getProviderId() {
        return ProviderId.ABYSS;
    }

    @Override
    public String getDisplayName() {
        return "Abyss";
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
            "http://api.abyssoverlay.com/player?uuid=" + uuid,
            "node-ao/2.0.3"
        );
        return result;
    }

}
