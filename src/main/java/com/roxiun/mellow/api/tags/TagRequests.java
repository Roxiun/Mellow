package com.roxiun.mellow.api.tags;

import com.roxiun.mellow.api.model.ProviderResult;
import com.roxiun.mellow.util.cache.RequestCache;
import java.io.IOException;
import java.util.List;
import java.util.function.Predicate;

/** Shared freshness and failure handling for native and Cubelify tag adapters. */
public final class TagRequests<T> {
    private final RequestCache<String, ProviderResult<List<T>>> cache =
        new RequestCache<>(2048, 300_000L, 10_000L, ProviderResult::isSuccess);
    public interface Fetch<T> { List<T> get() throws IOException; }
    public List<T> get(String key, Fetch<T> fetch) throws IOException {
        ProviderResult<List<T>> result = cache.get(key, () -> {
            try { return ProviderResult.success(fetch.get()); }
            catch (IOException e) { return ProviderResult.failure(e.getMessage()); }
        });
        if (!result.isSuccess()) throw new IOException(result.getError());
        return result.getValue();
    }
    public interface Batch<T> { java.util.Map<String, List<T>> get(java.util.Set<String> ids) throws IOException; }
    public java.util.Map<String, ProviderResult<List<T>>> getAll(java.util.Set<String> ids, String settings, Batch<T> fetch) {
        java.util.Set<String> keys = new java.util.LinkedHashSet<>();
        for (String id : ids) keys.add(id + "|" + settings);
        java.util.Map<String, ProviderResult<List<T>>> cached = cache.getAll(keys, missing -> {
            java.util.Set<String> players = new java.util.LinkedHashSet<>();
            for (String key : missing) players.add(key.substring(0, key.indexOf('|')));
            java.util.Map<String, ProviderResult<List<T>>> result = new java.util.LinkedHashMap<>();
            try {
                java.util.Map<String, List<T>> values = fetch.get(players);
                for (String id : players) result.put(id + "|" + settings, values.containsKey(id)
                    ? ProviderResult.success(values.get(id)) : ProviderResult.failure("Provider omitted player " + id));
            } catch (IOException e) {
                for (String key : missing) result.put(key, ProviderResult.failure(e.getMessage()));
            }
            return result;
        });
        java.util.Map<String, ProviderResult<List<T>>> result = new java.util.LinkedHashMap<>();
        for (String id : ids) result.put(id, cached.get(id + "|" + settings));
        return result;
    }
    public void clear() { cache.clear(); }
    public void removeMatching(Predicate<String> match) { cache.removeMatching(match); }
}
