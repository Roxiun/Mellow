package com.roxiun.mellow.api.hypixel.provider;

import com.roxiun.mellow.api.model.FetchFailureReason;
import com.roxiun.mellow.api.model.ProviderResult;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Map;

public final class PlayerHttp {
    private PlayerHttp() {}
    public static ProviderResult<String> fetchPlayerDataResult(
        String urlString,
        String userAgent
    ) {
        return fetchPlayerDataResult(
            urlString,
            userAgent,
            Collections.emptyMap()
        );
    }

    public static ProviderResult<String> fetchPlayerDataResult(
        String urlString,
        String userAgent,
        Map<String, String> headers
    ) {
        HttpURLConnection connection = null;
        try {
            URL url = new URL(urlString);
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);

            if (userAgent != null) {
                connection.setRequestProperty("User-Agent", userAgent);
            }
            connection.setRequestProperty("Accept", "application/json");

            for (Map.Entry<String, String> entry : headers.entrySet()) {
                connection.setRequestProperty(entry.getKey(), entry.getValue());
            }

            int responseCode = connection.getResponseCode();
            if (responseCode != HttpURLConnection.HTTP_OK) {
                if (responseCode == 429) {
                    return ProviderResult.failure(
                        FetchFailureReason.RATE_LIMITED,
                        "HTTP 429"
                    );
                }
                return ProviderResult.failure(
                    FetchFailureReason.PROVIDER_ERROR,
                    "HTTP " + responseCode
                );
            }

            BufferedReader in = new BufferedReader(
                new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8)
            );
            StringBuilder response = new StringBuilder();
            String inputLine;

            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }
            in.close();

            return ProviderResult.success(response.toString());
        } catch (SocketTimeoutException e) {
            return ProviderResult.failure(
                FetchFailureReason.NETWORK_ERROR,
                "timeout"
            );
        } catch (IOException e) {
            return ProviderResult.failure(
                FetchFailureReason.NETWORK_ERROR,
                e.getMessage()
            );
        } catch (Exception e) {
            return ProviderResult.failure(
                FetchFailureReason.UNKNOWN,
                e.getMessage()
            );
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }
}
