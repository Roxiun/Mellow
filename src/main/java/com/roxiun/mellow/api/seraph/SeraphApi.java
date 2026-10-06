package com.roxiun.mellow.api.seraph;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.roxiun.mellow.Mellow;
import com.roxiun.mellow.api.mojang.MojangApi;
import com.roxiun.mellow.util.UUIDUtils;
import com.roxiun.mellow.util.cache.TimedValueCache;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

@Deprecated
public class SeraphApi {

    private static final long CLIENT_CACHE_TTL_MS = 300_000L;
    private static final long TAG_CACHE_TTL_MS = 120_000L;
    private static final String SERAPH_API_URL = "https://api.seraph.si";
    private static final String ADD_SNIPER_URL = "https://api.seraph.si/addsniper";

    private final MojangApi mojangApi;
    private final SeraphRequestLimiter requestLimiter;
    private final TimedValueCache<String, SeraphClientType> clientTypeCache =
        new TimedValueCache<>(CLIENT_CACHE_TTL_MS);
    private final TimedValueCache<String, List<SeraphTag>> tagCache =
        new TimedValueCache<>(TAG_CACHE_TTL_MS);
    private final RequestFailureBackoff clientFailureBackoff =
        new RequestFailureBackoff();
    private final RequestFailureBackoff tagFailureBackoff =
        new RequestFailureBackoff();
    private final Map<String, CompletableFuture<ClientTypeLookupResult>>
        clientLookupsInProgress = new ConcurrentHashMap<>();
    private final Map<String, CompletableFuture<List<SeraphTag>>>
        tagLookupsInProgress = new ConcurrentHashMap<>();

    public SeraphApi(MojangApi mojangApi) {
        this(mojangApi, SeraphRequestLimiter.getInstance());
    }

    SeraphApi(
        MojangApi mojangApi,
        SeraphRequestLimiter requestLimiter
    ) {
        this.mojangApi = mojangApi;
        this.requestLimiter = requestLimiter == null
            ? SeraphRequestLimiter.getInstance()
            : requestLimiter;
    }

    public MojangApi.MojangProfile fetchSeraphMojang(String nameOrId) {
        if (!SeraphAvailability.isEnabled()) return null;
        return mojangApi == null ? null : mojangApi.fetchSeraphMojang(nameOrId);
    }

    public SeraphClientType fetchClientType(String uuid, String seraphApiKey) {
        return fetchClientTypeResult(uuid, seraphApiKey).getClientType();
    }

    public ClientTypeLookupResult fetchClientTypeResult(
        String uuid,
        String seraphApiKey
    ) {
        if (!SeraphAvailability.isEnabled()) return new ClientTypeLookupResult(null, false, 0L);
        String normalizedUuid = normalizeUuid(uuid);
        String normalizedApiKey = normalizeApiKey(seraphApiKey);
        String cacheKey = buildCacheKey(normalizedUuid, normalizedApiKey);
        if (normalizedUuid.isEmpty() || normalizedApiKey.isEmpty()) {
            return new ClientTypeLookupResult(null, false, 0L);
        }
        if (!cacheKey.isEmpty() && clientTypeCache.containsFresh(cacheKey)) {
            return new ClientTypeLookupResult(
                clientTypeCache.get(cacheKey),
                true,
                0L
            );
        }

        long retryAfterMs = clientFailureBackoff.getRetryAfterMillis(cacheKey);
        if (retryAfterMs > 0L) {
            return new ClientTypeLookupResult(null, false, retryAfterMs);
        }

        CompletableFuture<ClientTypeLookupResult> lookup =
            new CompletableFuture<>();
        CompletableFuture<ClientTypeLookupResult> existing =
            clientLookupsInProgress.putIfAbsent(cacheKey, lookup);
        if (existing != null) {
            return awaitClientLookup(existing);
        }

        ClientTypeLookupResult result;
        SeraphClientType clientType = null;
        boolean resolved = false;
        try {
            GetResponse response = executeGetRequest(
                new URL(
                    SERAPH_API_URL +
                    "/private-access/client/" +
                    normalizedUuid
                ),
                normalizedApiKey
            );
            if (response.statusCode == HttpURLConnection.HTTP_OK) {
                clientType = parseClientTag(response.body);
                resolved = true;
            } else if (
                response.statusCode == HttpURLConnection.HTTP_NOT_FOUND ||
                response.statusCode == HttpURLConnection.HTTP_NO_CONTENT
            ) {
                clientType = null;
                resolved = true;
            }
            clientFailureBackoff.recordSuccess(cacheKey);
        } catch (Exception ignored) {
            clientType = null;
            clientFailureBackoff.recordFailure(cacheKey);
        }

        if (resolved && !cacheKey.isEmpty()) {
            clientTypeCache.put(cacheKey, clientType);
        }
        result = new ClientTypeLookupResult(
            clientType,
            resolved,
            clientFailureBackoff.getRetryAfterMillis(cacheKey)
        );
        lookup.complete(result);
        clientLookupsInProgress.remove(cacheKey, lookup);
        return result;
    }

