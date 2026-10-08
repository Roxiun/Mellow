package com.roxiun.mellow.stats.skywars;

import com.google.gson.*;
import com.roxiun.mellow.api.model.*;
import com.roxiun.mellow.api.hypixel.HypixelPlayerData;
import static com.roxiun.mellow.stats.JsonStats.*;

public final class SkywarsParser {
    private SkywarsParser() {}
    private static final String DEFAULT_SKYWARS_EMBLEM = "✯";
    public static ProviderResult<SkywarsPlayer> parse(HypixelPlayerData data) {
        try {
            JsonObject playerObject = data.player();
            String name = data.name();
            String formattedName = data.formattedName();

            JsonObject achievements = getObject(playerObject, "achievements");
            JsonObject stats = getObject(playerObject, "stats");
            if (!(hasObject(stats, "SkyWars"))) {
                return ProviderResult.failure(FetchFailureReason.NO_PLAYER_DATA, "No SkyWars stats supplied");
            }
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
}
