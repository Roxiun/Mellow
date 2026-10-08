package com.roxiun.mellow.api.provider;

import com.roxiun.mellow.api.provider.model.FetchFailureReason;
import com.roxiun.mellow.api.provider.model.ProviderId;
import com.roxiun.mellow.api.provider.model.ProviderResult;
import com.roxiun.mellow.api.util.HypixelApiUtils;

public class NadeshikoApi implements StatsProvider {

    public NadeshikoApi() {
    }

    @Override
    public ProviderId getProviderId() {
        return ProviderId.NADESHIKO;
    }

    @Override
    public String getDisplayName() {
        return "Nadeshiko";
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
            "https://nadeshiko.io/player/" + uuid + "/network",
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36"
        );
        if (!result.isSuccess()) return result;
        java.util.regex.Matcher match = java.util.regex.Pattern.compile(
            "playerData = JSON.parse\\(decodeURIComponent\\(\"(.*?)\"\\)\\)"
        ).matcher(result.getValue());
        if (!match.find()) return ProviderResult.failure(FetchFailureReason.PARSE_ERROR, "Missing Nadeshiko player data");
        try { return ProviderResult.success(java.net.URLDecoder.decode(match.group(1), "UTF-8")); }
        catch (Exception error) { return ProviderResult.failure(FetchFailureReason.PARSE_ERROR, error.getMessage()); }
    }

}
