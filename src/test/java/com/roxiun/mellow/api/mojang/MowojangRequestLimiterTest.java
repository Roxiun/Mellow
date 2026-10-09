package com.roxiun.mellow.api.mojang;

import java.util.concurrent.atomic.AtomicLong;
import org.junit.Assert;
import org.junit.Test;

public class MowojangRequestLimiterTest {

    @Test
    public void enforcesLimitAcrossSlidingWindow() {
        AtomicLong now = new AtomicLong(1_000L);
        MowojangRequestLimiter limiter = new MowojangRequestLimiter(
            3,
            60_000L,
            now::get
        );

        Assert.assertTrue(limiter.tryAcquire());
        Assert.assertTrue(limiter.tryAcquire());
        Assert.assertTrue(limiter.tryAcquire());
        Assert.assertFalse(limiter.tryAcquire());

        now.addAndGet(59_999L);
        Assert.assertFalse(limiter.tryAcquire());

        now.incrementAndGet();
        Assert.assertTrue(limiter.tryAcquire());
    }

    @Test
    public void rateLimitResponsePausesAllRequests() {
        AtomicLong now = new AtomicLong(5_000L);
        MowojangRequestLimiter limiter = new MowojangRequestLimiter(
            100,
            60_000L,
            now::get
        );

        Assert.assertTrue(limiter.tryAcquire());
        limiter.recordResponse(429, "90");
        Assert.assertFalse(limiter.tryAcquire());
        Assert.assertEquals(90_000L, limiter.getRetryAfterMillis());

        now.addAndGet(89_999L);
        Assert.assertFalse(limiter.tryAcquire());
        now.incrementAndGet();
        Assert.assertTrue(limiter.tryAcquire());
    }
}
