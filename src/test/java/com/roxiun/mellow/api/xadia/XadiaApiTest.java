package com.roxiun.mellow.api.xadia;

import com.roxiun.mellow.api.tags.TagReport;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.roxiun.mellow.support.FakeHttpURLConnection;
import com.roxiun.mellow.data.PlayerProfile;
import com.roxiun.mellow.util.formatting.FormattingUtils;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.Assert;
import org.junit.Test;

public class XadiaApiTest {
    private static final String RESPONSE = "{\"results\":[{\"query\":\"player\",\"found\":true,\"tags\":["
        + "{\"type\":\"sniper\",\"label\":\"Sniper\",\"reason\":\"Evidence\",\"verified\":true},"
        + "{\"type\":\"hacker\",\"label\":\"Hacker\",\"reason\":null,\"verified\":false},"
        + "{\"type\":\"possibly_cheating\",\"label\":\"Risky\",\"reason\":null,\"verified\":null}]}]}";

    private FakeHttpURLConnection connection(int status, String body) throws Exception {
        return new FakeHttpURLConnection(new URL("https://xadia.sniped.me/v1/players"), status,
            body, "{\"error\":\"request rejected\"}");
    }

    private XadiaApi api(FakeHttpURLConnection connection, AtomicInteger calls) {
        return new XadiaApi() {
            @Override
            protected HttpURLConnection openConnection(URL url) {
                Assert.assertEquals(connection.getURL(), url);
                calls.incrementAndGet();
                return connection;
            }
        };
    }

    @Test
    public void sendsAuthenticatedPostAndPreservesVerification() throws Exception {
        FakeHttpURLConnection connection = connection(200, RESPONSE);
        List<XadiaTag> tags = api(connection, new AtomicInteger()).fetchXadiaTags("player", null, " key ", false);
        Assert.assertEquals("POST", connection.getRequestMethod());
        Assert.assertEquals("key", connection.getRequestProperty("X-API-Key"));
        Assert.assertEquals("application/json", connection.getRequestProperty("Content-Type"));
        JsonObject body = new JsonParser().parse(connection.getWrittenBody()).getAsJsonObject();
        Assert.assertEquals("player", body.getAsJsonArray("players").get(0).getAsString());
        Assert.assertFalse(body.get("verified_only").getAsBoolean());
        Assert.assertEquals(Boolean.TRUE, tags.get(0).getVerified());
        Assert.assertEquals(Boolean.FALSE, tags.get(1).getVerified());
        Assert.assertNull(tags.get(2).getVerified());
        Assert.assertNull(tags.get(1).getReason());
        Assert.assertTrue(FormattingUtils.formatXadiaTag(tags.get(1)).contains("[Unverified]"));
        Assert.assertFalse(FormattingUtils.formatXadiaTag(tags.get(2)).contains("[Unverified]"));
        Assert.assertEquals("§8[§4S§8]§r", FormattingUtils.formatXadiaTagIcon(tags.get(0)));
        Assert.assertEquals("§8[§cH§8]§r", FormattingUtils.formatXadiaTagIcon(tags.get(1)));
    }

    @Test
    public void cacheSeparatesKeysAndFiltersAndReturnsCopies() throws Exception {
        AtomicInteger calls = new AtomicInteger();
        FakeHttpURLConnection connection = connection(200, RESPONSE);
        XadiaApi api = api(connection, calls);
        api.fetchXadiaTags(null, "Player", "key", false).clear();
        Assert.assertEquals(3, api.fetchXadiaTags(null, "player", "key", false).size());
        Assert.assertEquals(1, calls.get());
        api.fetchXadiaTags(null, "player", "key", true);
        Assert.assertEquals(2, calls.get());
        api.fetchXadiaTags(null, "player", "other-key", true);
        Assert.assertEquals(3, calls.get());
        api.clearPlayer(null, "PLAYER");
        api.fetchXadiaTags(null, "player", "key", false);
        Assert.assertEquals(4, calls.get());
        api.clearCache();
        api.fetchXadiaTags(null, "player", "key", false);
        Assert.assertEquals(5, calls.get());
    }

    @Test
    public void unknownPlayersAndEmptyTagsAreValidResults() throws Exception {
        for (String body : Arrays.asList("{\"results\":[{\"found\":false}]}",
            "{\"results\":[{\"found\":true,\"tags\":[]}]}")) {
            Assert.assertTrue(api(connection(200, body), new AtomicInteger())
                .fetchXadiaTags(null, "player", "key", false).isEmpty());
        }
    }

    @Test
    public void errorsAreRetriedAfterInvalidationInsteadOfReportedAsEmpty() throws Exception {
        for (int status : new int[] {401, 403, 429, 502, 200}) {
            AtomicInteger calls = new AtomicInteger();
            XadiaApi api = api(connection(status, "{\"results\":[]}"), calls);
            for (int i = 0; i < 2; i++) {
                try {
                    api.fetchXadiaTags(null, "player", "key", false);
                    Assert.fail("Expected an IOException");
                } catch (IOException expected) {
                    if (status != 200) Assert.assertTrue(expected.getMessage().contains(Integer.toString(status)));
                }
            }
            Assert.assertEquals(1, calls.get());
            api.clearCache();
            try { api.fetchXadiaTags(null, "player", "key", false); Assert.fail(); }
            catch (IOException expected) {}
            Assert.assertEquals(2, calls.get());
        }
    }

    @Test
    public void missingKeyDoesNotMakeRequest() throws Exception {
        AtomicInteger calls = new AtomicInteger();
        try {
            api(connection(200, RESPONSE), calls).fetchXadiaTags(null, "player", " ", false);
            Assert.fail("Expected missing key error");
        } catch (IOException expected) {
            Assert.assertEquals(0, calls.get());
        }
    }

    @Test
    public void profileAndTabCopiesKeepXadiaTags() {
        XadiaTag tag = new XadiaTag("sniper", "Sniper", null, true);
        PlayerProfile original = PlayerProfile.identity("uuid", "player");
        PlayerProfile tagged = original.withTags(TagReport.nativeTags(null, Arrays.asList(tag), java.util.Collections.emptyMap()));
        Assert.assertFalse(original.getTags().has("Xadia"));
        Assert.assertTrue(tagged.getTags().has("Xadia"));
        Assert.assertTrue(tagged.getTabStats().getTags().has("Xadia"));
        Assert.assertEquals("sniper", tagged.getTabStats().getTags().getTags().get(0).getType());
    }
}
