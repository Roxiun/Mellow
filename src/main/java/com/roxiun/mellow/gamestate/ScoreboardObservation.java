package com.roxiun.mellow.gamestate;

import java.util.*;
import java.util.regex.*;
import net.hypixel.data.type.GameType;

/** Facts extracted once from a sidebar. Missing evidence never means a match started. */
public final class ScoreboardObservation {
    private static final Pattern TIME = Pattern.compile("(\\d{1,2}):(\\d{2})");
    private static final Pattern START = Pattern.compile(".*start(?:s|ing)? in (\\d{1,2})\\s*(?:s|seconds?)[.!]?.*");
    public final GameType gameType;
    public final boolean waiting;
    public final boolean live;
    public final int countdownSeconds;
    public final int stageSeconds;
    public final int stageRemaining;

    private ScoreboardObservation(GameType type, boolean waiting, boolean live, int countdown, int stage, int remaining) {
        this.gameType = type; this.waiting = waiting; this.live = live;
        this.countdownSeconds = countdown; this.stageSeconds = stage; this.stageRemaining = remaining;
    }

    public static ScoreboardObservation parse(String title, List<String> lines) {
        String heading = normalize(title);
        GameType type = com.roxiun.mellow.stats.GameRegistry.scoreboardType(heading);
        boolean waiting = false, live = false;
        int countdown = -1, stage = -1, remaining = -1;
        for (String raw : lines == null ? Collections.<String>emptyList() : lines) {
            String line = normalize(raw);
            boolean starting = line.contains("starting in") || line.contains("starts in") || line.contains("start in");
            waiting |= line.matches("players(?::|\\s|$).*") && !line.startsWith("players left")
                || starting || line.contains("waiting for players");
            Matcher time = TIME.matcher(line);
            if (time.find()) {
                int seconds = Integer.parseInt(time.group(1)) * 60 + Integer.parseInt(time.group(2));
                if (starting) countdown = seconds;
                int scheduled = stageTime(line.substring(0, time.start()));
                if (scheduled >= 0 && stage < 0) { stage = scheduled; remaining = seconds; live = true; }
            }
            Matcher start = START.matcher(line);
            if (start.matches()) countdown = Integer.parseInt(start.group(1));
        }
        return new ScoreboardObservation(type, waiting, live, countdown, stage, remaining);
    }

    public static GamePhase resolve(GamePhase previous, boolean lobby, ScoreboardObservation observation) {
        if (lobby) return GamePhase.LOBBY;
        if (observation.live) return GamePhase.LIVE;
        if (previous == GamePhase.LIVE) return previous;
        if (observation.waiting) return GamePhase.PREGAME;
        return previous == GamePhase.PREGAME ? previous : GamePhase.UNKNOWN;
    }

    private static int stageTime(String value) {
        String event = value.replaceFirst("^next event:\\s*", "").replaceFirst("\\s+in\\s*$", "").trim();
        if (event.contains("diamond")) {
            if (containsTier(event, "ii", "2")) return 360;
            if (containsTier(event, "iii", "3")) return 1080;
        }
        if (event.contains("emerald")) {
            if (containsTier(event, "ii", "2")) return 720;
            if (containsTier(event, "iii", "3")) return 1440;
        }
        if (event.contains("sudden death")) return 2400;
        if (event.contains("game end") || event.contains("end game")) return 3000;
        if (event.contains("bed gone") || event.contains("beds gone")
            || event.contains("bed destroyed") || event.contains("beds destroyed")) return 1800;
        return -1;
    }

    private static boolean containsTier(String event, String roman, String numeric) {
        return event.matches(".*\\b(?:" + roman + "|" + numeric + ")\\b.*");
    }

    private static String normalize(String value) {
        return value == null ? "" : value.replaceAll("§.", "").toLowerCase(Locale.ROOT).replaceAll("\\s+", " ").trim();
    }
}