    public List<SeraphTag> fetchSeraphTags(String uuid, String seraphApiKey)
        throws IOException {
        if (!SeraphAvailability.isEnabled()) throw new IOException(SeraphAvailability.DISABLED_MESSAGE);
        String normalizedUuid = normalizeUuid(uuid);
        String normalizedApiKey = normalizeApiKey(seraphApiKey);
        if (normalizedUuid.isEmpty()) {
            throw new IOException("Invalid UUID provided.");
        }
        if (normalizedApiKey.isEmpty()) {
            throw new IOException("Missing Seraph API key.");
        }

        String cacheKey = buildCacheKey(normalizedUuid, normalizedApiKey);
        if (!cacheKey.isEmpty() && tagCache.containsFresh(cacheKey)) {
            return copyTags(tagCache.get(cacheKey));
        }

        long retryAfterMs = tagFailureBackoff.getRetryAfterMillis(cacheKey);
        if (retryAfterMs > 0L) {
            throw new IOException(
                "Seraph tag lookup is cooling down for " + retryAfterMs + "ms"
            );
        }

        CompletableFuture<List<SeraphTag>> lookup = new CompletableFuture<>();
        CompletableFuture<List<SeraphTag>> existing =
            tagLookupsInProgress.putIfAbsent(cacheKey, lookup);
        if (existing != null) {
            return awaitTagLookup(existing);
        }

        try {
            GetResponse response = executeGetRequest(
                new URL(
                    SERAPH_API_URL +
                    "/cubelify/blacklist/" +
                    normalizedUuid
                ),
                normalizedApiKey
            );
            List<SeraphTag> tags;
            if (response.statusCode == HttpURLConnection.HTTP_OK) {
                tags = parseTags(response.body);
            } else {
                tags = new ArrayList<>();
            }

            tagFailureBackoff.recordSuccess(cacheKey);
            tagCache.put(cacheKey, copyTags(tags));
            lookup.complete(copyTags(tags));
            return copyTags(tags);
        } catch (IOException e) {
            tagFailureBackoff.recordFailure(cacheKey);
            lookup.completeExceptionally(e);
            throw e;
        } finally {
            tagLookupsInProgress.remove(cacheKey, lookup);
        }
    }

