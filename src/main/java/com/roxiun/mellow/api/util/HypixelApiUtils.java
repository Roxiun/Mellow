package com.roxiun.mellow.api.util;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.roxiun.mellow.api.bedwars.BedwarsPlayer;
import com.roxiun.mellow.util.formatting.FormattingUtils;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLDecoder;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class HypixelApiUtils {

    public static String fetchPlayerData(String urlString, String userAgent) {
        HttpURLConnection connection = null;
        try {
            URL url = new URL(urlString);
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            if (userAgent != null) {
                connection.setRequestProperty("User-Agent", userAgent);
            }
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

                if (urlString.contains("nadeshiko")) {
                    Pattern pattern = Pattern.compile(
                        "playerData = JSON.parse\\(decodeURIComponent\\(\"(.*?)\"\\)\\)"
                    );
                    Matcher matcher = pattern.matcher(response.toString());

                    if (matcher.find()) {
                        String playerDataEncoded = matcher.group(1);
                        return URLDecoder.decode(playerDataEncoded, "UTF-8");
                    }
                }
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

    public static BedwarsPlayer parsePlayerData(String json, String provider) {
        JsonObject rootObject = new JsonParser().parse(json).getAsJsonObject();

        if (provider.equals("Abyss")) {
            if (!rootObject.get("success").getAsBoolean()) {
                return null;
            }
            rootObject = rootObject.getAsJsonObject("player");
        }

        String name = rootObject.has("displayname")
            ? rootObject.get("displayname").getAsString()
            : "[]";
        if (provider.equals("Nadeshiko")) {
            name = rootObject
                .getAsJsonObject("profile")
                .get("tagged_name")
                .getAsString();
        }

        JsonObject achievements = rootObject.getAsJsonObject("achievements");
        String stars = achievements.has("bedwars_level")
            ? achievements.get("bedwars_level").getAsString()
            : "0";

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
        int winstreak = bedwarsStats.has("winstreak")
            ? bedwarsStats.get("winstreak").getAsInt()
            : 0;

        return new BedwarsPlayer(
            name,
            FormattingUtils.formatStars(stars),
            fkdr,
            winstreak,
            finalKills,
            finalDeaths
        );
    }
}
