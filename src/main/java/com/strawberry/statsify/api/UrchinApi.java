package com.strawberry.statsify.api;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class UrchinApi {

    public String fetchUrchinTags(String playerName, String urchinKey) throws IOException {
        String tagsURL = "https://urchin.ws/player/" + playerName + "?key=" + urchinKey + "&sources=MANUAL";
        URL tagsAPIURL = new URL(tagsURL);
        HttpURLConnection statsConnection = (HttpURLConnection) tagsAPIURL.openConnection();
        statsConnection.setRequestProperty("User-Agent",
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/119.0.0.0 Safari/537.36 Edg/119.0.0.0");

        int responseCode = statsConnection.getResponseCode();
        if (responseCode != HttpURLConnection.HTTP_OK) {
            throw new IOException("URCHIN: " + responseCode);
        }

        InputStreamReader inputStream = new InputStreamReader(statsConnection.getInputStream());
        BufferedReader reader = new BufferedReader(inputStream);
        StringBuilder responseText = new StringBuilder();
        String line;

        while ((line = reader.readLine()) != null) {
            responseText.append(line);
        }
        reader.close();

        String response = responseText.toString();
        if (!response.isEmpty()) {
            try {
                String regex = "\"type\":\"(.*?)\".*?\"reason\":\"(.*?)\".*?\"added_on\":\"(.*?)\"";

                Pattern pattern = Pattern.compile(regex);
                Matcher matcher = pattern.matcher(response);

                if (matcher.find()) {
                    String type = matcher.group(1);
                    String reason = matcher.group(2);
                    return "\u00a7r" + type + ". \u00a7rReason: \u00a76" + reason;
                }
            } catch (NumberFormatException ignored) {

            }
        }
        return "";
    }
}
