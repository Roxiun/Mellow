package com.roxiun.mellow.commands;

import static com.roxiun.mellow.support.HttpResponses.response;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import org.junit.Assert;
import org.junit.Test;

public class NameHistoryCommandTest {

    @Test
    public void getMergedHistoryCachesOutboundRequestsPerUuid() {
        AtomicInteger requestCount = new AtomicInteger();
        OkHttpClient client = new OkHttpClient.Builder()
            .addInterceptor(chain -> {
                Request request = chain.request();
                requestCount.incrementAndGet();

                String host = request.url().host();
                if ("api.ashcon.app".equals(host)) {
                    return response(
                        request,
                        200,
                        "application/json",
                        "{\"username_history\":[{\"username\":\"CurrentName\"},{\"username\":\"OldName\",\"changed_at\":\"2020-01-01T00:00:00Z\"}]}"
                    );
                }
                if ("laby.net".equals(host)) {
                    return response(request, 200, "application/json", "{}");
                }
                if ("namemc.com".equals(host)) {
                    return response(
                        request,
                        200,
                        "text/html",
                        "<div id=\"minecraft-names\"></div>"
                    );
                }
                return response(request, 404, "application/json", "{}");
            })
            .build();

        NameHistoryCommand command = new NameHistoryCommand(null, client);
        UUID uuid = UUID.fromString("00000000-0000-0000-0000-000000000001");

        List<?> first = command.getMergedHistory(uuid);
        List<?> second = command.getMergedHistory(uuid);

        Assert.assertEquals(2, first.size());
        Assert.assertEquals(2, second.size());
        Assert.assertNotSame(first, second);
        Assert.assertEquals(3, requestCount.get());
    }
}
