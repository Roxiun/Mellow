package com.roxiun.mellow.util.cache;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;

public class TimedValueCache<K, V> {

    private final Map<K, CacheEntry<V>> entries = new ConcurrentHashMap<>();
    private final long ttlMs;

    public TimedValueCache(long ttlMs) {
        this.ttlMs = Math.max(0L, ttlMs);
    }

    public V get(K key) {
        CacheEntry<V> entry = entries.get(key);
        if (entry == null) {
            return null;
        }
        if (entry.isExpired(ttlMs)) {
            entries.remove(key, entry);
            return null;
        }
        return entry.value;
    }

    public boolean containsFresh(K key) {
        CacheEntry<V> entry = entries.get(key);
        if (entry == null) {
            return false;
        }
        if (entry.isExpired(ttlMs)) {
            entries.remove(key, entry);
            return false;
        }
        return true;
    }

    public void put(K key, V value) {
        if (key == null) {
            return;
        }
        if (entries.size() >= 4096) {
            entries.entrySet().removeIf(entry -> entry.getValue().isExpired(ttlMs));
            java.util.Iterator<K> keys = entries.keySet().iterator();
            if (entries.size() >= 4096 && keys.hasNext()) entries.remove(keys.next());
        }
        entries.put(key, new CacheEntry<>(value));
    }

    public void remove(K key) {
        if (key == null) {
            return;
        }
        entries.remove(key);
    }

    public void clear() {
        entries.clear();
    }

    public void removeMatching(Predicate<K> matcher) {
        if (matcher == null) {
            return;
        }
        entries.keySet().removeIf(matcher);
    }

    private static final class CacheEntry<V> {

        private final V value;
        private final long cachedAt;

        private CacheEntry(V value) {
            this.value = value;
            this.cachedAt = System.currentTimeMillis();
        }

        private boolean isExpired(long ttlMs) {
            return ttlMs > 0L && System.currentTimeMillis() - cachedAt > ttlMs;
        }
    }
}
