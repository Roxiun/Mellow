package com.roxiun.mellow.api.hypixel.provider;

import com.roxiun.mellow.api.hypixel.provider.model.ProviderId;
import com.roxiun.mellow.api.model.ProviderResult;

/** Transport only. PlayerCache owns request sharing and freshness. */
public interface StatsProvider {
    ProviderId getProviderId();
    String getDisplayName();
    default boolean requiresApiKey() { return false; }
    default boolean isConfigured() { return true; }
    ProviderResult<String> fetchPlayerDataResult(String uuid);
    default boolean supportsBatch() { return false; }
    default java.util.Map<String, ProviderResult<String>> fetchBatch(java.util.Set<String> uuids) {
        java.util.Map<String, ProviderResult<String>> result = new java.util.LinkedHashMap<>();
        for (String uuid : uuids) result.put(uuid, fetchPlayerDataResult(uuid));
        return result;
    }
}
