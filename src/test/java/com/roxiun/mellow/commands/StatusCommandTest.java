package com.roxiun.mellow.commands;

import static com.roxiun.mellow.support.HttpResponses.response;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import org.junit.Assert;
import org.junit.Test;

public class StatusCommandTest {

    @Test
    public void getStatusLinesCachesOutboundResponsesButKeepsRequestedUsername() {
        AtomicInteger requestCount = new AtomicInteger();
        OkHttpClient client = new OkHttpClient.Builder()
            .addInterceptor(chain -> {
                Request request = chain.request();
                requestCount.incrementAndGet();

                String host = request.url().host();
                String path = request.url().encodedPath();
                if ("api.hypixel.net".equals(host) && "/status".equals(path)) {
                    return response(
                        request,
                        200,
                        "{\"success\":true,\"session\":{\"online\":true,\"gameType\":\"BEDWARS\",\"mode\":\"EIGHT_ONE\"}}"
                    );
                }
                if ("api.hypixel.net".equals(host) && "/player".equals(path)) {
                    return response(
                        request,
                        200,
                        "{\"success\":true,\"player\":{\"lastLogin\":1700000000000}}"
                    );
                }
                if ("lunaaaa.net".equals(host)) {
                    return response(
                        request,
                        200,
                        "{\"history\":[{\"message\":\"Queued lobby\",\"timestamp\":1700000000}]}"
                    );
                }
                return response(request, 404, "{}");
            })
            .build();

        StatusCommand command = new StatusCommand(
            null,
            "hypixel-key",
            "luna-key",
            client
        );
        UUID uuid = UUID.fromString("00000000-0000-0000-0000-000000000002");

        List<String> first = command.getStatusLines("Alpha", uuid, true, true);
        List<String> second = command.getStatusLines("Beta", uuid, true, true);

        Assert.assertEquals("§fAlpha", first.get(0));
        Assert.assertEquals("§fBeta", second.get(0));
        Assert.assertTrue(first.stream().anyMatch(line -> line.contains("§rStatus:")));
        Assert.assertTrue(
            first.stream().anyMatch(line -> line.startsWith("§7Last login: §f"))
        );
        Assert.assertTrue(
            first
                .stream()
                .anyMatch(line -> line.startsWith("§7Last lobby msg: §f\"Queued lobby\""))
        );
        Assert.assertEquals(3, requestCount.get());
    }

    @Test
    public void getStatusLinesUsesBordicWithoutApiKeys() {
        AtomicInteger requestCount = new AtomicInteger();
        OkHttpClient client = new OkHttpClient.Builder()
            .addInterceptor(chain -> {
                Request request = chain.request();
                requestCount.incrementAndGet();

                Assert.assertEquals("api.bordic.xyz", request.url().host());
                Assert.assertEquals(
                    "/v3/cache/hypixel",
                    request.url().encodedPath()
                );
                Assert.assertEquals(
                    "00000000-0000-0000-0000-000000000003",
                    request.url().queryParameter("uuid")
                );
                return response(
                    request,
                    200,
                    "{\"success\":true,\"lastUpdated\":1700000000000,\"player\":{\"displayname\":\"BordicPlayer\",\"stats\":{}}}"
                );
            })
            .build();

        StatusCommand command = new StatusCommand(
            null,
            "",
            "",
            true,
            client
        );
        UUID uuid = UUID.fromString("00000000-0000-0000-0000-000000000003");

        List<String> lines = command.getStatusLines(
            "BordicPlayer",
            uuid,
            false,
            false
        );

        Assert.assertEquals("§fBordicPlayer", lines.get(0));
        Assert.assertTrue(
            lines
                .stream()
                .anyMatch(line -> line.startsWith("§7Bordic cache updated: §f"))
        );
        Assert.assertEquals(1, requestCount.get());
    }
}
