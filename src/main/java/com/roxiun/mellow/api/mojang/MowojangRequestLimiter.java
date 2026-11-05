package com.roxiun.mellow.api.mojang;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.function.LongSupplier;

/**
 * Process-wide request budget for the Mowojang identity endpoint.
 *
 * <p>A sliding window is used instead of a fixed minute boundary so traffic
 * cannot burst twice around a boundary. The production limit deliberately
 * leaves headroom below the endpoint's 120 request/minute limit.</p>
 */
public final class MowojangRequestLimiter {

    static final int DEFAULT_MAX_REQUESTS = 100;
    static final long DEFAULT_WINDOW_MS = 60_000L;
    static final long DEFAULT_RATE_LIMIT_COOLDOWN_MS = 60_000L;

    private static final MowojangRequestLimiter INSTANCE =
        new MowojangRequestLimiter(
            DEFAULT_MAX_REQUESTS,
            DEFAULT_WINDOW_MS,
            System::currentTimeMillis
        );

    private final int maxRequests;
    private final long windowMs;
    private final LongSupplier clock;
    private final Deque<Long> requestTimes = new ArrayDeque<>();
    private long cooldownUntilMs;

    MowojangRequestLimiter(
        int maxRequests,
        long windowMs,
        LongSupplier clock
    ) {
        this.maxRequests = Math.max(1, maxRequests);
        this.windowMs = Math.max(1L, windowMs);
        this.clock = clock == null ? System::currentTimeMillis : clock;
    }

    public static MowojangRequestLimiter getInstance() {
        return INSTANCE;
    }

    public synchronized boolean tryAcquire() {
        long now = clock.getAsLong();
        if (now < cooldownUntilMs) {
            return false;
        }

        removeExpired(now);
        if (requestTimes.size() >= maxRequests) {
            return false;
        }

        requestTimes.addLast(now);
        return true;
    }

    public synchronized void recordResponse(int statusCode, String retryAfter) {
        if (statusCode != 429) {
            return;
        }

        long now = clock.getAsLong();
        long cooldownMs = parseRetryAfterMillis(retryAfter);
        cooldownUntilMs = Math.max(cooldownUntilMs, now + cooldownMs);
    }

    synchronized long getRetryAfterMillis() {
        long now = clock.getAsLong();
        if (now < cooldownUntilMs) {
            return cooldownUntilMs - now;
        }

        removeExpired(now);
        if (requestTimes.size() < maxRequests) {
            return 0L;
        }
        return Math.max(1L, windowMs - (now - requestTimes.peekFirst()));
    }

    synchronized void resetForTests() {
        requestTimes.clear();
        cooldownUntilMs = 0L;
    }

    private void removeExpired(long now) {
        while (
            !requestTimes.isEmpty() &&
            now - requestTimes.peekFirst() >= windowMs
        ) {
            requestTimes.removeFirst();
        }
    }

    private long parseRetryAfterMillis(String retryAfter) {
        if (retryAfter == null || retryAfter.trim().isEmpty()) {
            return DEFAULT_RATE_LIMIT_COOLDOWN_MS;
        }

        try {
            long seconds = Long.parseLong(retryAfter.trim());
            if (seconds <= 0L) {
                return DEFAULT_RATE_LIMIT_COOLDOWN_MS;
            }
            return Math.max(
                DEFAULT_RATE_LIMIT_COOLDOWN_MS,
                Math.min(seconds, 10L * 60L) * 1_000L
            );
        } catch (NumberFormatException ignored) {
            return DEFAULT_RATE_LIMIT_COOLDOWN_MS;
        }
    }
}
