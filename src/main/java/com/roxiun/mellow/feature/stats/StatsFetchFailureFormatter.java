package com.roxiun.mellow.feature.stats;

import com.roxiun.mellow.api.model.FetchFailureReason;
import com.roxiun.mellow.cache.ProfileFetchResult;

public final class StatsFetchFailureFormatter {

    private StatsFetchFailureFormatter() {}

    public static String describe(ProfileFetchResult result) {
        if (result == null) {
            return "unknown error";
        }

        FetchFailureReason reason = result.getFailureReason();
        String providerName = providerLabel(result.getProviderName());
        String detail = normalizeDetail(result.getErrorDetail());

        if (reason == FetchFailureReason.MISSING_API_KEY) {
            return providerName + " needs an API key";
        }
        if (reason == FetchFailureReason.UUID_UNAVAILABLE) {
            if (detail.toLowerCase().contains("in-game uuid")) {
                return "no in-game UUID available yet";
            }
            return "could not resolve UUID";
        }
        if (reason == FetchFailureReason.RATE_LIMITED) {
            return providerName + " rate limited the request";
        }
        if (reason == FetchFailureReason.NETWORK_ERROR) {
            if (detail.toLowerCase().contains("timeout")) {
                return providerName + " timed out";
            }
            return providerName + " had a network error";
        }
        if (reason == FetchFailureReason.NO_PLAYER_DATA) {
            if (detail.toLowerCase().contains("nicked or an npc")) {
                return "player is nicked or an NPC";
            }
            return providerName + " returned no player data (possibly nicked)";
        }
        if (reason == FetchFailureReason.PARSE_ERROR) {
            return providerName + " returned unreadable data";
        }
        if (reason == FetchFailureReason.PROVIDER_ERROR) {
            if (!detail.isEmpty()) {
                return providerName + " returned " + detail;
            }
            return providerName + " returned an error";
        }

        if (!detail.isEmpty()) {
            return detail;
        }
        return "unknown error";
    }

    private static String providerLabel(String providerName) {
        return providerName == null || providerName.trim().isEmpty()
            ? "The provider"
            : providerName;
    }

    private static String normalizeDetail(String detail) {
        return detail == null ? "" : detail.trim();
    }
}
