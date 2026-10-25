package com.roxiun.mellow.api.watcher;

import com.roxiun.mellow.api.tags.*;
import com.roxiun.mellow.api.xadia.XadiaTag;
import com.roxiun.mellow.data.PlayerProfile;
import com.roxiun.mellow.support.FakeHttpURLConnection;
import java.io.IOException;
import java.net.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.Test;
import static org.junit.Assert.*;

public class WatcherApiTest {
    private static final String RESPONSE = "{\"score\":{\"value\":5,\"mode\":\"add\"},\"tags\":["
        + "{\"icon\":\"mdi-account-cancel\",\"text\":\"BLATANT\",\"color\":16711680,"
        + "\"tooltip\":\"Blatant Cheater (Added by moderator)\\n- killaura, reach\"},"
        + "{\"text\":\"SNIPER\",\"color\":11141120,\"tooltip\":\"Sniper\"}]}";

    private static final class Api extends WatcherApi {
        int calls, status = 200;
        String response = RESPONSE;
        URL url;
        FakeHttpURLConnection connection;
        Api(AtomicLong time) { super(time::get); }
        @Override protected HttpURLConnection openConnection(URL url) {
            calls++;
            this.url = url;
            return connection = new FakeHttpURLConnection(url, status, response, "{\"error\":true}");
        }
    }

    @Test public void queryAuthenticationNormalizesUuidAndPreservesChatAndTabPresentation() throws Exception {
        Api api = new Api(new AtomicLong());
        List<PlayerTag> tags = api.fetchTags("A3F81C5D-72E6-4B09-B8D1-C4E7F2096A3B", "Player", " key+& ");
        assertEquals("https://api.watchertag.com/api/v1/cubelify?key=key%2B%26&uuid=a3f81c5d72e64b09b8d1c4e7f2096a3b", api.url.toString());
        assertEquals("GET", api.connection.getRequestMethod());
        assertNull(api.connection.getRequestProperty("X-API-Key"));
        assertEquals("§8[§4BC§8]§r", tags.get(0).getIcon());
        assertEquals("§8[§4S§8]§r", tags.get(1).getIcon());
        assertTrue(tags.get(0).getText().startsWith("§4§lBLATANT§r §7("));
        assertTrue(tags.get(0).getText().contains("killaura, reach"));
        assertFalse(tags.get(0).getText().contains("\n"));
        TagReport report = TagReport.nativeTags(null,
            Collections.singletonList(new XadiaTag("sniper", "Sniper", "Evidence", true)), Collections.emptyMap()).withTags(tags);
        assertEquals("§d§lXadia§r§d: §4§lSniper§r §a[Verified] §7(Evidence)", report.messages().get(0));
        assertTrue(report.messages().get(1).startsWith("§b§lWatcher§r§b: §4§lBLATANT"));
        assertEquals(tags.get(0).getIcon(), PlayerProfile.identity("uuid", "Player").withTags(report)
            .getTabStats().getTags().from("Watcher").get(0).getIcon());
    }

    @Test public void individualAndRosterLookupsShareCacheAndInvalidation() throws Exception {
        Api api = new Api(new AtomicLong());
        api.fetchTags("ABC", null, "key").clear();
        assertEquals(2, api.fetchPlayers(Collections.singleton("abc"), "key").get("abc").getValue().size());
        assertEquals(1, api.calls);
        api.clearPlayer("ABC", null);
        api.fetchTags("abc", null, "key");
        api.fetchTags("abc", null, "another-key");
        assertEquals(3, api.calls);
        api.clearCache();
        api.fetchTags("abc", null, "key");
        assertEquals(4, api.calls);
        api.fetchTags("ERROR", "Player Name", "key");
        assertTrue(api.url.toString().endsWith("&ign=player+name"));
    }

    @Test public void missingKeysErrorsAndMalformedResponsesAreNeverCleanPlayers() throws Exception {
        Api api = new Api(new AtomicLong());
        expectFailure(api, "uuid", " ");
        assertEquals(0, api.calls);
        for (int status : new int[]{401, 403, 404, 429, 500, 503}) {
            api = new Api(new AtomicLong()); api.status = status;
            assertTrue(expectFailure(api, "uuid", "secret").contains(Integer.toString(status)));
            assertFalse(api.fetchPlayers(Collections.singleton("uuid"), "secret").get("uuid").isSuccess());
            assertEquals(1, api.calls);
        }
        for (String body : new String[]{"{}", "not json", "{\"tags\":[{}]}", "{\"error\":true,\"tags\":[]}"}) {
            api = new Api(new AtomicLong()); api.response = body;
            expectFailure(api, "uuid", "key");
        }
        api = new Api(new AtomicLong()); api.response = "{\"tags\":[]}";
        assertTrue(api.fetchTags("uuid", null, "key").isEmpty());
    }

    @Test public void rollingQuotaAndServerCooldownSurviveCacheClearsWithoutBlocking() throws Exception {
        AtomicLong time = new AtomicLong();
        Api api = new Api(time); api.response = "{\"tags\":[]}";
        for (int i = 0; i < 60; i++) api.fetchTags("player" + i, null, "key");
        api.clearCache();
        expectFailure(api, "sixty-first", "key");
        assertEquals(60, api.calls);
        time.set(60_001);
        api.fetchTags("next", null, "key");
        assertEquals(61, api.calls);
        api.status = 429;
        expectFailure(api, "server-limited", "key");
        api.clearCache();
        expectFailure(api, "another", "key");
        assertEquals(62, api.calls);
        api.status = 200;
        api.fetchTags("another", null, "other-key");
        time.addAndGet(60_001);
        api.clearCache();
        api.fetchTags("another", null, "key");
        assertEquals(64, api.calls);
    }

    @Test public void partialFailuresKeepOtherProvidersAndRetainOnlyWatchersPreviousBadges() throws Exception {
        Api api = new Api(new AtomicLong());
        TagReport previous = TagReport.empty().withTags(api.fetchTags("uuid", null, "key"));
        TagReport update = TagReport.nativeTags(null,
            Collections.singletonList(new XadiaTag("sniper", "Sniper", null, true)),
            Collections.singletonMap("Watcher", "HTTP 429"));
        TagReport merged = update.retainFailedSources(previous);
        assertEquals(2, merged.from("Watcher").size());
        assertEquals(1, merged.from("Xadia").size());
        assertTrue(merged.messages().get(3).contains("Watcher unavailable"));
        assertFalse(TagReport.empty().retainFailedSources(previous).has("Watcher"));
    }

    private static String expectFailure(WatcherApi api, String id, String key) throws Exception {
        try { api.fetchTags(id, null, key); fail("Expected provider failure"); }
        catch (IOException error) { assertFalse(error.getMessage().contains("secret")); return error.getMessage(); }
        return "";
    }
}
