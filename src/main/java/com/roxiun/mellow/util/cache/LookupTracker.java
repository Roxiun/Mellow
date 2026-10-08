package com.roxiun.mellow.util.cache;

import java.util.HashMap;
import java.util.Map;
import java.util.function.LongSupplier;

/** Lifetime-owned lookups: keep successes, allow one delayed retry, reject invalidated completions. */
public final class LookupTracker<K> {
    private static final long RETRY_DELAY_MS = 30_000L;
    private static final int MAX_ATTEMPTS = 2;

    private final Map<K, Attempt> entries = new HashMap<>();
    private final LongSupplier clock;
    public LookupTracker() { this(System::currentTimeMillis); }
    public LookupTracker(LongSupplier clock) { this.clock = clock; }
    public static final class Attempt {
        private int count;
        private long retryAt;
        private boolean pending, done;
    }
    public synchronized Attempt begin(K key) {
        Attempt previous = entries.get(key);
        if (previous != null && (previous.pending || previous.done || previous.count >= MAX_ATTEMPTS || clock.getAsLong() < previous.retryAt)) return null;
        Attempt next = new Attempt();
        next.count = previous == null ? 1 : previous.count + 1;
        next.pending = true;
        entries.put(key, next);
        return next;
    }
    public synchronized boolean finish(K key, Attempt attempt, boolean success) {
        if (entries.get(key) != attempt) return false;
        attempt.pending = false;
        attempt.done = success;
        attempt.retryAt = clock.getAsLong() + RETRY_DELAY_MS;
        return true;
    }
    public synchronized boolean finished(K key) {
        Attempt attempt = entries.get(key);
        return attempt != null && !attempt.pending && (attempt.done || attempt.count >= MAX_ATTEMPTS);
    }
    public synchronized boolean isCurrent(K key, Attempt attempt) { return entries.get(key) == attempt; }
    public synchronized void remove(K key) { entries.remove(key); }
    public synchronized void clear() { entries.clear(); }
}
