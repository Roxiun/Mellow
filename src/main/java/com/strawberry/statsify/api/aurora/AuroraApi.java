package com.strawberry.statsify.api.aurora;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;
import java.io.IOException;
import java.util.List;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

public class AuroraApi {

    private final OkHttpClient client = new OkHttpClient();
    private final Gson gson = new Gson();
    private static final String BASE_URL =
        "https://bordic.xyz/api/v2/resources/lookup/";

    public AuroraResponse queryStats(
        String type,
        String value,
        int range,
        int max,
        String apiKey
    ) throws IOException {
        if (apiKey == null || apiKey.isEmpty()) {
            return null;
        }

        String url =
            BASE_URL +
            type +
            "?key=" +
            apiKey +
            "&value=" +
            value +
            "&range=" +
            range +
            "&max=" +
            max;

        Request request = new Request.Builder()
            .url(url)
            .header("User-Agent", "Statsify/4.1.0")
            .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                // Log the error or handle it more gracefully
                System.err.println("Aurora API request failed: " + response);
                return null;
            }

            ResponseBody body = response.body();
            if (body == null) {
                return null;
            }

            return gson.fromJson(body.string(), AuroraResponse.class);
        }
    }

    public static class AuroraResponse {

        @SerializedName("success")
        public boolean success;

        @SerializedName("data")
        public List<PlayerMatch> data;
    }

    public static class PlayerMatch {

        @SerializedName("name")
        public String name;

        @SerializedName("distance")
        public int distance;
    }
}
