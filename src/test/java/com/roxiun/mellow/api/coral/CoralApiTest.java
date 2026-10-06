package com.roxiun.mellow.api.coral;

import com.roxiun.mellow.support.FakeHttpURLConnection;
import com.roxiun.mellow.Mellow;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.Assert;
import org.junit.Test;

public class CoralApiTest {

    @Test
    public void fetchTagsUsesV3EndpointHeaderAuthenticationAndNewSchema()
        throws Exception {
        FakeHttpURLConnection connection = new FakeHttpURLConnection(
            new URL(
                "https://api.urchin.gg/v3/player/tags?player=" +
                "00000000-0000-0000-0000-000000000001"
            ),
            200,
            "{\"uuid\":\"00000000000000000000000000000001\"," +
            "\"tags\":[{\"tag_type\":\"confirmed_cheater\"," +
            "\"reason\":\"Review confirmed\",\"added_on\":1," +
            "\"hide_username\":false,\"added_by\":42," +
            "\"added_by_username\":\"Moderator\"," +
            "\"expires_at\":null}]} ",
            null
        );

        CoralApi api = new CoralApi() {
            @Override
            protected HttpURLConnection openConnection(URL url) {
                Assert.assertEquals(connection.getURL(), url);
                return connection;
            }
        };

        List<CoralTag> tags = api.fetchCoralTags(
            "00000000-0000-0000-0000-000000000001",
            "PlayerName",
            "secret-key"
        );

        Assert.assertEquals(1, tags.size());
        Assert.assertEquals("confirmed_cheater", tags.get(0).getType());
        Assert.assertEquals("confirmed_cheater", tags.get(0).getTagType());
        Assert.assertEquals("Review confirmed", tags.get(0).getReason());
        Assert.assertEquals(1L, tags.get(0).getAddedOn());
        Assert.assertFalse(tags.get(0).isHideUsername());
        Assert.assertEquals(Long.valueOf(42L), tags.get(0).getAddedBy());
        Assert.assertEquals("Moderator", tags.get(0).getAddedByUsername());
        Assert.assertNull(tags.get(0).getExpiresAt());
        Assert.assertEquals("GET", connection.getRequestMethod());
        Assert.assertEquals(
            "secret-key",
            connection.getRequestProperty("X-API-Key")
        );
        Assert.assertEquals(
            "application/json",
            connection.getRequestProperty("Accept")
        );
        Assert.assertEquals(
            Mellow.NAME + "/" + Mellow.VERSION,
            connection.getRequestProperty("User-Agent")
        );
    }

    @Test
    public void fetchTagsUsesEncodedUsernameWhenUuidIsUnavailable()
        throws Exception {
        FakeHttpURLConnection connection = new FakeHttpURLConnection(
            new URL(
                "https://api.urchin.gg/v3/player/tags?player=player+name"
            ),
            200,
            "{\"uuid\":\"00000000000000000000000000000002\"," +
            "\"tags\":[]}",
            null
        );

        CoralApi api = new CoralApi() {
            @Override
            protected HttpURLConnection openConnection(URL url) {
                Assert.assertEquals(connection.getURL(), url);
                return connection;
            }
        };

        Assert.assertTrue(
            api.fetchCoralTags("ERROR", "Player Name", "secret-key").isEmpty()
        );
    }

    @Test
    public void fetchTagsAcceptsHiddenAuthorFieldsBeingOmitted() throws Exception {
        FakeHttpURLConnection connection = new FakeHttpURLConnection(
            new URL(
                "https://api.urchin.gg/v3/player/tags?player=playername"
            ),
            200,
            "{\"uuid\":\"00000000000000000000000000000003\"," +
            "\"tags\":[{\"tag_type\":\"caution\"," +
            "\"reason\":\"Hidden author\",\"added_on\":2," +
            "\"hide_username\":true}]}",
            null
        );
        CoralApi api = new CoralApi() {
            @Override
            protected HttpURLConnection openConnection(URL url) {
                return connection;
            }
        };

        CoralTag tag = api
            .fetchCoralTags(null, "PlayerName", "secret-key")
            .get(0);

        Assert.assertTrue(tag.isHideUsername());
        Assert.assertNull(tag.getAddedBy());
        Assert.assertNull(tag.getAddedByUsername());
        Assert.assertNull(tag.getExpiresAt());
    }

    @Test
    public void fetchTagsRequiresApiKeyBeforeOpeningConnection()
        throws Exception {
        AtomicInteger openedConnections = new AtomicInteger();
        CoralApi api = new CoralApi() {
            @Override
            protected HttpURLConnection openConnection(URL url) {
                openedConnections.incrementAndGet();
                return null;
            }
        };

        try {
            api.fetchCoralTags(null, "PlayerName", "  ");
            Assert.fail("Expected a missing Coral API key to be rejected.");
        } catch (IOException e) {
            Assert.assertTrue(e.getMessage().contains("API key"));
        }
        Assert.assertEquals(0, openedConnections.get());
    }

    @Test
    public void fetchTagsPropagatesCoralErrorDetails() throws Exception {
        FakeHttpURLConnection connection = new FakeHttpURLConnection(
            new URL(
                "https://api.urchin.gg/v3/player/tags?player=playername"
            ),
            429,
            null,
            "{\"error\":\"rate limit exceeded\"}"
        );
        CoralApi api = new CoralApi() {
            @Override
            protected HttpURLConnection openConnection(URL url) {
                return connection;
            }
        };

        try {
            api.fetchCoralTags(null, "PlayerName", "secret-key");
            Assert.fail("Expected a Coral rate-limit failure to propagate.");
        } catch (IOException e) {
            Assert.assertTrue(e.getMessage().contains("429"));
            Assert.assertTrue(e.getMessage().contains("rate limit exceeded"));
        }
    }
}
