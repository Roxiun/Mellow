package com.roxiun.mellow.support;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public final class FakeHttpURLConnection extends HttpURLConnection {

    private final int responseCode;
    private final byte[] responseBody;
    private final byte[] errorBody;
    private final ByteArrayOutputStream requestBody =
        new ByteArrayOutputStream();
    private final Map<String, String> requestProperties = new HashMap<>();

    public FakeHttpURLConnection(
        URL url,
        int responseCode,
        String responseBody,
        String errorBody
    ) {
        super(url);
        this.responseCode = responseCode;
        this.responseBody = responseBody == null
            ? new byte[0]
            : responseBody.getBytes(StandardCharsets.UTF_8);
        this.errorBody = errorBody == null
            ? null
            : errorBody.getBytes(StandardCharsets.UTF_8);
    }

    public FakeHttpURLConnection(URL url, int responseCode, String responseBody) {
        this(url, responseCode, responseBody, null);
    }

    @Override
    public void disconnect() {}

    @Override
    public boolean usingProxy() {
        return false;
    }

    @Override
    public void connect() {}

    @Override
    public void setRequestProperty(String key, String value) {
        requestProperties.put(key, value);
    }

    @Override
    public String getRequestProperty(String key) {
        return requestProperties.get(key);
    }

    @Override
    public OutputStream getOutputStream() {
        return requestBody;
    }

    @Override
    public int getResponseCode() {
        return responseCode;
    }

    @Override
    public InputStream getInputStream() throws IOException {
        if (responseCode < 200 || responseCode >= 300) {
            throw new IOException("HTTP " + responseCode);
        }
        return new ByteArrayInputStream(responseBody);
    }

    @Override
    public InputStream getErrorStream() {
        if (errorBody == null) {
            return null;
        }
        return new ByteArrayInputStream(errorBody);
    }

    public String getWrittenBody() {
        return new String(requestBody.toByteArray(), StandardCharsets.UTF_8);
    }
}