    public BlacklistSubmissionResult submitBlacklistReport(
        String uuid,
        String seraphApiKey,
        SeraphBlacklistReportType reportType,
        String reason
    ) throws IOException {
        if (!SeraphAvailability.isEnabled()) throw new IOException(SeraphAvailability.DISABLED_MESSAGE);
        String normalizedUuid = normalizeUuid(uuid);
        String normalizedApiKey = normalizeApiKey(seraphApiKey);
        String normalizedReason = reason == null ? "" : reason.trim();

        if (normalizedUuid.isEmpty()) {
            throw new IOException("Invalid UUID provided.");
        }
        if (normalizedApiKey.isEmpty()) {
            throw new IOException("Missing Seraph API key.");
        }
        if (reportType == null) {
            throw new IOException("Invalid Seraph report type.");
        }
        if (normalizedReason.isEmpty()) {
            throw new IOException("Reason is required.");
        }

        acquireRequestPermit();
        HttpURLConnection conn = openConnection(new URL(ADD_SNIPER_URL));
        try {
            configureConnection(conn, "POST", normalizedApiKey);
            conn.setDoOutput(true);
            conn.setRequestProperty(
                "Content-Type",
                "application/json; charset=UTF-8"
            );

            JsonObject payload = new JsonObject();
            payload.addProperty("uuid", normalizedUuid);
            payload.addProperty("report_type", reportType.getApiValue());
            payload.addProperty("reason", normalizedReason);

            try (
                OutputStreamWriter writer = new OutputStreamWriter(
                    conn.getOutputStream(),
                    StandardCharsets.UTF_8
                )
            ) {
                writer.write(payload.toString());
            }

            int responseCode = conn.getResponseCode();
            requestLimiter.recordResponse(
                responseCode,
                conn.getHeaderField("Retry-After")
            );
            String responseBody = readResponseBody(conn, responseCode);
            boolean success = responseCode >= 200 && responseCode < 300;
            if (success) {
                clearPlayer(normalizedUuid);
            }

            return new BlacklistSubmissionResult(
                success,
                responseCode,
                responseBody
            );
        } finally {
            conn.disconnect();
        }
    }

    private List<SeraphTag> parseTags(String response) {
        List<SeraphTag> tags = new ArrayList<>();
        try {
            JsonObject json = new JsonParser()
                .parse(response)
                .getAsJsonObject();
            if (!json.has("tags") || !json.get("tags").isJsonArray()) {
                return tags;
            }

            JsonArray tagsArray = json.getAsJsonArray("tags");
            for (JsonElement tagElement : tagsArray) {
                if (!tagElement.isJsonObject()) {
                    continue;
                }

                JsonObject tagObj = tagElement.getAsJsonObject();
                String tagName = getStringOrDefault(tagObj, "tag_name", "");

                if ("seraph.advertisement".equals(tagName)) {
                    continue;
                }

                tags.add(
                    new SeraphTag(
                        getStringOrDefault(tagObj, "icon", ""),
                        getStringOrDefault(tagObj, "tooltip", ""),
                        getIntOrDefault(tagObj, "color", 0),
                        tagName,
                        getStringOrDefault(tagObj, "text", null),
                        getIntOrDefault(tagObj, "textColor", 0)
                    )
                );
            }
        } catch (Exception ignored) {}
        return tags;
    }

    private String getStringOrDefault(
        JsonObject object,
        String propertyName,
        String fallback
    ) {
        return object.has(propertyName) &&
            !object.get(propertyName).isJsonNull()
            ? object.get(propertyName).getAsString()
            : fallback;
    }

    private int getIntOrDefault(
        JsonObject object,
        String propertyName,
        int fallback
    ) {
        return object.has(propertyName) &&
            !object.get(propertyName).isJsonNull()
            ? object.get(propertyName).getAsInt()
            : fallback;
    }

    private SeraphClientType parseClientTag(String response) {
        if (response == null || response.isEmpty()) {
            return null;
        }

        SeraphClientType clientType = parseClientTypeFromJson(response);
        if (clientType != null) {
            return clientType;
        }

        String lower = response.toLowerCase();
        int statusIndex = lower.indexOf("currently on ");
        if (statusIndex < 0) {
            return null;
        }

        int start = statusIndex + "currently on ".length();
        int clientIndex = lower.indexOf(" client", start);
        if (clientIndex < 0) {
            return null;
        }

        String clientName = response.substring(start, clientIndex).trim();
        if (clientName.isEmpty()) {
            return null;
        }
        return SeraphClientType.fromDetectedName(clientName);
    }

