package com.roxiun.mellow.api.util;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.roxiun.mellow.api.bedwars.BedwarsPlayer;
import com.roxiun.mellow.api.buildbattle.BuildBattlePlayer;
import com.roxiun.mellow.api.duels.DuelsMode;
import com.roxiun.mellow.api.duels.DuelsPlayer;
import com.roxiun.mellow.api.provider.model.FetchFailureReason;
import com.roxiun.mellow.api.provider.model.ProviderId;
import com.roxiun.mellow.api.provider.model.ProviderResult;
import com.roxiun.mellow.api.skywars.SkywarsPlayer;
import com.roxiun.mellow.api.tnt.TntRunPlayer;
import com.roxiun.mellow.util.formatting.FormattingUtils;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class HypixelApiUtils {

    private static final String DEFAULT_SKYWARS_EMBLEM = "✯";

    public static String fetchPlayerData(String urlString, String userAgent) {
        ProviderResult<String> result = fetchPlayerDataResult(urlString, userAgent);
        return result.isSuccess() ? result.getValue() : "";
    }

    public static String fetchPlayerData(
        String urlString,
        String userAgent,
        Map<String, String> headers
    ) {
        ProviderResult<String> result = fetchPlayerDataResult(
            urlString,
            userAgent,
            headers
        );
        return result.isSuccess() ? result.getValue() : "";
    }

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

            if (urlString.contains("nadeshiko")) {
                Pattern pattern = Pattern.compile(
                    "playerData = JSON.parse\\(decodeURIComponent\\(\\\"(.*?)\\\"\\)\\)"
                );
                Matcher matcher = pattern.matcher(response.toString());

                if (matcher.find()) {
                    String playerDataEncoded = matcher.group(1);
                    return ProviderResult.success(
                        URLDecoder.decode(playerDataEncoded, "UTF-8")
                    );
                }
            }

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

    public static BedwarsPlayer parsePlayerData(String json, String provider) {
        ProviderId providerId;
        if ("Abyss".equalsIgnoreCase(provider)) {
            providerId = ProviderId.ABYSS;
        } else if ("Nadeshiko".equalsIgnoreCase(provider)) {
            providerId = ProviderId.NADESHIKO;
        } else if ("Bordic".equalsIgnoreCase(provider)) {
            providerId = ProviderId.BORDIC;
        } else if ("Bedlify".equalsIgnoreCase(provider)) {
            providerId = ProviderId.BEDLIFY;
        } else {
            providerId = ProviderId.HYPIXEL_PUBLIC;
        }
        return parsePlayerDataResult(json, providerId).getValue();
    }

    public static BedwarsPlayer parsePlayerData(String json, ProviderId providerId) {
        return parsePlayerDataResult(json, providerId).getValue();
    }

    public static ProviderResult<BedwarsPlayer> parsePlayerDataResult(
        String json,
        ProviderId providerId
    ) {
        try {
            JsonObject rootObject = new JsonParser().parse(json).getAsJsonObject();
            JsonObject playerObject = getPlayerObject(rootObject, providerId);
            if (isMissingPlayerObject(playerObject)) {
                return ProviderResult.failure(
                    FetchFailureReason.NO_PLAYER_DATA,
                    "Provider returned no player data"
                );
            }

            String name = getString(
                playerObject,
                "displayname",
                getString(rootObject, "name", "[]")
            );
            if (providerId == ProviderId.NADESHIKO) {
                JsonObject profile = getObject(rootObject, "profile");
                name =
                    getString(
                        profile,
                        "hypixel_displayname",
                        getString(rootObject, "name", name)
                    );
            }
            String formattedName = getFormattedNameWithRank(
                rootObject,
                playerObject,
                providerId,
                name
            );

            JsonObject achievements = getObject(playerObject, "achievements");
            int stars = getInt(achievements, "bedwars_level", 0);

            JsonObject stats = getObject(playerObject, "stats");
            JsonObject bedwarsStats = getObject(stats, "Bedwars");

            int finalKills = getInt(bedwarsStats, "final_kills_bedwars", 0);
            int finalDeaths = getInt(bedwarsStats, "final_deaths_bedwars", 0);
            double fkdr = finalDeaths == 0
                ? finalKills
                : (double) finalKills / finalDeaths;

            Integer winstreakValue = getNullableInt(bedwarsStats, "winstreak");
            int winstreak = winstreakValue == null ? 0 : winstreakValue;
            boolean hasWinstreakData = winstreakValue != null;
            int wins = getInt(bedwarsStats, "wins_bedwars", 0);
            int losses = getInt(bedwarsStats, "losses_bedwars", 0);
            int bedsBroken = getInt(bedwarsStats, "beds_broken_bedwars", 0);
            int bedsLost = getInt(bedwarsStats, "beds_lost_bedwars", 0);
            int finals = finalKills;

            return ProviderResult.success(
                new BedwarsPlayer(
                    name,
                    formattedName,
                    FormattingUtils.formatStars(String.valueOf(stars)),
                    fkdr,
                    winstreak,
                    hasWinstreakData,
                    finalKills,
                    finalDeaths,
                    wins,
                    losses,
                    bedsBroken,
                    bedsLost,
                    finals
                )
            );
        } catch (Exception e) {
            return ProviderResult.failure(
                FetchFailureReason.PARSE_ERROR,
                e.getMessage()
            );
        }
    }

    public static SkywarsPlayer parseSkywarsPlayerData(
        String json,
        ProviderId providerId
    ) {
        return parseSkywarsPlayerDataResult(json, providerId).getValue();
    }

    public static ProviderResult<SkywarsPlayer> parseSkywarsPlayerDataResult(
        String json,
        ProviderId providerId
    ) {
        try {
            JsonObject rootObject = new JsonParser().parse(json).getAsJsonObject();
            JsonObject playerObject = getPlayerObject(rootObject, providerId);
            if (isMissingPlayerObject(playerObject)) {
                return ProviderResult.failure(
                    FetchFailureReason.NO_PLAYER_DATA,
                    "Provider returned no player data"
                );
            }

            String name = getString(
                playerObject,
                "displayname",
                getString(rootObject, "name", "[]")
            );
            if (providerId == ProviderId.NADESHIKO) {
                JsonObject profile = getObject(rootObject, "profile");
                name =
                    getString(
                        profile,
                        "hypixel_displayname",
                        getString(rootObject, "name", name)
                    );
            }

            String formattedName = getFormattedNameWithRank(
                rootObject,
                playerObject,
                providerId,
                name
            );

            JsonObject achievements = getObject(playerObject, "achievements");
            JsonObject stats = getObject(playerObject, "stats");
            JsonObject skywarsStats = getObject(stats, "SkyWars");

            int kills = getInt(skywarsStats, "kills", 0);
            int deaths = getInt(skywarsStats, "deaths", 0);
            int wins = getInt(skywarsStats, "wins", 0);
            int losses = getInt(skywarsStats, "losses", 0);

            double kdr = deaths == 0 ? kills : (double) kills / deaths;
            String levelFormatted = resolveSkywarsLevelFormatted(
                achievements,
                skywarsStats
            );
            String levelFormattedWithBrackets =
                resolveSkywarsLevelFormattedWithBrackets(
                    levelFormatted,
                    skywarsStats
                );

            return ProviderResult.success(
                new SkywarsPlayer(
                    name,
                    formattedName,
                    levelFormatted,
                    levelFormattedWithBrackets,
                    kdr,
                    wins,
                    losses,
                    kills,
                    deaths
                )
            );
        } catch (Exception e) {
            return ProviderResult.failure(
                FetchFailureReason.PARSE_ERROR,
                e.getMessage()
            );
        }
    }

    public static DuelsPlayer parseDuelsPlayerData(
        String json,
        ProviderId providerId,
        DuelsMode requestedMode
    ) {
        return parseDuelsPlayerDataResult(json, providerId, requestedMode).getValue();
    }

    public static ProviderResult<DuelsPlayer> parseDuelsPlayerDataResult(
        String json,
        ProviderId providerId,
        DuelsMode requestedMode
    ) {
        try {
            JsonObject rootObject = new JsonParser().parse(json).getAsJsonObject();
            JsonObject playerObject = getPlayerObject(rootObject, providerId);
            if (isMissingPlayerObject(playerObject)) {
                return ProviderResult.failure(
                    FetchFailureReason.NO_PLAYER_DATA,
                    "Provider returned no player data"
                );
            }

            String name = getString(
                playerObject,
                "displayname",
                getString(rootObject, "name", "[]")
            );
            if (providerId == ProviderId.NADESHIKO) {
                JsonObject profile = getObject(rootObject, "profile");
                name =
                    getString(
                        profile,
                        "hypixel_displayname",
                        getString(rootObject, "name", name)
                    );
            }

            String formattedName = getFormattedNameWithRank(
                rootObject,
                playerObject,
                providerId,
                name
            );

            JsonObject achievements = getObject(playerObject, "achievements");
            JsonObject stats = getObject(playerObject, "stats");
            JsonObject duelsStats = getObject(stats, "Duels");

            DuelsMode mode = requestedMode == null ? DuelsMode.OVERALL : requestedMode;
            if (
                mode != DuelsMode.OVERALL &&
                !hasModeSpecificDuelsStats(duelsStats, mode)
            ) {
                mode = DuelsMode.OVERALL;
            }

            int kills = getDuelsStat(duelsStats, mode, "kills");
            int deaths = getDuelsStat(duelsStats, mode, "deaths");
            int wins = getDuelsStat(duelsStats, mode, "wins");
            int losses = getDuelsStat(duelsStats, mode, "losses");
            int winstreak = getDuelsWinstreak(duelsStats, mode);
            String division = resolveDuelsDivision(duelsStats, achievements, mode);

            return ProviderResult.success(
                new DuelsPlayer(
                    name,
                    formattedName,
                    mode,
                    division,
                    kills,
                    deaths,
                    wins,
                    losses,
                    winstreak
                )
            );
        } catch (Exception e) {
            return ProviderResult.failure(
                FetchFailureReason.PARSE_ERROR,
                e.getMessage()
            );
        }
    }

    public static BuildBattlePlayer parseBuildBattlePlayerData(
        String json,
        ProviderId providerId
    ) {
        return parseBuildBattlePlayerDataResult(json, providerId).getValue();
    }

    public static ProviderResult<BuildBattlePlayer> parseBuildBattlePlayerDataResult(
        String json,
        ProviderId providerId
    ) {
        try {
            JsonObject rootObject = new JsonParser().parse(json).getAsJsonObject();
            JsonObject playerObject = getPlayerObject(rootObject, providerId);
            if (isMissingPlayerObject(playerObject)) {
                return ProviderResult.failure(
                    FetchFailureReason.NO_PLAYER_DATA,
                    "Provider returned no player data"
                );
            }

            String name = getString(
                playerObject,
                "displayname",
                getString(rootObject, "name", "[]")
            );
            if (providerId == ProviderId.NADESHIKO) {
                JsonObject profile = getObject(rootObject, "profile");
                name =
                    getString(
                        profile,
                        "hypixel_displayname",
                        getString(rootObject, "name", name)
                    );
            }

            String formattedName = getFormattedNameWithRank(
                rootObject,
                playerObject,
                providerId,
                name
            );

            JsonObject stats = getObject(playerObject, "stats");
            JsonObject buildBattleStats = getObject(stats, "BuildBattle");

            int score = maxExistingIntAcrossObjects(
                new JsonObject[] { buildBattleStats, stats, playerObject },
                new String[] { "score", "buildbattle_build_battle_score" }
            );
            if (score == Integer.MIN_VALUE) {
                score = 0;
            }

            JsonObject[] buildBattleObjects = new JsonObject[] {
                buildBattleStats,
                stats,
                playerObject,
            };

            int wins = maxExistingIntAcrossObjects(
                buildBattleObjects,
                new String[] {
                    "wins",
                    "buildbattle_wins",
                    "buildbattle_build_battle_wins"
                }
            );
            if (wins == Integer.MIN_VALUE) {
                wins = sumBestPerKeyAcrossObjects(
                    buildBattleObjects,
                    new String[] {
                        "wins_solo_normal",
                        "wins_teams_normal",
                        "wins_guess_the_build",
                        "wins_guess_the_build_teams",
                        "wins_solo_pro",
                        "wins_speed_builders"
                    }
                );
            }
            if (wins == Integer.MIN_VALUE) {
                wins = 0;
            }

            int gamesPlayed = maxExistingIntAcrossObjects(
                buildBattleObjects,
                new String[] { "games_played", "buildbattle_games_played" }
            );
            if (gamesPlayed == Integer.MIN_VALUE) {
                gamesPlayed = sumBestPerKeyAcrossObjects(
                    buildBattleObjects,
                    new String[] {
                        "games_played_solo_normal",
                        "games_played_teams_normal",
                        "games_played_guess_the_build",
                        "games_played_guess_the_build_teams",
                        "games_played_solo_pro",
                        "games_played_speed_builders"
                    }
                );
            }
            if (gamesPlayed == Integer.MIN_VALUE) {
                gamesPlayed = 0;
            }

            return ProviderResult.success(
                new BuildBattlePlayer(
                    name,
                    formattedName,
                    score,
                    wins,
                    gamesPlayed
                )
            );
        } catch (Exception e) {
            return ProviderResult.failure(
                FetchFailureReason.PARSE_ERROR,
                e.getMessage()
            );
        }
    }

    public static TntRunPlayer parseTntRunPlayerData(
        String json,
        ProviderId providerId
    ) {
        return parseTntRunPlayerDataResult(json, providerId).getValue();
    }

    public static ProviderResult<TntRunPlayer> parseTntRunPlayerDataResult(
        String json,
        ProviderId providerId
    ) {
        try {
            JsonObject rootObject = new JsonParser().parse(json).getAsJsonObject();
            JsonObject playerObject = getPlayerObject(rootObject, providerId);
            if (isMissingPlayerObject(playerObject)) {
                return ProviderResult.failure(
                    FetchFailureReason.NO_PLAYER_DATA,
                    "Provider returned no player data"
                );
            }

            String name = getString(
                playerObject,
                "displayname",
                getString(rootObject, "name", "[]")
            );
            if (providerId == ProviderId.NADESHIKO) {
                JsonObject profile = getObject(rootObject, "profile");
                name =
                    getString(
                        profile,
                        "hypixel_displayname",
                        getString(rootObject, "name", name)
                    );
            }

            String formattedName = getFormattedNameWithRank(
                rootObject,
                playerObject,
                providerId,
                name
            );

            JsonObject stats = getObject(playerObject, "stats");
            JsonObject tntGamesStats = getObject(stats, "TNTGames");

            int wins = maxExistingIntAcrossObjects(
                new JsonObject[] { tntGamesStats, stats, playerObject },
                new String[] { "wins_tntrun", "tntgames_tnt_run_wins" }
            );
            if (wins == Integer.MIN_VALUE) {
                wins = 0;
            }

            int deaths = maxExistingIntAcrossObjects(
                new JsonObject[] { tntGamesStats, stats, playerObject },
                new String[] {
                    "deaths_tntrun",
                    "deaths_tourney_tnt_run_0",
                    "tntgames_tnt_run_deaths"
                }
            );
            if (deaths == Integer.MIN_VALUE) {
                deaths = 0;
            }

            int bestRecord = maxExistingIntAcrossObjects(
                new JsonObject[] { tntGamesStats, stats, playerObject },
                new String[] { "record_tntrun", "tntgames_tnt_run_record" }
            );
            if (bestRecord == Integer.MIN_VALUE) {
                bestRecord = 0;
            }

            return ProviderResult.success(
                new TntRunPlayer(
                    name,
                    formattedName,
                    wins,
                    deaths,
                    bestRecord
                )
            );
        } catch (Exception e) {
            return ProviderResult.failure(
                FetchFailureReason.PARSE_ERROR,
                e.getMessage()
            );
        }
    }

    private static String getFormattedNameWithRank(
        JsonObject rootObject,
        JsonObject playerObject,
        ProviderId providerId,
        String plainName
    ) {
        if (plainName == null || plainName.isEmpty() || "[]".equals(plainName)) {
            plainName = getString(rootObject, "name", "Unknown");
        }

        if (providerId == ProviderId.NADESHIKO) {
            JsonObject profile = getObject(rootObject, "profile");
            String tagged = getString(profile, "tagged_name", "");
            if (!tagged.isEmpty()) {
                return tagged;
            }

            String tag = getString(profile, "tag", "");
            if (!tag.isEmpty()) {
                return tag + " " + plainName;
            }

            return plainName;
        }

        String prefix = normalizeFormatting(getString(playerObject, "prefix", ""));
        if (!prefix.isEmpty()) {
            return prefix + " " + plainName;
        }

        String staffRank = getString(playerObject, "rank", "");
        if (!staffRank.isEmpty() && !"NONE".equalsIgnoreCase(staffRank)) {
            return formatStaffRank(staffRank) + " " + plainName;
        }

        String newPackageRank = getString(playerObject, "newPackageRank", "");
        String packageRank = getString(playerObject, "packageRank", "");
        String monthlyRank = getString(playerObject, "monthlyPackageRank", "");

        String plusColor = colorForHypixelRank(
            getString(playerObject, "rankPlusColor", "RED")
        );
        String monthlyColor = colorForHypixelRank(
            getString(playerObject, "monthlyRankColor", "GOLD")
        );

        if ("SUPERSTAR".equalsIgnoreCase(monthlyRank)) {
            return monthlyColor + "[MVP" + plusColor + "++" + monthlyColor + "] " + plainName;
        }
        if (
            "MVP_PLUS".equalsIgnoreCase(newPackageRank) ||
            "MVP_PLUS".equalsIgnoreCase(packageRank)
        ) {
            return "§b[MVP" + plusColor + "+§b] " + plainName;
        }
        if (
            "MVP".equalsIgnoreCase(newPackageRank) ||
            "MVP".equalsIgnoreCase(packageRank)
        ) {
            return "§b[MVP] " + plainName;
        }
        if (
            "VIP_PLUS".equalsIgnoreCase(newPackageRank) ||
            "VIP_PLUS".equalsIgnoreCase(packageRank)
        ) {
            return "§a[VIP§6+§a] " + plainName;
        }
        if (
            "VIP".equalsIgnoreCase(newPackageRank) ||
            "VIP".equalsIgnoreCase(packageRank)
        ) {
            return "§a[VIP] " + plainName;
        }

        return plainName;
    }

    private static String normalizeFormatting(String value) {
        if (value == null) {
            return "";
        }
        return value.replace('&', '§').trim();
    }

    private static String formatStaffRank(String rank) {
        if (rank == null || rank.isEmpty()) {
            return "";
        }

        switch (rank.toUpperCase()) {
            case "ADMIN":
                return "§c[ADMIN]";
            case "MODERATOR":
                return "§2[MOD]";
            case "HELPER":
                return "§9[HELPER]";
            case "YOUTUBER":
                return "§c[§fYOUTUBE§c]";
            case "GAME_MASTER":
                return "§2[GM]";
            case "OWNER":
                return "§c[OWNER]";
            case "NORMAL":
            case "NONE":
                return "";
            default:
                return "§f[" + rank + "]";
        }
    }

    private static String colorForHypixelRank(String color) {
        if (color == null || color.isEmpty()) {
            return "§f";
        }

        switch (color.toUpperCase()) {
            case "BLACK":
                return "§0";
            case "DARK_BLUE":
                return "§1";
            case "DARK_GREEN":
                return "§2";
            case "DARK_AQUA":
                return "§3";
            case "DARK_RED":
                return "§4";
            case "DARK_PURPLE":
                return "§5";
            case "GOLD":
                return "§6";
            case "GRAY":
                return "§7";
            case "DARK_GRAY":
                return "§8";
            case "BLUE":
                return "§9";
            case "GREEN":
                return "§a";
            case "AQUA":
                return "§b";
            case "RED":
                return "§c";
            case "LIGHT_PURPLE":
                return "§d";
            case "YELLOW":
                return "§e";
            case "WHITE":
            default:
                return "§f";
        }
    }

    private static JsonObject getPlayerObject(JsonObject root, ProviderId providerId) {
        if (providerId == ProviderId.ABYSS) {
            if (!getBoolean(root, "success", false)) {
                return null;
            }
            return getObject(root, "player");
        }

        if (
            providerId == ProviderId.HYPIXEL_PUBLIC ||
            providerId == ProviderId.BORDIC ||
            providerId == ProviderId.BEDLIFY
        ) {
            if (!getBoolean(root, "success", false)) {
                return null;
            }
            return getObject(root, "player");
        }

        return root;
    }

    private static boolean isMissingPlayerObject(JsonObject playerObject) {
        return playerObject == null || playerObject.entrySet().isEmpty();
    }

    private static String resolveSkywarsLevelFormatted(
        JsonObject achievements,
        JsonObject skywarsStats
    ) {
        String formatted = normalizeFormatting(
            getString(skywarsStats, "levelFormatted", "")
        );
        String formattedWithBrackets = normalizeFormatting(
            getString(skywarsStats, "levelFormattedWithBrackets", "")
        );

        if (!formatted.isEmpty()) {
            if (hasSkywarsEmblem(formatted)) {
                return formatted;
            }

            String bracketedInner = stripOuterBrackets(formattedWithBrackets);
            if (!bracketedInner.isEmpty() && hasSkywarsEmblem(bracketedInner)) {
                return bracketedInner;
            }

            return appendDefaultSkywarsEmblem(formatted);
        }

        String bracketedInner = stripOuterBrackets(formattedWithBrackets);
        if (!bracketedInner.isEmpty()) {
            if (hasSkywarsEmblem(bracketedInner)) {
                return bracketedInner;
            }
            return appendDefaultSkywarsEmblem(bracketedInner);
        }

        int achievementLevel = getInt(achievements, "skywars_level", -1);
        if (achievementLevel >= 0) {
            return "§7" + achievementLevel + DEFAULT_SKYWARS_EMBLEM;
        }

        int level = getInt(skywarsStats, "level", -1);
        if (level >= 0) {
            return "§7" + level + DEFAULT_SKYWARS_EMBLEM;
        }

        int exp = getInt(skywarsStats, "skywars_experience", -1);
        if (exp >= 0) {
            return "§7" + exp + DEFAULT_SKYWARS_EMBLEM;
        }

        return "§70" + DEFAULT_SKYWARS_EMBLEM;
    }

    private static String resolveSkywarsLevelFormattedWithBrackets(
        String levelFormatted,
        JsonObject skywarsStats
    ) {
        String bracketed = normalizeFormatting(
            getString(skywarsStats, "levelFormattedWithBrackets", "")
        ).trim();
        if (!bracketed.isEmpty()) {
            return bracketed;
        }

        return wrapSkywarsLevelWithBrackets(levelFormatted);
    }

    private static String wrapSkywarsLevelWithBrackets(String levelFormatted) {
        String level = appendDefaultSkywarsEmblem(levelFormatted);
        if (level == null || level.isEmpty()) {
            level = "§70" + DEFAULT_SKYWARS_EMBLEM;
        }

        String plain = level.replaceAll("§.", "").trim();
        if (
            (plain.startsWith("[") && plain.endsWith("]")) ||
            (plain.startsWith("{") && plain.endsWith("}"))
        ) {
            return level;
        }

        String bracketColor = "§7";
        java.util.regex.Matcher matcher = java.util.regex.Pattern.compile("§.").matcher(level);
        if (matcher.find()) {
            bracketColor = matcher.group();
        }

        return bracketColor + "[" + level + bracketColor + "]";
    }

    private static boolean hasSkywarsEmblem(String formatted) {
        if (formatted == null || formatted.isEmpty()) {
            return false;
        }

        String plain = formatted.replaceAll("§.", "").trim();
        if (plain.isEmpty()) {
            return false;
        }

        for (int i = 0; i < plain.length(); i++) {
            char c = plain.charAt(i);
            if (!Character.isDigit(c) && c != '[' && c != ']') {
                return true;
            }
        }

        return false;
    }

    private static String appendDefaultSkywarsEmblem(String formatted) {
        if (formatted == null || formatted.isEmpty()) {
            return "§70" + DEFAULT_SKYWARS_EMBLEM;
        }

        if (hasSkywarsEmblem(formatted)) {
            return formatted;
        }

        return formatted + DEFAULT_SKYWARS_EMBLEM;
    }

    private static String stripOuterBrackets(String value) {
        if (value == null || value.isEmpty()) {
            return "";
        }

        String trimmed = value.trim();
        String plain = trimmed.replaceAll("§.", "");
        boolean square = plain.startsWith("[") && plain.endsWith("]");
        boolean curly = plain.startsWith("{") && plain.endsWith("}");
        if (!square && !curly) {
            return trimmed;
        }

        char openChar = square ? '[' : '{';
        char closeChar = square ? ']' : '}';
        int open = trimmed.indexOf(openChar);
        int close = trimmed.lastIndexOf(closeChar);
        if (open >= 0 && close > open) {
            return (
                trimmed.substring(0, open) +
                trimmed.substring(open + 1, close) +
                trimmed.substring(close + 1)
            );
        }

        return trimmed;
    }

    private static boolean hasModeSpecificDuelsStats(
        JsonObject duelsStats,
        DuelsMode mode
    ) {
        if (duelsStats == null || mode == null || mode.isOverall()) {
            return true;
        }

        for (String prefix : mode.getStatPrefixes()) {
            if (
                duelsStats.has(prefix + "_wins") ||
                duelsStats.has(prefix + "_losses") ||
                duelsStats.has(prefix + "_kills") ||
                duelsStats.has(prefix + "_deaths")
            ) {
                return true;
            }
        }

        return false;
    }

    private static int getDuelsStat(
        JsonObject duelsStats,
        DuelsMode mode,
        String statSuffix
    ) {
        if (duelsStats == null || statSuffix == null || statSuffix.isEmpty()) {
            return 0;
        }

        if (mode == null || mode.isOverall()) {
            return getInt(duelsStats, statSuffix, 0);
        }

        int total = 0;
        boolean found = false;
        for (String prefix : mode.getStatPrefixes()) {
            String key = prefix + "_" + statSuffix;
            if (duelsStats.has(key)) {
                total += getInt(duelsStats, key, 0);
                found = true;
            }
        }

        return found ? total : 0;
    }

    private static int getDuelsWinstreak(JsonObject duelsStats, DuelsMode mode) {
        if (duelsStats == null) {
            return 0;
        }

        if (mode == null || mode.isOverall()) {
            return getInt(duelsStats, "current_winstreak", 0);
        }

        int best = 0;
        boolean found = false;
        for (String prefix : mode.getStatPrefixes()) {
            int value = maxExistingInt(
                duelsStats,
                new String[] {
                    "current_winstreak_mode_" + prefix,
                    "current_" + prefix + "_winstreak",
                    prefix + "_winstreak"
                }
            );
            if (value != Integer.MIN_VALUE) {
                best = Math.max(best, value);
                found = true;
            }

            if (prefix.endsWith("_duel")) {
                String trimmed = prefix.substring(0, prefix.length() - "_duel".length());
                int trimmedValue = maxExistingInt(
                    duelsStats,
                    new String[] {
                        "current_" + trimmed + "_winstreak",
                        trimmed + "_winstreak"
                    }
                );
                if (trimmedValue != Integer.MIN_VALUE) {
                    best = Math.max(best, trimmedValue);
                    found = true;
                }
            }
        }

        for (String alias : mode.getDivisionPrefixes()) {
            int value = maxExistingInt(
                duelsStats,
                new String[] {
                    "current_" + alias + "_winstreak",
                    alias + "_winstreak"
                }
            );
            if (value != Integer.MIN_VALUE) {
                best = Math.max(best, value);
                found = true;
            }
        }

        if (found) {
            return best;
        }
        return getInt(duelsStats, "current_winstreak", 0);
    }

    private static String resolveDuelsDivision(
        JsonObject duelsStats,
        JsonObject achievements,
        DuelsMode mode
    ) {
        String explicit = findDuelsDivisionString(duelsStats, mode);
        if (!explicit.isEmpty()) {
            return explicit;
        }

        String tiered = resolveTieredDuelsDivision(duelsStats, mode);
        if (!tiered.isEmpty()) {
            return tiered;
        }

        Integer prestige = getDuelsPrestige(achievements, mode);
        if (prestige != null && prestige >= 0) {
            return formatDuelsDivision(prestige);
        }

        return "§7Unranked";
    }

    private static String findDuelsDivisionString(
        JsonObject duelsStats,
        DuelsMode mode
    ) {
        if (duelsStats == null) {
            return "";
        }

        String[] generalKeys = new String[] {
            "duels_division",
            "duels_title",
            "division",
            "title"
        };
        if (mode == null || mode.isOverall()) {
            return findFirstNonEmptyString(duelsStats, generalKeys);
        }

        for (String prefix : mode.getStatPrefixes()) {
            String candidate = findFirstNonEmptyString(
                duelsStats,
                new String[] {
                    prefix + "_division",
                    prefix + "_title",
                    "current_" + prefix + "_division",
                    "current_" + prefix + "_title"
                }
            );
            if (!candidate.isEmpty()) {
                return candidate;
            }
        }

        return findFirstNonEmptyString(duelsStats, generalKeys);
    }

    private static Integer getDuelsPrestige(
        JsonObject achievements,
        DuelsMode mode
    ) {
        if (achievements == null) {
            return null;
        }

        if (mode != null && !mode.isOverall()) {
            for (String key : mode.getTitlePrestigeKeys()) {
                Integer value = getNullableInt(achievements, key);
                if (value != null) {
                    return value;
                }
            }
        }

        return getNullableInt(achievements, "duels_title_prestige");
    }

    private static String resolveTieredDuelsDivision(
        JsonObject duelsStats,
        DuelsMode mode
    ) {
        if (duelsStats == null) {
            return "";
        }

        String resolved = resolveTieredDuelsDivisionForPrefixes(
            duelsStats,
            mode == null ? DuelsMode.OVERALL.getDivisionPrefixes() : mode.getDivisionPrefixes()
        );
        if (!resolved.isEmpty()) {
            return resolved;
        }

        if (mode != null && !mode.isOverall()) {
            return resolveTieredDuelsDivisionForPrefixes(
                duelsStats,
                DuelsMode.OVERALL.getDivisionPrefixes()
            );
        }

        return "";
    }

    private static String resolveTieredDuelsDivisionForPrefixes(
        JsonObject duelsStats,
        String[] prefixes
    ) {
        if (duelsStats == null || prefixes == null || prefixes.length == 0) {
            return "";
        }

        String[] rankKeys = new String[] {
            "rookie",
            "iron",
            "gold",
            "diamond",
            "master",
            "legend",
            "grandmaster",
            "godlike",
            "celestial",
            "divine",
            "ascended",
        };

        for (String prefix : prefixes) {
            if (prefix == null || prefix.isEmpty()) {
                continue;
            }

            int bestRank = -1;
            int bestProgress = -1;
            for (int i = 0; i < rankKeys.length; i++) {
                String key = prefix + "_" + rankKeys[i] + "_title_prestige";
                if (!duelsStats.has(key)) {
                    continue;
                }

                int progress = getInt(duelsStats, key, -1);
                if (progress < 0) {
                    continue;
                }

                bestRank = i;
                bestProgress = progress;
            }

            if (bestRank >= 0) {
                int tier = Math.min(Math.max(bestProgress, 0), 4) + 1;
                return formatDuelsDivisionWithTier(bestRank, tier);
            }
        }

        return "";
    }

    private static String formatDuelsDivision(int prestige) {
        if (prestige < 0) {
            return "§7Unranked";
        }

        String[] names = new String[] {
            "Rookie",
            "Iron",
            "Gold",
            "Diamond",
            "Master",
            "Legend",
            "Grandmaster",
            "Godlike",
            "Celestial",
            "Divine",
            "Ascended",
        };
        String[] colors = new String[] {
            "§7",
            "§f",
            "§6",
            "§b",
            "§2",
            "§d",
            "§4",
            "§5",
            "§3",
            "§c",
            "§e",
        };

        int index = Math.min(prestige, names.length - 1);
        return colors[index] + names[index];
    }

    private static String formatDuelsDivisionWithTier(int rankIndex, int tier) {
        String[] names = new String[] {
            "Rookie",
            "Iron",
            "Gold",
            "Diamond",
            "Master",
            "Legend",
            "Grandmaster",
            "Godlike",
            "Celestial",
            "Divine",
            "Ascended",
        };
        String[] colors = new String[] {
            "§7",
            "§f",
            "§6",
            "§b",
            "§2",
            "§d",
            "§4",
            "§5",
            "§3",
            "§c",
            "§e",
        };
        String[] roman = new String[] { "I", "II", "III", "IV", "V" };

        if (rankIndex < 0 || rankIndex >= names.length) {
            return "§7Unranked";
        }

        int tierIndex = Math.min(Math.max(tier, 1), 5) - 1;
        return colors[rankIndex] + names[rankIndex] + " " + roman[tierIndex];
    }

    private static String findFirstNonEmptyString(
        JsonObject object,
        String[] keys
    ) {
        if (object == null || keys == null || keys.length == 0) {
            return "";
        }

        for (String key : keys) {
            String value = normalizeFormatting(getString(object, key, "")).trim();
            if (!value.isEmpty()) {
                return value;
            }
        }

        return "";
    }

    private static int maxExistingInt(JsonObject object, String[] keys) {
        if (object == null || keys == null || keys.length == 0) {
            return Integer.MIN_VALUE;
        }

        int best = Integer.MIN_VALUE;
        for (String key : keys) {
            if (key == null || key.isEmpty() || !object.has(key)) {
                continue;
            }
            best = Math.max(best, getInt(object, key, 0));
        }
        return best;
    }

    private static int maxExistingIntAcrossObjects(
        JsonObject[] objects,
        String[] keys
    ) {
        if (objects == null || objects.length == 0) {
            return Integer.MIN_VALUE;
        }

        int best = Integer.MIN_VALUE;
        for (JsonObject object : objects) {
            int value = maxExistingInt(object, keys);
            if (value == Integer.MIN_VALUE) {
                continue;
            }
            best = Math.max(best, value);
        }

        return best;
    }

    private static int sumBestPerKeyAcrossObjects(
        JsonObject[] objects,
        String[] keys
    ) {
        if (
            objects == null ||
            objects.length == 0 ||
            keys == null ||
            keys.length == 0
        ) {
            return Integer.MIN_VALUE;
        }

        int sum = 0;
        boolean foundAny = false;

        for (String key : keys) {
            if (key == null || key.isEmpty()) {
                continue;
            }

            int bestForKey = Integer.MIN_VALUE;
            for (JsonObject object : objects) {
                if (object == null || !object.has(key)) {
                    continue;
                }
                bestForKey = Math.max(bestForKey, getInt(object, key, 0));
            }

            if (bestForKey != Integer.MIN_VALUE) {
                sum += bestForKey;
                foundAny = true;
            }
        }

        return foundAny ? sum : Integer.MIN_VALUE;
    }

    private static JsonObject getObject(JsonObject object, String key) {
        if (object == null || !object.has(key)) {
            return new JsonObject();
        }

        JsonElement element = object.get(key);
        if (element == null || element.isJsonNull() || !element.isJsonObject()) {
            return new JsonObject();
        }

        return element.getAsJsonObject();
    }

    private static int getInt(JsonObject object, String key, int fallback) {
        if (object == null || !object.has(key)) {
            return fallback;
        }

        JsonElement element = object.get(key);
        if (element == null || element.isJsonNull()) {
            return fallback;
        }

        try {
            if (element.getAsJsonPrimitive().isNumber()) {
                return element.getAsInt();
            }
            return (int) Double.parseDouble(element.getAsString());
        } catch (Exception e) {
            return fallback;
        }
    }

    private static Integer getNullableInt(JsonObject object, String key) {
        if (object == null || !object.has(key)) {
            return null;
        }

        JsonElement element = object.get(key);
        if (element == null || element.isJsonNull()) {
            return null;
        }

        try {
            if (element.getAsJsonPrimitive().isNumber()) {
                return element.getAsInt();
            }
            return (int) Double.parseDouble(element.getAsString());
        } catch (Exception e) {
            return null;
        }
    }

    private static String getString(JsonObject object, String key, String fallback) {
        if (object == null || !object.has(key)) {
            return fallback;
        }

        JsonElement element = object.get(key);
        if (element == null || element.isJsonNull()) {
            return fallback;
        }

        try {
            return element.getAsString();
        } catch (Exception e) {
            return fallback;
        }
    }

    private static boolean getBoolean(JsonObject object, String key, boolean fallback) {
        if (object == null || !object.has(key)) {
            return fallback;
        }

        JsonElement element = object.get(key);
        if (element == null || element.isJsonNull()) {
            return fallback;
        }

        try {
            return element.getAsBoolean();
        } catch (Exception e) {
            return fallback;
        }
    }
}
