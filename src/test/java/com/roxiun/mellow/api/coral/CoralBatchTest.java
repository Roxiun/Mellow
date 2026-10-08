package com.roxiun.mellow.api.coral;

import com.roxiun.mellow.support.FakeHttpURLConnection;
import com.roxiun.mellow.api.model.ProviderResult;
import java.net.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.Test;
import static org.junit.Assert.*;

public class CoralBatchTest {
    @Test public void batchSharesSingleLookupCacheAndDistinguishesOmittedPlayers() throws Exception {
        String first = "069a79f444e94726a5befca90e38aaf5", missing = "12345678123442348234123456789abc";
        AtomicInteger calls = new AtomicInteger();
        FakeHttpURLConnection connection = new FakeHttpURLConnection(new URL("https://example.test"), 200,
            "{\"players\":{\"" + first + "\":[]}}");
        CoralApi api = new CoralApi() {
            @Override protected HttpURLConnection openConnection(URL url) { calls.incrementAndGet(); return connection; }
        };
        Map<String, ProviderResult<List<CoralTag>>> results = api.fetchBatch(new LinkedHashSet<>(Arrays.asList(first, missing)), "key");
        assertTrue(results.get(first).isSuccess());
        assertFalse(results.get(missing).isSuccess());
        assertTrue(api.fetchCoralTags(first, null, "key").isEmpty());
        assertEquals(1, calls.get());
        assertTrue(connection.getWrittenBody().contains(first));
        assertTrue(connection.getWrittenBody().contains(missing));
    }
}
