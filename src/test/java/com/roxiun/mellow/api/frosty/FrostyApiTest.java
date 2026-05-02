package com.roxiun.mellow.api.frosty;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Protocol;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.junit.Assert;
import org.junit.Test;

public class FrostyApiTest {

    @Test
    public void queryStatsCachesSuccessfulResponsesByRequestParams() throws IOException {
        AtomicInteger requestCount = new AtomicInteger();
        OkHttpClient client = new OkHttpClient.Builder()
            .addInterceptor(chain -> {
                Request request = chain.request();
                requestCount.incrementAndGet();
                Assert.assertEquals("mvp++", request.url().queryParameter("rank"));
                Assert.assertEquals(
                    "19953",
                    request.url().queryParameter("final_kills")
                );
                Assert.assertEquals(
                    "9183",
                    request.url().queryParameter("beds_broken")
                );
                return response(
                    request,
                    200,
                    "{" +
                        "\"success\":true," +
                        "\"data\":[{" +
                        "\"username\":\"bashbuta\"," +
                        "\"uuid\":\"5efb01316aef44219e2f67e36f607c0c\"," +
                        "\"game_rank\":\"MVP++\"," +
                        "\"rank_color\":\"GOLD\"," +
                        "\"plus_color\":\"DARK_AQUA\"," +
                        "\"network_level\":38201236," +
                        "\"last_online\":\"2026-04-17T22:22:15.000Z\"," +
                        "\"bedwars_index\":53343," +
                        "\"bedwars_level\":2441048," +
                        "\"total_final_kills\":19953," +
                        "\"total_final_deaths\":1940," +
                        "\"total_beds_broken\":9183," +
                        "\"total_beds_lost\":2608," +
                        "\"total_wins\":4533," +
                        "\"total_losses\":1940," +
                        "\"island_topper\":\"islandtopper_none\"," +
                        "\"victory_dance\":\"victorydance_toy_stick\"," +
                        "\"kill_effect\":\"killeffect_blood_explosion\"," +
                        "\"projectile_trail\":\"projectiletrail_meteorblaze\"," +
                        "\"kill_message\":\"killmessages_counter\"," +
                        "\"npc_skin\":\"npcskin_none\"," +
                        "\"death_cry\":\"deathcry_none\"," +
                        "\"spray\":\"sprays_carried\"," +
                        "\"bed_destroy\":\"beddestroy_pig_missile\"," +
                        "\"glyph\":\"glyph_none\"," +
                        "\"wood_type\":\"woodskin_dark_oak_log\"," +
                        "\"starting_weapon\":\"starting_weapon_wooden_sword\"," +
                        "\"figurine\":\"figurine_none\"" +
                        "}]" +
                        "}"
                );
            })
            .build();

        FrostyApi api = new FrostyApi(client);

        FrostyReponse first = api.queryCosmetics(19953, 9183, "frosty-key");
        FrostyReponse second = api.queryCosmetics(19953, 9183, "frosty-key");

        Assert.assertNotNull(first);
        Assert.assertNotNull(second);
        Assert.assertTrue(first.success);
        Assert.assertEquals(1, first.data.size());
        Assert.assertEquals("bashbuta", first.data.get(0).username);
        Assert.assertEquals(38201236L, first.data.get(0).networkLevel);
        Assert.assertEquals(1, requestCount.get());
        Assert.assertNotSame(first, second);
    }

    @Test
    public void queryStatsCachesFailedResponsesByRequestParams() throws IOException {
        AtomicInteger requestCount = new AtomicInteger();
        OkHttpClient client = new OkHttpClient.Builder()
            .addInterceptor(chain -> {
                Request request = chain.request();
                requestCount.incrementAndGet();
                return response(request, 500, "{}");
            })
            .build();

        FrostyApi api = new FrostyApi(client);

        Assert.assertNull(api.queryCosmetics(19953, 9183, "frosty-key"));
        Assert.assertNull(api.queryCosmetics(19953, 9183, "frosty-key"));
        Assert.assertEquals(1, requestCount.get());
    }

    private static Response response(Request request, int code, String body) {
        return new Response.Builder()
            .request(request)
            .protocol(Protocol.HTTP_1_1)
            .code(code)
            .message(code == 200 ? "OK" : "Error")
            .body(ResponseBody.create(body, MediaType.parse("application/json")))
            .build();
    }
}


