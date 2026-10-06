package com.roxiun.mellow.api.seraph;

import com.roxiun.mellow.support.FakeHttpURLConnection;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.roxiun.mellow.Mellow;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class SeraphApiTest {

    @Before
    public void resetLimiterBeforeTest() {
        SeraphRequestLimiter.getInstance().resetForTests();
    }

    @After
    public void resetLimiterAfterTest() {
        SeraphRequestLimiter.getInstance().resetForTests();
    }

    @Test
    public void submitBlacklistReportUsesExpectedHeadersAndPayload()
        throws Exception {
        FakeHttpURLConnection connection = new FakeHttpURLConnection(
            new URL("https://api.seraph.si/addsniper"),
            200,
            "{\"success\":true}",
            null
        );

        SeraphApi api = new SeraphApi(null) {
            @Override
            protected HttpURLConnection openConnection(URL url) {
                Assert.assertEquals(
                    "https://api.seraph.si/addsniper",
                    url.toString()
                );
                return connection;
            }
        };

        SeraphApi.BlacklistSubmissionResult result = api.submitBlacklistReport(
            "00000000-0000-0000-0000-000000000123",
            "secret-key",
            SeraphBlacklistReportType.SNIPING,
            "queue sniping"
        );

        Assert.assertTrue(result.isSuccess());
        Assert.assertEquals(200, result.getStatusCode());
        Assert.assertEquals("POST", connection.getRequestMethod());
        Assert.assertTrue(connection.getDoOutput());
        Assert.assertEquals(
            "secret-key",
            connection.getRequestProperty("seraph-api-key")
        );
        Assert.assertEquals(
            "application/json; charset=UTF-8",
            connection.getRequestProperty("Content-Type")
        );
        Assert.assertEquals(
            Mellow.NAME + "/" + Mellow.VERSION,
            connection.getRequestProperty("User-Agent")
        );

        JsonObject payload = new JsonParser()
            .parse(connection.getWrittenBody())
            .getAsJsonObject();
        Assert.assertEquals(
            "00000000-0000-0000-0000-000000000123",
            payload.get("uuid").getAsString()
        );
        Assert.assertEquals("sniping", payload.get("report_type").getAsString());
        Assert.assertEquals("queue sniping", payload.get("reason").getAsString());
    }

    @Test
    public void fetchClientTypeCachesResolvedMissingResponses() throws Exception {
        AtomicInteger openedConnections = new AtomicInteger();
        FakeHttpURLConnection connection = new FakeHttpURLConnection(
            new URL("https://api.seraph.si/private-access/client"),
            404,
            null,
            null
        );

        SeraphApi api = new SeraphApi(null) {
            @Override
            protected HttpURLConnection openConnection(URL url) {
                Assert.assertEquals(
                    "https://api.seraph.si/private-access/client/" +
                    "00000000-0000-0000-0000-000000000001",
                    url.toString()
                );
                openedConnections.incrementAndGet();
                return connection;
            }
        };

        SeraphApi.ClientTypeLookupResult first = api.fetchClientTypeResult(
            "00000000000000000000000000000001",
            "secret-key"
        );
        SeraphApi.ClientTypeLookupResult second = api.fetchClientTypeResult(
            "00000000000000000000000000000001",
            "secret-key"
        );

        Assert.assertTrue(first.isResolved());
        Assert.assertNull(first.getClientType());
        Assert.assertTrue(second.isResolved());
        Assert.assertNull(second.getClientType());
        Assert.assertEquals(1, openedConnections.get());
        Assert.assertEquals(
            "secret-key",
            connection.getRequestProperty("seraph-api-key")
        );
    }

    @Test
    public void fetchClientTypeBacksOffAfterTransientFailures() throws Exception {
        AtomicInteger openedConnections = new AtomicInteger();

        SeraphApi api = new SeraphApi(null) {
            @Override
            protected HttpURLConnection openConnection(URL url) throws IOException {
                openedConnections.incrementAndGet();
                return new FakeHttpURLConnection(url, 429, null, null);
            }
        };

        SeraphApi.ClientTypeLookupResult first = api.fetchClientTypeResult(
            "00000000000000000000000000000002",
            "secret-key"
        );
        SeraphApi.ClientTypeLookupResult second = api.fetchClientTypeResult(
            "00000000000000000000000000000002",
            "secret-key"
        );

        Assert.assertFalse(first.isResolved());
        Assert.assertNull(first.getClientType());
        Assert.assertFalse(second.isResolved());
        Assert.assertNull(second.getClientType());
        Assert.assertTrue(first.getRetryAfterMillis() > 0L);
        Assert.assertTrue(second.getRetryAfterMillis() > 0L);
        Assert.assertEquals(1, openedConnections.get());
    }

    @Test
    public void fetchTagsUsesHeaderAuthenticationAndCanonicalUuidPath()
        throws Exception {
        FakeHttpURLConnection connection = new FakeHttpURLConnection(
            new URL("https://api.seraph.si/cubelify/blacklist"),
            200,
            "{\"tags\":[{" +
            "\"tag_name\":\"seraph.sniper\"," +
            "\"tooltip\":\"Sniper\"," +
            "\"color\":123}]} ",
            null
        );

        SeraphApi api = new SeraphApi(null) {
            @Override
            protected HttpURLConnection openConnection(URL url) {
                Assert.assertEquals(
                    "https://api.seraph.si/cubelify/blacklist/" +
                    "00000000-0000-0000-0000-000000000003",
                    url.toString()
                );
                return connection;
            }
        };

        Assert.assertEquals(
            1,
            api
                .fetchSeraphTags(
                    "00000000000000000000000000000003",
                    "secret-key"
                )
                .size()
        );
        Assert.assertEquals(
            "secret-key",
            connection.getRequestProperty("seraph-api-key")
        );
        Assert.assertEquals(
            Mellow.NAME + "/" + Mellow.VERSION,
            connection.getRequestProperty("User-Agent")
        );
        Assert.assertEquals(
            "application/json",
            connection.getRequestProperty("Accept")
        );
    }

    @Test
    public void fetchTagsRejectsMissingKeyWithoutOpeningConnection() {
        AtomicInteger openedConnections = new AtomicInteger();
        SeraphApi api = new SeraphApi(null) {
            @Override
            protected HttpURLConnection openConnection(URL url) {
                openedConnections.incrementAndGet();
                return new FakeHttpURLConnection(url, 401, null, null);
            }
        };

        try {
            api.fetchSeraphTags(
                "00000000-0000-0000-0000-000000000003",
                ""
            );
            Assert.fail("Expected the missing API key to be rejected.");
        } catch (IOException e) {
            Assert.assertTrue(e.getMessage().contains("API key"));
        }
        Assert.assertEquals(0, openedConnections.get());
    }

    @Test
    public void fetchTagsPropagatesTransientFailures() throws Exception {
        AtomicInteger openedConnections = new AtomicInteger();
        SeraphApi api = new SeraphApi(null) {
            @Override
            protected HttpURLConnection openConnection(URL url) {
                openedConnections.incrementAndGet();
                return new FakeHttpURLConnection(url, 503, null, null);
            }
        };

        try {
            api.fetchSeraphTags(
                "00000000-0000-0000-0000-000000000004",
                "secret-key"
            );
            Assert.fail("Expected a transient Seraph failure to propagate.");
        } catch (IOException e) {
            Assert.assertTrue(e.getMessage().contains("503"));
        }

        try {
            api.fetchSeraphTags(
                "00000000-0000-0000-0000-000000000004",
                "secret-key"
            );
            Assert.fail("Expected the Seraph tag cooldown to reject the retry.");
        } catch (IOException e) {
            Assert.assertTrue(e.getMessage().contains("cooling down"));
        }
        Assert.assertEquals(1, openedConnections.get());
    }
}