    private SeraphClientType parseClientTypeFromJson(String response) {
        try {
            JsonObject json = new JsonParser().parse(response).getAsJsonObject();
            if (!json.has("tags") || !json.get("tags").isJsonArray()) {
                return null;
            }

            JsonArray tags = json.getAsJsonArray("tags");
            for (JsonElement element : tags) {
                if (!element.isJsonObject()) {
                    continue;
                }

                JsonObject tag = element.getAsJsonObject();
                if (tag.has("tag_name") && !tag.get("tag_name").isJsonNull()) {
                    SeraphClientType clientType = SeraphClientType.fromDetectedName(
                        tag.get("tag_name").getAsString()
                    );
                    if (clientType != null) {
                        return clientType;
                    }
                }

                if (tag.has("tooltip") && !tag.get("tooltip").isJsonNull()) {
                    SeraphClientType clientType = parseClientTag(
                        tag.get("tooltip").getAsString()
                    );
                    if (clientType != null) {
                        return clientType;
                    }
                }
            }
        } catch (Exception ignored) {}

        return null;
    }

    private String buildCacheKey(String uuid, String seraphApiKey) {
        String normalizedUuid = normalizeUuid(uuid);
        if (normalizedUuid.isEmpty()) {
            return "";
        }

        return normalizedUuid + "|" + normalizeApiKey(seraphApiKey);
    }

    private String normalizeApiKey(String apiKey) {
        return apiKey == null ? "" : apiKey.trim();
    }

    public void clearCache() {
        clientTypeCache.clear();
        tagCache.clear();
        clientFailureBackoff.clear();
        tagFailureBackoff.clear();
    }

    public void clearPlayer(String uuid) {
        String normalizedUuid = normalizeUuid(uuid);
        if (normalizedUuid.isEmpty()) {
            return;
        }

        clientTypeCache.removeMatching(key ->
            key != null && key.startsWith(normalizedUuid + "|")
        );
        tagCache.removeMatching(key ->
            key != null && key.startsWith(normalizedUuid + "|")
        );
        clientFailureBackoff.removeMatching(key ->
            key != null && key.startsWith(normalizedUuid + "|")
        );
        tagFailureBackoff.removeMatching(key ->
            key != null && key.startsWith(normalizedUuid + "|")
        );
    }

    private String normalizeUuid(String uuid) {
        if (uuid == null) {
            return "";
        }

        String normalized = uuid.trim().toLowerCase(Locale.ROOT);
        if (normalized.isEmpty() || "error".equals(normalized)) {
            return "";
        }

        try {
            return UUIDUtils.fromString(normalized).toString();
        } catch (IllegalArgumentException ignored) {
            return "";
        }
    }

    private List<SeraphTag> copyTags(List<SeraphTag> tags) {
        return tags == null ? new ArrayList<>() : new ArrayList<>(tags);
    }

    protected HttpURLConnection openConnection(URL url) throws IOException {
        return (HttpURLConnection) url.openConnection();
    }

    private GetResponse executeGetRequest(URL url, String seraphApiKey)
        throws IOException {
        acquireRequestPermit();
        HttpURLConnection conn = openConnection(url);
        try {
            configureConnection(conn, "GET", normalizeApiKey(seraphApiKey));
            int responseCode = conn.getResponseCode();
            requestLimiter.recordResponse(
                responseCode,
                conn.getHeaderField("Retry-After")
            );
            if (responseCode == HttpURLConnection.HTTP_OK) {
                return new GetResponse(
                    responseCode,
                    readResponseBody(conn, responseCode)
                );
            }
            if (
                responseCode == HttpURLConnection.HTTP_NOT_FOUND ||
                responseCode == HttpURLConnection.HTTP_NO_CONTENT
            ) {
                return new GetResponse(responseCode, "");
            }
            throw new IOException(
                "Seraph API request failed with code: " + responseCode
            );
        } finally {
            conn.disconnect();
        }
    }

