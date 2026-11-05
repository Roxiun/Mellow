package com.roxiun.mellow.stats;

import com.roxiun.mellow.api.hypixel.HypixelPlayerData;

import com.roxiun.mellow.api.model.FetchFailureReason;
import com.roxiun.mellow.api.hypixel.provider.model.ProviderId;
import com.roxiun.mellow.api.model.ProviderResult;
import org.junit.Assert;
import org.junit.Test;

public class PlayerStatsParsingTest {

    @Test
    public void parsePlayerDataResultFlagsMissingPlayerData() {
        ProviderResult<?> result = parse(
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
        ProviderResult<?> result = parse(
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
        ProviderResult<?> result = parse(
            "{\"success\":true,\"player\":{\"displayname\":\"BordicPlayer\",\"stats\":{\"Bedwars\":{}},\"achievements\":{}}}",
            ProviderId.BORDIC
        );

        Assert.assertTrue(result.isSuccess());
    }
    @Test
    public void bedlifyCacheSupportsAllStatModes() {
        String json = "{\"success\":true,\"lastUpdated\":1700000000000,\"player\":{\"displayname\":\"CachePlayer\",\"stats\":{\"Bedwars\":{},\"SkyWars\":{},\"Duels\":{},\"BuildBattle\":{},\"TNTGames\":{}},\"achievements\":{}}}";
        Assert.assertTrue(parse(json, ProviderId.BEDLIFY).isSuccess());
        Assert.assertTrue(parse(json, ProviderId.BEDLIFY, StatScope.SKYWARS).isSuccess());
        Assert.assertTrue(parse(json, ProviderId.BEDLIFY, StatScope.DUELS).isSuccess());
        Assert.assertTrue(parse(json, ProviderId.BEDLIFY, StatScope.BUILD_BATTLE).isSuccess());
        Assert.assertTrue(parse(json, ProviderId.BEDLIFY, StatScope.TNT_RUN).isSuccess());
    }

    @Test
    public void bedlifyCacheRejectsMissingOrUnsuccessfulPlayer() {
        for (String json : new String[] {"{\"success\":false,\"player\":{\"displayname\":\"Ignored\"}}", "{\"success\":true,\"player\":null}"}) {
            ProviderResult<?> result = parse(json, ProviderId.BEDLIFY);
            Assert.assertFalse(result.isSuccess());
            Assert.assertEquals(FetchFailureReason.NO_PLAYER_DATA, result.getFailureReason());
        }
    }
    private ProviderResult<?> parse(String json, ProviderId provider) { return parse(json, provider, StatScope.BEDWARS); }
    private ProviderResult<?> parse(String json, ProviderId provider, StatScope scope) {
        ProviderResult<HypixelPlayerData> decoded = HypixelPlayerData.decode(json, provider);
        return decoded.isSuccess() ? GameRegistry.find(scope).parse(decoded.getValue(), "overall") : decoded;
    }
}
