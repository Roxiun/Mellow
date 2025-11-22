package com.strawberry.statsify.api;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import java.io.IOException;
import java.util.Base64;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

public class MojangApi {

    private final OkHttpClient client;
    private final Gson gson;
    private static final String UUID_API_URL =
        "https://api.mojang.com/users/profiles/minecraft/";
    private static final String SESSION_API_URL =
        "https://sessionserver.mojang.com/session/minecraft/profile/";
    private static final String MINETOOLS_API_URL =
        "https://api.minetools.eu/uuid/";

    public MojangApi(OkHttpClient client, Gson gson) {
        this.client = client;
        this.gson = gson;
    }

    public String getUUID(String username) throws IOException {
        Request request = new Request.Builder()
            .url(UUID_API_URL + username)
            .build();

        try (Response response = client.newCall(request).execute()) {
            if (response.isSuccessful()) {
                ResponseBody body = response.body();
                if (body != null) {
                    JsonObject jsonObject = gson.fromJson(
                        body.string(),
                        JsonObject.class
                    );
                    if (jsonObject != null && jsonObject.has("id")) {
                        return jsonObject.get("id").getAsString();
                    }
                }
            } else if (response.code() == 429) {
                // Rate limited, fallback to minetools
                return getUUIDFromMinetools(username);
            }
            throw new IOException(
                "Failed to get UUID for " +
                    username +
                    ". Response code: " +
                    response.code()
            );
        }
    }

    private String getUUIDFromMinetools(String username) throws IOException {
        Request request = new Request.Builder()
            .url(MINETOOLS_API_URL + username)
            .build();
        try (Response response = client.newCall(request).execute()) {
            if (response.isSuccessful()) {
                ResponseBody body = response.body();
                if (body != null) {
                    JsonObject jsonObject = gson.fromJson(
                        body.string(),
                        JsonObject.class
                    );
                    if (
                        jsonObject != null &&
                        jsonObject.has("id") &&
                        !jsonObject.get("id").isJsonNull()
                    ) {
                        return jsonObject.get("id").getAsString();
                    }
                }
            }
            throw new IOException(
                "Failed to get UUID for " +
                    username +
                    " from Minetools. Response code: " +
                    response.code()
            );
        }
    }

    public String getSkinInfo(String uuid) throws IOException {
        Request request = new Request.Builder()
            .url(SESSION_API_URL + uuid)
            .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                return null; // Or throw exception
            }
            ResponseBody body = response.body();
            if (body == null) {
                return null;
            }
            JsonObject jsonObject = gson.fromJson(
                body.string(),
                JsonObject.class
            );
            if (jsonObject != null && jsonObject.has("properties")) {
                JsonObject properties = jsonObject
                    .getAsJsonArray("properties")
                    .get(0)
                    .getAsJsonObject();
                if (properties.has("value")) {
                    String value = properties.get("value").getAsString();
                    byte[] decodedBytes = Base64.getDecoder().decode(value);
                    return new String(decodedBytes);
                }
            }
        }
        return null;
    }
}
