package com.roxiun.mellow.util.cache;

import java.util.concurrent.*;
import org.junit.Test;
import static org.junit.Assert.*;

public class RequestCacheTest {
    @Test public void invalidationDetachesPendingWrite() throws Exception {
        RequestCache<String, String> cache = new RequestCache<>(10, 120000, 1000, v -> true);
        CountDownLatch started = new CountDownLatch(1), finish = new CountDownLatch(1);
        ExecutorService executor = Executors.newSingleThreadExecutor();
        try {
            Future<String> old = executor.submit(() -> cache.get("uuid", () -> {
                started.countDown();
                try { finish.await(2, TimeUnit.SECONDS); } catch (InterruptedException e) { throw new RuntimeException(e); }
                return "old";
            }));
            assertTrue(started.await(2, TimeUnit.SECONDS));
            cache.clear();
            assertEquals("new", cache.get("uuid", () -> "new"));
            finish.countDown();
            assertEquals("old", old.get(2, TimeUnit.SECONDS));
            assertEquals("new", cache.get("uuid", () -> { throw new AssertionError("unexpected request"); }));
        } finally { finish.countDown(); executor.shutdownNow(); }
    }
    @Test public void failuresExpireAndSizeIsBounded() {
        RequestCache<String, String> cache = new RequestCache<>(1, 120000, 0, v -> !v.equals("failed"));
        assertEquals("failed", cache.get("a", () -> "failed"));
        assertEquals("recovered", cache.get("a", () -> "recovered"));
        cache.get("b", () -> "other");
        assertEquals("evicted", cache.get("a", () -> "evicted"));
    }
    @Test public void concurrentSingleLookupSharesPendingBatch() throws Exception {
        RequestCache<String, String> cache = new RequestCache<>(10, 120000, 1000, value -> true);
        CountDownLatch started = new CountDownLatch(1), release = new CountDownLatch(1);
        java.util.concurrent.atomic.AtomicInteger requests = new java.util.concurrent.atomic.AtomicInteger();
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<?> batch = executor.submit(() -> cache.getAll(java.util.Collections.singleton("a"), keys -> {
                requests.incrementAndGet(); started.countDown();
                try { release.await(2, TimeUnit.SECONDS); } catch (InterruptedException e) { throw new RuntimeException(e); }
                return java.util.Collections.singletonMap("a", "shared");
            }));
            assertTrue(started.await(2, TimeUnit.SECONDS));
            Future<String> single = executor.submit(() -> cache.get("a", () -> { requests.incrementAndGet(); return "duplicate"; }));
            release.countDown();
            batch.get(2, TimeUnit.SECONDS);
            assertEquals("shared", single.get(2, TimeUnit.SECONDS));
            assertEquals(1, requests.get());
        } finally { release.countDown(); executor.shutdownNow(); }
    }
}
