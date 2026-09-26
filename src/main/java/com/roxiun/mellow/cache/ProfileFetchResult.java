package com.roxiun.mellow.cache;

import com.roxiun.mellow.api.provider.model.FetchFailureReason;
import com.roxiun.mellow.data.PlayerProfile;

public class ProfileFetchResult {

    private final PlayerProfile profile;
    private final FetchFailureReason failureReason;
    private final String errorDetail;
    private final String providerName;

    private ProfileFetchResult(
        PlayerProfile profile,
        FetchFailureReason failureReason,
        String errorDetail,
        String providerName
    ) {
        this.profile = profile;
        this.failureReason = failureReason;
        this.errorDetail = errorDetail;
        this.providerName = providerName;
    }

    public static ProfileFetchResult success(
        PlayerProfile profile,
        String providerName
    ) {
        return new ProfileFetchResult(profile, null, null, providerName);
    }

    public static ProfileFetchResult failure(
        FetchFailureReason failureReason,
        String errorDetail,
        String providerName
    ) {
        return new ProfileFetchResult(
            null,
            failureReason == null ? FetchFailureReason.UNKNOWN : failureReason,
            errorDetail,
            providerName
        );
    }

    public boolean isSuccess() {
        return failureReason == null && profile != null;
    }

    public ProfileFetchResult withProfile(PlayerProfile availableProfile) {
        if (failureReason == null || availableProfile == null) {
            return this;
        }
        return new ProfileFetchResult(
            availableProfile,
            failureReason,
            errorDetail,
            providerName
        );
    }

    public PlayerProfile getProfile() {
        return profile;
    }

    public FetchFailureReason getFailureReason() {
        return failureReason;
    }

    public String getErrorDetail() {
        return errorDetail;
    }

    public String getProviderName() {
        return providerName;
    }
}
