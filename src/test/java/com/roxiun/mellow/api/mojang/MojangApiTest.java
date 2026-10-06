package com.roxiun.mellow.api.mojang;

import com.roxiun.mellow.support.FakeHttpURLConnection;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.Assert;
import org.junit.Test;

public class MojangApiTest {

    @Test
    public void fetchUuidUsesProvidersInPriorityOrderAndCachesSuccess() {
        String[] endpoints = {
            "https://mowojang.seraph.si/Notch",
            "https://mowojang.matdoes.dev/Notch",
            "https://api.minecraftservices.com/minecraft/profile/lookup/name/Notch",
            "https://api.minetools.eu/uuid/Notch"
        };
        for (int successfulProvider = 0; successfulProvider < endpoints.length; successfulProvider++) {
            final int successIndex = successfulProvider;
            AtomicInteger openedConnections = new AtomicInteger();
            MojangApi api = new MojangApi() {
                @Override
                protected HttpURLConnection openConnection(URL url) {
                    int index = openedConnections.getAndIncrement();
                    Assert.assertEquals(endpoints[index], url.toString());
                    return index == successIndex
                        ? connection(200, "{\"id\":\"069a79f444e94726a5befca90e38aaf5\",\"name\":\"Notch\"}")
                        : connection(503, null);
                }
            };
            Assert.assertEquals("069a79f444e94726a5befca90e38aaf5", api.fetchUUID("Notch"));
            Assert.assertEquals("069a79f444e94726a5befca90e38aaf5", api.fetchUUID("notch"));
            Assert.assertEquals(successIndex + 1, openedConnections.get());
        }
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
        Assert.assertEquals(3, openedConnections.get());
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
        Assert.assertEquals(4, openedConnections.get());
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
