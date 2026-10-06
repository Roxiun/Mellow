package com.roxiun.mellow.api.aurora;

import static com.roxiun.mellow.support.HttpResponses.response;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicReference;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import org.junit.Assert;
import org.junit.Test;

public class AuroraWinstreakServiceTest {

    @Test
    public void fetchWinstreakDoesNotSendAnApiKey() throws IOException {
        AtomicReference<Request> capturedRequest = new AtomicReference<>();
        OkHttpClient client = new OkHttpClient.Builder()
            .addInterceptor(chain -> {
                capturedRequest.set(chain.request());
                return response(chain.request(), "{\"winstreak\":42}");
            })
            .build();

        AuroraWinstreakService service = new AuroraWinstreakService(client);

        Assert.assertEquals(42, service.fetchWinstreakBlocking("compact-uuid"));
        Assert.assertEquals(
            "compact-uuid",
            capturedRequest.get().url().queryParameter("uuid")
        );
        Assert.assertNull(capturedRequest.get().url().queryParameter("key"));
    }
}
