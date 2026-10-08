package com.roxiun.mellow.util.cache;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Predicate;
import java.util.function.Supplier;

/** Bounded cache with shared requests. Invalidation also detaches pending writes. */
public final class RequestCache<K, V> {
    private final Map<K, Entry<V>> entries = new LinkedHashMap<>();
    private final int capacity;
    private final long successTtl;
    private final long failureTtl;
    private final Predicate<V> successful;

    public RequestCache(int capacity, long successTtl, long failureTtl, Predicate<V> successful) {
        this.capacity = Math.max(1, capacity);
        this.successTtl = successTtl;
        this.failureTtl = failureTtl;
        this.successful = successful;
    }

    /** The winning caller performs I/O; concurrent callers share its future. Never call on the render thread. */
    public V get(K key, Supplier<V> fetch) {
        return getAll(java.util.Collections.singleton(key), missing ->
            java.util.Collections.singletonMap(key, fetch.get())).get(key);
    }

    /** Reserves cache misses together so roster requests can use provider batch endpoints. */
    public Map<K, V> getAll(java.util.Set<K> keys,
        java.util.function.Function<java.util.Set<K>, Map<K, V>> fetch) {
        Map<K, Entry<V>> selected = new LinkedHashMap<>();
        Map<K, Entry<V>> owned = new LinkedHashMap<>();
        synchronized (this) {
            for (K key : keys) {
                Entry<V> entry = entries.get(key);
                if (entry == null || entry.expiresAt <= System.currentTimeMillis()) {
                    entry = new Entry<>();
                    entries.put(key, entry);
                    owned.put(key, entry);
                }
                selected.put(key, entry);
            }
            while (entries.size() > capacity) entries.remove(entries.keySet().iterator().next());
        }
        if (!owned.isEmpty()) {
            try {
                Map<K, V> values = fetch.apply(owned.keySet());
                for (Map.Entry<K, Entry<V>> item : owned.entrySet()) {
                    V value = values.get(item.getKey());
                    if (!values.containsKey(item.getKey())) throw new IllegalStateException("Batch omitted a requested result");
                    Entry<V> entry = item.getValue();
                    entry.expiresAt = System.currentTimeMillis() + (successful.test(value) ? successTtl : failureTtl);
                    entry.future.complete(value);
                }
            } catch (Throwable error) {
                synchronized (this) {
                    for (Map.Entry<K, Entry<V>> item : owned.entrySet()) entries.remove(item.getKey(), item.getValue());
                }
                for (Entry<V> entry : owned.values()) entry.future.completeExceptionally(error);
            }
        }
        Map<K, V> result = new LinkedHashMap<>();
        for (Map.Entry<K, Entry<V>> entry : selected.entrySet()) result.put(entry.getKey(), entry.getValue().future.join());
        return result;
    }

    public synchronized void removeMatching(Predicate<K> matcher) { entries.keySet().removeIf(matcher); }
    public synchronized void clear() { entries.clear(); }

    private static final class Entry<V> {
        final CompletableFuture<V> future = new CompletableFuture<>();
        volatile long expiresAt = Long.MAX_VALUE;
    }
}
