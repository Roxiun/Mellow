package com.strawberry.statsify.api;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.strawberry.statsify.util.Utils;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLDecoder;
import java.text.DecimalFormat;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class NadeshikoApi {

    public String nadeshikoAPI(String uuid) {
        try {
            String urlString =
                "https://nadeshiko.io/player/" + uuid + "/network";

            URL url = new URL(urlString);
            HttpURLConnection connection =
                (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");

            connection.setRequestProperty(
                "User-Agent",
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36"
            );
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
                String responseString = response.toString();
                Pattern pattern = Pattern.compile(
                    "playerData = JSON.parse\\(decodeURIComponent\\(\"(.*?)\"\\)\\)"
                );
                Matcher matcher = pattern.matcher(responseString.toString());

                if (matcher.find()) {
                    String playerDataEncoded = matcher.group(1);
                    return URLDecoder.decode(playerDataEncoded, "UTF-8");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "";
    }

    public String fetchPlayerStats(String playerName) throws IOException {
        String uuid = Utils.getUUIDFromPlayerName(playerName);
        if (uuid == null) {
            return (
                " \u00a7cCould not find " +
                playerName +
                " in the current lobby."
            );
        }
        String stjson = nadeshikoAPI(uuid);
        if (stjson == null || stjson.isEmpty()) {
            return " \u00a7cFailed to get stats for " + playerName;
        }

        JsonObject rootObject = new JsonParser()
            .parse(stjson)
            .getAsJsonObject();

        JsonObject profile = rootObject.getAsJsonObject("profile");
        String displayedName = profile.has("tagged_name")
            ? profile.get("tagged_name").getAsString()
            : "[]";
        JsonObject ach = rootObject.getAsJsonObject("achievements");
        String level = ach.has("bedwars_level")
            ? ach.get("bedwars_level").getAsString()
            : "0";
        level = Utils.formatStars(level);

        JsonObject bedwarsStats = rootObject
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
        String fkdrColor = "\u00a77";
        if (fkdr >= 1 && fkdr < 3) fkdrColor = "\u00a7f";
        if (fkdr >= 3 && fkdr < 8) fkdrColor = "\u00a7a";
        if (fkdr >= 8 && fkdr < 16) fkdrColor = "\u00a76";
        if (fkdr >= 16 && fkdr < 25) fkdrColor = "\u00a7d";
        if (fkdr > 25) fkdrColor = "\u00a74";
        DecimalFormat df = new DecimalFormat("#.##");
        String formattedFkdr = df.format(fkdr);
        return (
            displayedName +
            " \u00a7r" +
            level +
            " FKDR: " +
            fkdrColor +
            formattedFkdr
        );
    }
}
