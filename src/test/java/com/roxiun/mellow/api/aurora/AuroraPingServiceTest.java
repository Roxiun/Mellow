package com.roxiun.mellow.api.aurora;

import static com.roxiun.mellow.support.HttpResponses.response;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicReference;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import org.junit.Assert;
import org.junit.Test;

public class AuroraPingServiceTest {

    @Test
    public void fetchPingDoesNotSendAnApiKey() throws IOException {
        AtomicReference<Request> capturedRequest = new AtomicReference<>();
        OkHttpClient client = new OkHttpClient.Builder()
            .addInterceptor(chain -> {
                capturedRequest.set(chain.request());
                return response(
                    chain.request(),
                    "{\"success\":true,\"data\":[{\"avg\":87}]}"
                );
            })
            .build();

        AuroraPingService service = new AuroraPingService(client);

        Assert.assertEquals(87, service.fetchPingBlocking("compact-uuid"));
        Assert.assertEquals(
            "compact-uuid",
            capturedRequest.get().url().queryParameter("uuid")
        );
        Assert.assertNull(capturedRequest.get().url().queryParameter("key"));
    }
}
