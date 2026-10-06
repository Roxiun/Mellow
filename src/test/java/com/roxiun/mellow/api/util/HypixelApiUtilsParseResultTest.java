package com.roxiun.mellow.api.util;

import com.roxiun.mellow.api.provider.model.FetchFailureReason;
import com.roxiun.mellow.api.provider.model.ProviderId;
import com.roxiun.mellow.api.provider.model.ProviderResult;
import org.junit.Assert;
import org.junit.Test;

public class HypixelApiUtilsParseResultTest {

    @Test
    public void parsePlayerDataResultFlagsMissingPlayerData() {
        ProviderResult<?> result = HypixelApiUtils.parsePlayerDataResult(
            "{}",
            ProviderId.HYPIXEL_PUBLIC
        );

        Assert.assertFalse(result.isSuccess());
        Assert.assertEquals(
            FetchFailureReason.NO_PLAYER_DATA,
            result.getFailureReason()
        );
    }

    @Test
    public void parsePlayerDataResultFlagsInvalidJsonAsParseFailure() {
        ProviderResult<?> result = HypixelApiUtils.parsePlayerDataResult(
            "{not-json}",
            ProviderId.ABYSS
        );

        Assert.assertFalse(result.isSuccess());
        Assert.assertEquals(
            FetchFailureReason.PARSE_ERROR,
            result.getFailureReason()
        );
    }

    @Test
    public void parsePlayerDataResultAcceptsBordicHypixelShape() {
        ProviderResult<?> result = HypixelApiUtils.parsePlayerDataResult(
            "{\"success\":true,\"player\":{\"displayname\":\"BordicPlayer\",\"stats\":{\"Bedwars\":{}},\"achievements\":{}}}",
            ProviderId.BORDIC
        );

        Assert.assertTrue(result.isSuccess());
    }
    @Test
    public void bedlifyCacheSupportsAllStatModes() {
        String json = "{\"success\":true,\"lastUpdated\":1700000000000,\"player\":{\"displayname\":\"CachePlayer\",\"stats\":{\"Bedwars\":{},\"SkyWars\":{},\"Duels\":{},\"BuildBattle\":{},\"TNTGames\":{}},\"achievements\":{}}}";
        Assert.assertTrue(HypixelApiUtils.parsePlayerDataResult(json, ProviderId.BEDLIFY).isSuccess());
        Assert.assertNotNull(HypixelApiUtils.parsePlayerData(json, "Bedlify"));
        Assert.assertTrue(HypixelApiUtils.parseSkywarsPlayerDataResult(json, ProviderId.BEDLIFY).isSuccess());
        Assert.assertTrue(HypixelApiUtils.parseDuelsPlayerDataResult(json, ProviderId.BEDLIFY, com.roxiun.mellow.api.duels.DuelsMode.OVERALL).isSuccess());
        Assert.assertTrue(HypixelApiUtils.parseBuildBattlePlayerDataResult(json, ProviderId.BEDLIFY).isSuccess());
        Assert.assertTrue(HypixelApiUtils.parseTntRunPlayerDataResult(json, ProviderId.BEDLIFY).isSuccess());
    }

    @Test
    public void bedlifyCacheRejectsMissingOrUnsuccessfulPlayer() {
        for (String json : new String[] {"{\"success\":false,\"player\":{\"displayname\":\"Ignored\"}}", "{\"success\":true,\"player\":null}"}) {
            ProviderResult<?> result = HypixelApiUtils.parsePlayerDataResult(json, ProviderId.BEDLIFY);
            Assert.assertFalse(result.isSuccess());
            Assert.assertEquals(FetchFailureReason.NO_PLAYER_DATA, result.getFailureReason());
        }
    }
}
