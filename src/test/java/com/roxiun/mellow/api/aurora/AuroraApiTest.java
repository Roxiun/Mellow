package com.roxiun.mellow.api.aurora;

import static com.roxiun.mellow.support.HttpResponses.response;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import org.junit.Assert;
import org.junit.Test;

public class AuroraApiTest {

    @Test
    public void queryStatsCachesSuccessfulResponsesByRequestParams() throws IOException {
        AtomicInteger requestCount = new AtomicInteger();
        OkHttpClient client = new OkHttpClient.Builder()
            .addInterceptor(chain -> {
                Request request = chain.request();
                requestCount.incrementAndGet();
                return response(
                    request,
                    200,
                    "{\"success\":true,\"data\":[{\"name\":\"Alpha\",\"distance\":0}]}"
                );
            })
            .build();

        AuroraApi api = new AuroraApi(client);

        AuroraApi.AuroraResponse first = api.queryStats(
            "beds",
            "100",
            200,
            5,
            "aurora-key"
        );
        AuroraApi.AuroraResponse second = api.queryStats(
            "beds",
            "100",
            200,
            5,
            "aurora-key"
        );

        Assert.assertNotNull(first);
        Assert.assertNotNull(second);
        Assert.assertTrue(first.success);
        Assert.assertEquals(1, first.data.size());
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

        AuroraApi api = new AuroraApi(client);

        Assert.assertNull(api.queryStats("beds", "100", 200, 5, "aurora-key"));
        Assert.assertNull(api.queryStats("beds", "100", 200, 5, "aurora-key"));
        Assert.assertEquals(1, requestCount.get());
    }
}
