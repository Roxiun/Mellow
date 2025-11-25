package com.strawberry.statsify.api;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.strawberry.statsify.util.FormattingUtils;
import com.strawberry.statsify.util.PlayerUtils;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.DecimalFormat;

public class AbyssApi implements StatsProvider {

    private final MojangApi mojangApi;

    public AbyssApi(MojangApi mojangApi) {
        this.mojangApi = mojangApi;
    }

    @Override
    public String fetchPlayerData(String uuid) {
        HttpURLConnection connection = null;
        try {
            String urlString =
                "http://api.abyssoverlay.com/player?uuid=" + uuid;

            URL url = new URL(urlString);
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");

            connection.setRequestProperty("User-Agent", "node-ao/2.0.3");
            connection.setRequestProperty("Accept", "application/json");
            int responseCode = connection.getResponseCode();

            if (responseCode == HttpURLConnection.HTTP_OK) {
                BufferedReader in = new BufferedReader(
                    new InputStreamReader(connection.getInputStream())
                );
                StringBuilder response = new StringBuilder();
                String inputLine;

                while ((inputLine = in.readLine()) != null) {
                    response.append(inputLine);
                }
                in.close();
                return response.toString();
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
        return "";
    }

    @Override
    public String fetchPlayerStats(String playerName) throws IOException {
        String uuid = PlayerUtils.getUUIDFromPlayerName(playerName);
        if (uuid == null) {
            uuid = mojangApi.fetchUUID(playerName);
            if (uuid.equals("ERROR")) {
                return (
                    " §cCould not find " +
                    playerName +
                    " in the current lobby or on Mojang API."
                );
            }
        }
        String stjson = fetchPlayerData(uuid);
        if (stjson == null || stjson.isEmpty()) {
            return " §cFailed to get stats for " + playerName;
        }

        JsonObject rootObject = new JsonParser()
            .parse(stjson)
            .getAsJsonObject();

        if (!rootObject.get("success").getAsBoolean()) {
            return (
                " §cFailed to get stats for " + playerName + " from Abyss API."
            );
        }

        JsonObject player = rootObject.getAsJsonObject("player");
        String displayedName = player.has("displayname")
            ? "§r[" + player.get("displayname").getAsString() + "]"
            : "[]";

        JsonObject achievements = player.getAsJsonObject("achievements");
        String level = achievements.has("bedwars_level")
            ? achievements.get("bedwars_level").getAsString()
            : "0";
        level = FormattingUtils.formatStars(level);

        JsonObject bedwarsStats = player
            .getAsJsonObject("stats")
            .getAsJsonObject("Bedwars");
        int finalKills = bedwarsStats.has("final_kills_bedwars")
            ? bedwarsStats.get("final_kills_bedwars").getAsInt()
            : 0;
        int finalDeaths = bedwarsStats.has("final_deaths_bedwars")
            ? bedwarsStats.get("final_deaths_bedwars").getAsInt()
            : 0;
        double fkdr = (finalDeaths == 0)
            ? finalKills
            : (double) finalKills / finalDeaths;
        String fkdrColor = "§7";
        if (fkdr >= 1 && fkdr < 3) fkdrColor = "§f";
        if (fkdr >= 3 && fkdr < 8) fkdrColor = "§a";
        if (fkdr >= 8 && fkdr < 16) fkdrColor = "§6";
        if (fkdr >= 16 && fkdr < 25) fkdrColor = "§d";
        if (fkdr > 25) fkdrColor = "§4";
        DecimalFormat df = new DecimalFormat("#.##");
        String formattedFkdr = df.format(fkdr);
        return (
            displayedName +
            " §r" +
            level +
            " FKDR: " +
            fkdrColor +
            formattedFkdr
        );
    }
}
