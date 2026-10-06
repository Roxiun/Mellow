package com.roxiun.mellow.feature.stats;

import com.roxiun.mellow.api.provider.model.FetchFailureReason;
import com.roxiun.mellow.cache.ProfileFetchResult;
import org.junit.Assert;
import org.junit.Test;

public class StatsFetchFailureFormatterTest {

    @Test
    public void describesHiddenProfilesWithoutProviderNoise() {
        ProfileFetchResult result = ProfileFetchResult.failure(
            FetchFailureReason.NO_PLAYER_DATA,
            "Player is nicked or an NPC",
            "Abyss"
        );

        String description = StatsFetchFailureFormatter.describe(result);
        Assert.assertTrue(description.contains("nicked"));
        Assert.assertTrue(description.contains("NPC"));
        Assert.assertFalse(description.contains("Abyss"));
    }
}
