package com.strawberry.statsify.api;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import net.minecraft.client.Minecraft;
import net.minecraft.client.network.NetworkPlayerInfo;

public class MojangApi {

    public String fetchUUID(String username) {
        try {
            String urlString =
                "https://api.minecraftservices.com/minecraft/profile/lookup/name/" +
                username;
            HttpURLConnection connection = (HttpURLConnection) new URL(
                urlString
            ).openConnection();
            connection.setRequestMethod("GET");

            int responseCode = connection.getResponseCode();
            if (responseCode == 200) {
                BufferedReader in = new BufferedReader(
                    new InputStreamReader(connection.getInputStream())
                );
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = in.readLine()) != null) response.append(line);
                in.close();
                String uuid = extractUUID(response.toString());
                return uuid != null ? uuid : "ERROR";
            }

            if (responseCode == 404) return "ERROR";

            if (responseCode == 429) {
                // Rate limited, fallback to minetools
                urlString = "https://api.minetools.eu/uuid/" + username;
                connection = (HttpURLConnection) new URL(
                    urlString
                ).openConnection();
                connection.setRequestMethod("GET");

                BufferedReader in = new BufferedReader(
                    new InputStreamReader(connection.getInputStream())
                );
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = in.readLine()) != null) response.append(line);
                in.close();

                if (
                    response.toString().contains("\"id\": null")
                ) return "ERROR";
                String[] parts = response.toString().split("\"id\":\"");
                if (parts.length > 1) {
                    return parts[1].split("\"")[0];
                } else {
                    return "ERROR";
                }
            }
        } catch (Exception ignored) {}

        return "ERROR";
    }

    private String extractUUID(String response) {
        String[] parts = response.split("\"");
        if (response.contains("Couldn't")) {
            return "ERROR";
        }

        if (parts.length >= 5) {
            return parts[3];
        }

        return null;
    }

    public String getUUIDFromName(String playerName) {
        for (NetworkPlayerInfo info : Minecraft.getMinecraft()
            .getNetHandler()
            .getPlayerInfoMap()) {
            if (info.getGameProfile().getName().equalsIgnoreCase(playerName)) {
                return String.valueOf(info.getGameProfile().getId());
            }
        }
        return null; // Player not found (probably not in tab list)
    }
}
