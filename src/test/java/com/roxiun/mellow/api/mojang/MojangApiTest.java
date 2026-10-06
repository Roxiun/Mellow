package com.roxiun.mellow.api.mojang;

import com.roxiun.mellow.support.FakeHttpURLConnection;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayDeque;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.Assert;
import org.junit.Test;

public class MojangApiTest {

    @Test
    public void fetchUuidSkipsSeraphAndFallsBackToMinetools() {
        Queue<FakeHttpURLConnection> connections = new ArrayDeque<>();
        connections.add(connection(429, null));
        connections.add(
            connection(
                200,
                "{\"id\":\"069a79f444e94726a5befca90e38aaf5\"," +
                "\"name\":\"Notch\"}"
            )
        );

        MojangApi api = new MojangApi() {
            private int requestIndex;

            @Override
            protected HttpURLConnection openConnection(URL url) {
                if (requestIndex++ == 0) {
                    Assert.assertEquals(
                        "https://api.minecraftservices.com/minecraft/" +
                        "profile/lookup/name/Notch",
                        url.toString()
                    );
                } else {
                    Assert.assertEquals(
                        "https://api.minetools.eu/uuid/Notch",
                        url.toString()
                    );
                }
                return connections.remove();
            }
        };

        Assert.assertEquals(
            "069a79f444e94726a5befca90e38aaf5",
            api.fetchUUID("Notch")
        );
        Assert.assertTrue(connections.isEmpty());
    }

    @Test
    public void fetchSeraphMojangIsDisabled() {
        MojangApi api = new MojangApi() {
            @Override
            protected HttpURLConnection openConnection(URL url) {
                throw new AssertionError("Seraph must not be contacted");
            }
        };
        Assert.assertNull(api.fetchSeraphMojang("Notch"));
    }

    @Test
    public void fetchUuidCachesAuthoritativeNotFoundResponse() {
        AtomicInteger openedConnections = new AtomicInteger();
        MojangApi api = new MojangApi() {
            @Override
            protected HttpURLConnection openConnection(URL url) {
                openedConnections.incrementAndGet();
                return connection(404, null);
            }
        };

        Assert.assertEquals("ERROR", api.fetchUUID("MissingPlayer"));
        Assert.assertEquals("ERROR", api.fetchUUID("missingplayer"));
        Assert.assertEquals(1, openedConnections.get());
    }

    @Test
    public void fetchUuidCachesCompleteFallbackFailure() {
        AtomicInteger openedConnections = new AtomicInteger();
        MojangApi api = new MojangApi() {
            @Override
            protected HttpURLConnection openConnection(URL url) {
                openedConnections.incrementAndGet();
                return connection(503, null);
            }
        };

        Assert.assertEquals("ERROR", api.fetchUUID("UnavailablePlayer"));
        Assert.assertEquals("ERROR", api.fetchUUID("unavailableplayer"));
        Assert.assertEquals(2, openedConnections.get());
    }

    private static FakeHttpURLConnection connection(
        int responseCode,
        String responseBody
    ) {
        try {
            return new FakeHttpURLConnection(
                new URL("https://example.invalid"),
                responseCode,
                responseBody
            );
        } catch (Exception e) {
            throw new AssertionError(e);
        }
    }
}
