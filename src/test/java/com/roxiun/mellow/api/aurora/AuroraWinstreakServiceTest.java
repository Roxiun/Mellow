package com.roxiun.mellow.api.aurora;

import static com.roxiun.mellow.support.HttpResponses.response;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicReference;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import org.junit.Assert;
import org.junit.Test;

public class AuroraWinstreakServiceTest {
    @org.junit.Test public void clearPlayerDoesNotInvalidateOtherPlayersAndRejectsStaleWrites() {
        AuroraWinstreakService service = new AuroraWinstreakService();
        Object alex = service.getGeneration("alex"), sam = service.getGeneration("sam");
        service.clearPlayer("alex");
        org.junit.Assert.assertFalse(service.storeIfCurrent("alex", alex, 10));
        org.junit.Assert.assertTrue(service.storeIfCurrent("sam", sam, 20));
        org.junit.Assert.assertEquals(20, service.getCachedWinstreak("sam"));
        service.pinForMatch("sam", 20);
        service.clearCache();
        org.junit.Assert.assertFalse(service.storeIfCurrent("sam", sam, 21));
        org.junit.Assert.assertFalse(service.hasMatchWinstreak("sam"));
    }


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
