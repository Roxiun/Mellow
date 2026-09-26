package com.roxiun.mellow.cache;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;

import com.roxiun.mellow.api.provider.model.FetchFailureReason;
import com.roxiun.mellow.data.PlayerProfile;
import org.junit.Test;

public class ProfileFetchResultTest {

    @Test
    public void failureCanCarryProfileForIndependentEnrichment() {
        PlayerProfile profile = PlayerProfile.identity("uuid", "Player");

        ProfileFetchResult result = ProfileFetchResult.failure(
            FetchFailureReason.PROVIDER_ERROR,
            "Stats provider unavailable",
            "Provider"
        )
            .withProfile(profile);

        assertFalse(result.isSuccess());
        assertSame(profile, result.getProfile());
    }
}