    private void acquireRequestPermit() throws IOException {
        if (!SeraphAvailability.isEnabled()) throw new IOException(SeraphAvailability.DISABLED_MESSAGE);
        if (requestLimiter.tryAcquire()) {
            return;
        }

        throw new IOException(
            "Seraph request budget exhausted; retry in " +
            requestLimiter.getRetryAfterMillis() +
            "ms"
        );
    }

    private ClientTypeLookupResult awaitClientLookup(
        CompletableFuture<ClientTypeLookupResult> lookup
    ) {
        try {
            return lookup.get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return new ClientTypeLookupResult(null, false, 5_000L);
        } catch (ExecutionException e) {
            return new ClientTypeLookupResult(null, false, 5_000L);
        }
    }

    private List<SeraphTag> awaitTagLookup(
        CompletableFuture<List<SeraphTag>> lookup
    ) throws IOException {
        try {
            return copyTags(lookup.get());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Interrupted while waiting for Seraph tags", e);
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();
            if (cause instanceof IOException) {
                throw (IOException) cause;
            }
            throw new IOException("Seraph tag lookup failed", cause);
        }
    }

    private void configureConnection(
        HttpURLConnection conn,
        String method,
        String seraphApiKey
    ) throws IOException {
        conn.setRequestMethod(method);
        conn.setRequestProperty(
            "User-Agent",
            Mellow.NAME + "/" + Mellow.VERSION
        );
        conn.setRequestProperty("Accept", "application/json");
        if (seraphApiKey != null && !seraphApiKey.isEmpty()) {
            conn.setRequestProperty("seraph-api-key", seraphApiKey);
        }
        conn.setConnectTimeout(5000);
        conn.setReadTimeout(5000);
    }

    private String readResponseBody(HttpURLConnection conn, int responseCode)
        throws IOException {
        if (conn == null) {
            return "";
        }

        java.io.InputStream stream = responseCode >= 200 && responseCode < 300
            ? conn.getInputStream()
            : conn.getErrorStream();
        if (stream == null) {
            return "";
        }

        BufferedReader in = new BufferedReader(
            new InputStreamReader(stream, StandardCharsets.UTF_8)
        );
        try {
            return in.lines().collect(Collectors.joining());
        } finally {
            in.close();
        }
    }

    private static final class GetResponse {

        private final int statusCode;
        private final String body;

        private GetResponse(int statusCode, String body) {
            this.statusCode = statusCode;
            this.body = body;
        }
    }

    public static final class BlacklistSubmissionResult {

        private final boolean success;
        private final int statusCode;
        private final String responseBody;

        public BlacklistSubmissionResult(
            boolean success,
            int statusCode,
            String responseBody
        ) {
            this.success = success;
            this.statusCode = statusCode;
            this.responseBody = responseBody;
        }

        public boolean isSuccess() {
            return success;
        }

        public int getStatusCode() {
            return statusCode;
        }

        public String getResponseBody() {
            return responseBody;
        }
    }

    public static final class ClientTypeLookupResult {

        private final SeraphClientType clientType;
        private final boolean resolved;
        private final long retryAfterMillis;

        public ClientTypeLookupResult(
            SeraphClientType clientType,
            boolean resolved,
            long retryAfterMillis
        ) {
            this.clientType = clientType;
            this.resolved = resolved;
            this.retryAfterMillis = Math.max(0L, retryAfterMillis);
        }

        public SeraphClientType getClientType() {
            return clientType;
        }

        public boolean isResolved() {
            return resolved;
        }

        public long getRetryAfterMillis() {
            return retryAfterMillis;
        }
    }
}
