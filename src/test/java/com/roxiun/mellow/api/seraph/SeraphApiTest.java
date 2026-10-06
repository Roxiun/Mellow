package com.roxiun.mellow.api.seraph;

import com.roxiun.mellow.api.mojang.MojangApi;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import org.junit.Assert;
import org.junit.Test;

public class SeraphApiTest {
    private static final String UUID = "00000000-0000-0000-0000-000000000001";

    private MojangApi offlineMojang() {
        return new MojangApi() {
            @Override
            protected HttpURLConnection openConnection(URL url) {
                throw new AssertionError("Deprecated Seraph lookup opened " + url);
            }
        };
    }

    private SeraphApi offlineSeraph() {
        return new SeraphApi(offlineMojang()) {
            @Override
            protected HttpURLConnection openConnection(URL url) {
                throw new AssertionError("Deprecated Seraph request opened " + url);
            }
        };
    }

    @Test
    public void tagsAndSubmissionsFailWithoutOpeningConnections() throws Exception {
        SeraphApi api = offlineSeraph();
        try {
            api.fetchSeraphTags(UUID, "saved-api-key");
            Assert.fail("Expected deprecation error");
        } catch (IOException expected) {
            Assert.assertEquals(SeraphAvailability.DISABLED_MESSAGE, expected.getMessage());
        }
        try {
            api.submitBlacklistReport(UUID, "saved-api-key", SeraphBlacklistReportType.SNIPING, "reason");
            Assert.fail("Expected deprecation error");
        } catch (IOException expected) {
            Assert.assertEquals(SeraphAvailability.DISABLED_MESSAGE, expected.getMessage());
        }
    }

    @Test
    public void clientAndUuidLookupsDoNotOpenConnections() {
        SeraphApi api = offlineSeraph();
        Assert.assertNull(api.fetchClientType(UUID, "saved-api-key"));
        Assert.assertFalse(api.fetchClientTypeResult(UUID, "saved-api-key").isResolved());
        Assert.assertNull(api.fetchSeraphMojang("Player"));
        Assert.assertNull(offlineMojang().fetchSeraphMojang("Player"));
    }

    @Test
    public void pingDoesNotFetchEvenWithSavedKey() throws Exception {
        SeraphPingService ping = new SeraphPingService();
        Assert.assertEquals(-1, ping.fetchPingBlocking(UUID, "saved-api-key"));
        ping.fetchAsync(UUID, "saved-api-key");
        Assert.assertEquals(-1, ping.getCachedPing(UUID));
        Assert.assertTrue(ping.tryStartFetch(UUID));
        ping.finishFetch(UUID);
    }
}
