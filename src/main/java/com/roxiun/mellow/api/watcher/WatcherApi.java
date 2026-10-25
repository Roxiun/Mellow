package com.roxiun.mellow.api.watcher;

import com.google.gson.JsonObject;
import com.roxiun.mellow.api.model.ProviderResult;
import com.roxiun.mellow.api.tags.*;
import com.roxiun.mellow.util.formatting.FormattingUtils;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.*;
import java.util.function.LongSupplier;

/** Watcher's documented Cubelify API supports individual UUID or name lookups, not batches. */
public class WatcherApi {
    private final TagRequests<PlayerTag> requests = new TagRequests<>();
    private final Map<String, Window> windows = new LinkedHashMap<>();
    private final LongSupplier clock;

    public WatcherApi() { this(System::currentTimeMillis); }
    WatcherApi(LongSupplier clock) { this.clock = clock; }

    public List<PlayerTag> fetchTags(String uuid, String name, String key) throws IOException {
        String apiKey = key == null ? "" : key.trim();
        if (apiKey.isEmpty()) throw new IOException("A Watcher API key is required.");
        String id = normalize(uuid);
        boolean useUuid = !id.isEmpty() && !"error".equals(id);
        if (!useUuid) id = normalize(name);
        if (id.isEmpty()) throw new IOException("A player UUID or username is required.");
        final String identifier = id;
        return new ArrayList<>(requests.get(id + "|" + apiKey, () -> {
            reserve(apiKey);
            URL url = new URL("https://api.watchertag.com/api/v1/cubelify?key="
                + URLEncoder.encode(apiKey, "UTF-8") + (useUuid ? "&uuid=" : "&ign=")
                + URLEncoder.encode(identifier, "UTF-8"));
            String body;
            try {
                body = TagHttp.request(openConnection(url), "Watcher", null, null);
            } catch (TagHttp.HttpFailure error) {
                if (error.getStatus() == 429) coolDown(apiKey);
                throw error;
            } catch (IOException error) {
                throw new IOException("Watcher API connection failed.");
            }
            ProviderResult<List<PlayerTag>> result = CubelifyParser.parse(body, MAPPING);
            if (!result.isSuccess()) throw new IOException("Invalid Watcher tag response.");
            return result.getValue();
        }));
    }

    public Map<String, ProviderResult<List<PlayerTag>>> fetchPlayers(Set<String> uuids, String key) {
        Map<String, ProviderResult<List<PlayerTag>>> results = new LinkedHashMap<>();
        for (String uuid : uuids) {
            String id = normalize(uuid);
            try { results.put(id, ProviderResult.success(fetchTags(id, null, key))); }
            catch (IOException error) { results.put(id, ProviderResult.failure(error.getMessage())); }
        }
        return results;
    }

    protected HttpURLConnection openConnection(URL url) throws IOException {
        return (HttpURLConnection) url.openConnection();
    }

    public void clearCache() { requests.clear(); }
    public void clearPlayer(String uuid, String name) {
        String id = normalize(uuid), ign = normalize(name);
        requests.removeMatching(key -> (!id.isEmpty() && key.startsWith(id + "|"))
            || (!ign.isEmpty() && key.startsWith(ign + "|")));
    }

    // A rolling minute per key; fail fast rather than block command/roster workers.
    // Cache invalidation must not reset this API quota or a server cooldown.
    private synchronized void reserve(String key) throws IOException {
        long now = clock.getAsLong();
        Window window = windows.computeIfAbsent(key, ignored -> new Window());
        while (windows.size() > 8) windows.remove(windows.keySet().iterator().next());
        while (!window.requests.isEmpty() && now - window.requests.peekFirst() >= 60_000L)
            window.requests.removeFirst();
        if (now < window.retryAt || window.requests.size() >= 60)
            throw new IOException("Watcher rate limit reached; try again shortly.");
        window.requests.addLast(now);
    }

    private synchronized void coolDown(String key) {
        Window window = windows.get(key);
        if (window != null) window.retryAt = clock.getAsLong() + 60_000L;
    }

    private static String normalize(String value) {
        String normalized = value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
        return normalized.matches("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}")
            ? normalized.replace("-", "") : normalized;
    }

    private static final class Window {
        final Deque<Long> requests = new ArrayDeque<>();
        long retryAt;
    }

    private static final CubelifyParser.Mapping MAPPING = new CubelifyParser.Mapping() {
        public String error(JsonObject response) {
            return response.has("error") && !response.get("error").isJsonNull()
                && !"false".equals(response.get("error").getAsString()) ? "Watcher error" : null;
        }
        public PlayerTag tag(JsonObject tag) {
            String label = clean(tag.get("text").getAsString());
            if (label.isEmpty()) throw new IllegalArgumentException("Empty Watcher tag");
            String type = label.toLowerCase(Locale.ROOT);
            String icon;
            switch (type) {
                case "blatant": case "bc": icon = "BC"; break;
                case "closet": case "cc": icon = "CC"; break;
                case "boosting": case "booster": icon = "B"; break;
                case "sniper": icon = "S"; break;
                default: icon = label;
            }
            String color = FormattingUtils.legacyColor(tag.get("color").getAsInt());
            String detail = tag.has("tooltip") && !tag.get("tooltip").isJsonNull()
                ? clean(tag.get("tooltip").getAsString()) : "";
            return new PlayerTag("Watcher", type,
                FormattingUtils.formatTagLabel(label, color) + (detail.isEmpty() ? "" : " §7(" + detail + ")"),
                FormattingUtils.formatTagBadge(icon, color), true);
        }
    };

    private static String clean(String value) {
        return value.replaceAll("§.", "").replaceAll("[\\s\\p{Cntrl}]+", " ").trim();
    }
}
