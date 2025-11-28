package com.roxiun.mellow.util.tags;

import com.roxiun.mellow.Mellow;
import com.roxiun.mellow.api.provider.NadeshikoApi;
import com.roxiun.mellow.api.provider.StatsProvider;
import com.roxiun.mellow.util.blacklist.BlacklistManager;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Arrays;
import java.util.Base64;
import java.util.Calendar;
import java.util.Date;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.minecraft.util.EnumChatFormatting;

public class TagUtils {

    private final Mellow mellow;
    private final BlacklistManager blacklistManager;

    public TagUtils(Mellow mellow, BlacklistManager blacklistManager) {
        this.mellow = mellow;
        this.blacklistManager = blacklistManager;
    }

    public String buildTags(
        String name,
        String uuid,
        int stars,
        double fkdr,
        int ws,
        int finals,
        int fdeaths
    ) {
        String totaltags = "";

        if (uuid != null && !uuid.isEmpty()) {
            String formattedUUID = uuid;
            if (!formattedUUID.contains("-")) {
                formattedUUID = formattedUUID.replaceFirst(
                    "([0-9a-fA-F]{8})([0-9a-fA-F]{4})([0-9a-fA-F]{4})([0-9a-fA-F]{4})([0-9a-fA-F]{12})",
                    "$1-$2-$3-$4-$5"
                );
            }
            try {
                if (
                    blacklistManager.isBlacklisted(
                        UUID.fromString(formattedUUID)
                    )
                ) {
                    totaltags =
                        totaltags + EnumChatFormatting.DARK_RED + "BL §r";
                }
            } catch (IllegalArgumentException e) {
                // Invalid UUID format, ignore.
            }
        }

        String[] suswords = {
            "msmc",
            "kikin",
            "g0ld",
            "Fxrina_",
            "MAL_",
            "fer_",
            "ly_",
            "tzi_",
            "Verse_",
            "uwunova",
            "Anas_",
            "MyloAlt_",
            "rayl_",
            "mchk_",
            "HellAlts_",
            "disruptive",
            "solaralts_",
            "G0LDALTS_",
            "unwilling",
            "predicative",
        };
        boolean suswordcheck = Arrays.stream(suswords).anyMatch(keyword ->
            name.toLowerCase().contains(keyword.toLowerCase())
        );
        if (
            suswordcheck ||
            Pattern.compile("\\d.*\\d.*\\d.*\\d").matcher(name).find()
        ) totaltags = totaltags + EnumChatFormatting.YELLOW + "N §r";

        if (stars <= 6 && ws >= 1) totaltags =
            totaltags + EnumChatFormatting.GREEN + "W §r";

        if (stars <= 6 && fkdr >= 4) totaltags =
            totaltags + EnumChatFormatting.DARK_RED + "F §r";

        String[] defaultSkinIDS = {
            "a3bd16079f764cd541e072e888fe43885e711f98658323db0f9a6045da91ee7a ",
            "b66bc80f002b10371e2fa23de6f230dd5e2f3affc2e15786f65bc9be4c6eb71a",
            "e5cdc3243b2153ab28a159861be643a4fc1e3c17d291cdd3e57a7f370ad676f3",
            "f5dddb41dcafef616e959c2817808e0be741c89ffbfed39134a13e75b811863d",
            "4c05ab9e07b3505dc3ec11370c3bdce5570ad2fb2b562e9b9dd9cf271f81aa44",
            "31f477eb1a7beee631c2ca64d06f8f68fa93a3386d04452ab27f43acdf1b60cb",
            "6ac6ca262d67bcfb3dbc924ba8215a18195497c780058a5749de674217721892",
            "1abc803022d8300ab7578b189294cce39622d9a404cdc00d3feacfdf45be6981",
            "daf3d88ccb38f11f74814e92053d92f7728ddb1a7955652a60e30cb27ae6659f",
            "fece7017b1bb13926d1158864b283b8b930271f80a90482f174cca6a17e88236",
        };
        try {
            String urlString =
                "https://sessionserver.mojang.com/session/minecraft/profile/" +
                uuid;

            URL url = new URL(urlString);
            HttpURLConnection connection =
                (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");

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
                String[] parts = responseString.split("\"value\" : \"");
                String value = parts[1].split("\"")[0];
                byte[] decodedBytes = Base64.getDecoder().decode(value);
                String valueJson = new String(decodedBytes);
                boolean skincheck = Arrays.stream(defaultSkinIDS).anyMatch(id ->
                    valueJson.toLowerCase().contains(id.toLowerCase())
                );
                if (skincheck) totaltags =
                    totaltags + EnumChatFormatting.DARK_AQUA + "SK §r";
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        StatsProvider statsProvider = mellow.getStatsProvider();
        String playerData = statsProvider.fetchPlayerData(uuid);
        Pattern timestampPattern;
        if (statsProvider instanceof NadeshikoApi) {
            timestampPattern = Pattern.compile(
                "\"first_login\":(\\d+),",
                Pattern.CASE_INSENSITIVE
            );
        } else {
            timestampPattern = Pattern.compile(
                "\"firstLogin\":(\\d+),",
                Pattern.CASE_INSENSITIVE
            );
        }
        Matcher timestampMatcher = timestampPattern.matcher(playerData);
        if (timestampMatcher.find()) {
            long timestamp = Long.parseLong(timestampMatcher.group(1));
            Date loginDate = new Date(timestamp);

            Calendar currentCalendar = Calendar.getInstance();
            Calendar loginCalendar = Calendar.getInstance();

            currentCalendar.setTimeInMillis(System.currentTimeMillis());
            currentCalendar.set(Calendar.HOUR_OF_DAY, 0);
            currentCalendar.set(Calendar.MINUTE, 0);
            currentCalendar.set(Calendar.SECOND, 0);
            currentCalendar.set(Calendar.MILLISECOND, 0);

            loginCalendar.setTime(loginDate);
            loginCalendar.set(Calendar.HOUR_OF_DAY, 0);
            loginCalendar.set(Calendar.MINUTE, 0);
            loginCalendar.set(Calendar.SECOND, 0);
            loginCalendar.set(Calendar.MILLISECOND, 0);

            long diff =
                currentCalendar.getTimeInMillis() -
                loginCalendar.getTimeInMillis();
            long oneDayMillis = 24 * 60 * 60 * 1000;

            if (Math.abs(diff) <= oneDayMillis) {
                totaltags = totaltags + EnumChatFormatting.RED + "NL §r";
            }
        }

        if (finals == 0 && fdeaths == 0) totaltags =
            totaltags + EnumChatFormatting.RED + "0F §r";

        return totaltags;
    }
}
