package com.strawberry.statsify.api.duels;

import com.strawberry.statsify.util.formatting.FormattingUtils;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PlanckeApi {

    public String checkDuels(String playerName) throws IOException {
        String url = "https://plancke.io/hypixel/player/stats/" + playerName;

        URL urlObject = new URL(url);
        HttpURLConnection connection =
            (HttpURLConnection) urlObject.openConnection();
        connection.setRequestProperty("User-Agent", "Mozilla/5.0");

        InputStream inputStream = connection.getInputStream();
        BufferedReader reader = new BufferedReader(
            new InputStreamReader(inputStream)
        );
        StringBuilder responseText = new StringBuilder();
        String line;

        while ((line = reader.readLine()) != null) {
            responseText.append(line);
        }
        reader.close();

        String response = responseText.toString();
        int responseCode = connection.getResponseCode();
        if (responseCode == HttpURLConnection.HTTP_NOT_FOUND) {
            return playerName + " is §cnicked§r";
        }

        Pattern namePattern = Pattern.compile(
            "(?<=content=\"Plancke\" /><meta property=\"og:locale\" content=\"en_US\" /><meta property=\"og:description\" content=\").+?(?=\")"
        );
        Matcher nameMatcher = namePattern.matcher(response);
        String displayedName = nameMatcher.find()
            ? nameMatcher.group()
            : "Unknown";

        String playerrank = ""; // empty if no rank
        String trimmedName = displayedName.trim();

        String[] parts = trimmedName.split("\\s+", 2);
        if (
            parts.length > 0 &&
            parts[0].startsWith("[") &&
            parts[0].endsWith("]")
        ) {
            String unformattedRank = parts[0];
            playerrank = FormattingUtils.formatRank(unformattedRank) + " ";
        }
        // Insane Regex Wow
        String regex =
            "<tr><td>Classic 1v1</td><td>([\\d,]+)</td><td>([\\d,]+)</td><td>([\\d.,]+)</td><td>([\\d,]+)</td><td>([\\d,]+)</td><td>([\\d.,]+)</td><td>([\\d.,]+)</td><td>([\\d.,]+)</td></tr>";
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(response);
        if (matcher.find()) {
            String ClassicStats =
                "\n§aKills:§r " +
                matcher.group(1) +
                " §cDeaths:§r " +
                matcher.group(2) +
                " (§d" +
                matcher.group(3) +
                "§r) " +
                "\n" +
                "§bW:§r " +
                matcher.group(4) +
                " §cL: §r" +
                matcher.group(5) +
                " (§d" +
                matcher.group(6) +
                "§r)";
            return playerrank + playerName + "§r (Classic 1v1)" + ClassicStats;
        } else {
            return playerrank + playerName + " §chas no Classic Duels stats.§r";
        }
    }
}
