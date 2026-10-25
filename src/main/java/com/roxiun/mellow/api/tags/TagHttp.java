package com.roxiun.mellow.api.tags;

import com.google.gson.*;
import com.roxiun.mellow.Mellow;
import java.io.*;
import java.net.HttpURLConnection;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;

/** Shared JSON transport for native and Cubelify tag endpoints. */
public final class TagHttp {
    private TagHttp() {}

    public static String request(HttpURLConnection connection, String provider, String apiKey, JsonObject body)
        throws IOException {
        try {
            connection.setRequestMethod(body == null ? "GET" : "POST");
            connection.setInstanceFollowRedirects(false);
            connection.setRequestProperty("Accept", "application/json");
            connection.setRequestProperty("User-Agent", Mellow.NAME + "/" + Mellow.VERSION);
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);
            if (apiKey != null) connection.setRequestProperty("X-API-Key", apiKey);
            if (body != null) {
                connection.setDoOutput(true);
                connection.setRequestProperty("Content-Type", "application/json");
                try (OutputStream output = connection.getOutputStream()) {
                    output.write(body.toString().getBytes(StandardCharsets.UTF_8));
                }
            }
            int status = connection.getResponseCode();
            if (status != 200) throw new HttpFailure(provider, status, errorDetail(connection, apiKey));
            return read(connection.getInputStream());
        } catch (HttpFailure error) {
            throw error;
        } catch (IOException error) {
            // URLs may contain credentials. Never include transport exception messages in chat.
            throw new IOException(provider + " API connection failed.");
        } finally {
            connection.disconnect();
        }
    }

    private static String read(InputStream input) throws IOException {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8))) {
            return reader.lines().collect(Collectors.joining("\n"));
        } catch (UncheckedIOException error) {
            throw error.getCause();
        }
    }

    private static String errorDetail(HttpURLConnection connection, String apiKey) {
        // Only native endpoints use header keys; omit untrusted details for query-auth endpoints.
        if (apiKey == null) return "";
        try {
            InputStream input = connection.getErrorStream();
            if (input == null) return "";
            JsonObject body = new JsonParser().parse(read(input)).getAsJsonObject();
            JsonElement error = body.has("message") ? body.get("message") : body.get("error");
            if (error == null || !error.isJsonPrimitive() || !error.getAsJsonPrimitive().isString()) return "";
            String detail = error.getAsString().replaceAll("§.", "").replaceAll("[\\s\\p{Cntrl}]+", " ");
            if (!apiKey.isEmpty()) detail = detail.replace(apiKey, "[redacted]");
            return ": " + detail;
        } catch (Exception ignored) { return ""; }
    }

    public static final class HttpFailure extends IOException {
        private final int status;
        HttpFailure(String provider, int status, String detail) {
            super(provider + " API request failed with response code " + status + detail);
            this.status = status;
        }
        public int getStatus() { return status; }
    }
}
