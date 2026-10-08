package com.roxiun.mellow.stats;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.roxiun.mellow.api.hypixel.HypixelPlayerData;
import com.roxiun.mellow.api.hypixel.provider.model.ProviderId;
import com.roxiun.mellow.api.tags.TagReport;
import com.roxiun.mellow.data.PlayerProfile;
import org.junit.Test;
import static org.junit.Assert.*;

public class PlayerFirstLoginTest {
    @Test public void originalResponseCarriesFirstLoginThroughTagEnrichment() {
        JsonObject root = new JsonParser().parse("{\"success\":true,\"player\":{\"displayname\":\"Player\","
            + "\"firstLogin\":1700000000000,\"stats\":{\"Bedwars\":{}}}}").getAsJsonObject();
        PlayerProfile profile = PlayerStatsService.parse("uuid", "Player", root, ProviderId.BORDIC,
            StatsSelection.overall(StatScope.BEDWARS)).getValue();
        assertNotNull(profile);
        assertEquals(1700000000000L, profile.getFirstLogin());
        assertEquals(profile.getFirstLogin(), profile.withTags(TagReport.empty()).getFirstLogin());
        assertEquals(0L, PlayerProfile.identity("uuid", "Player").getFirstLogin());
    }

    @Test public void nadeshikoFirstLoginUsesDecodedResponse() {
        for (String json : new String[] {
            "{\"name\":\"Player\",\"first_login\":1700000000000}",
            "{\"profile\":{\"hypixel_displayname\":\"Player\",\"first_login\":\"1700000000000\"}}"
        }) {
            assertEquals(1700000000000L,
                HypixelPlayerData.decode(json, ProviderId.NADESHIKO).getValue().firstLogin());
        }
    }

    @Test public void absentOrMalformedFirstLoginDoesNotFailPlayerData() {
        for (String value : new String[] {"null", "\"invalid\"", "{}"}) {
            assertEquals(0L, HypixelPlayerData.decode("{\"success\":true,\"player\":{"
                + "\"displayname\":\"Player\",\"firstLogin\":" + value + "}}",
                ProviderId.HYPIXEL_PUBLIC).getValue().firstLogin());
        }
    }
}
