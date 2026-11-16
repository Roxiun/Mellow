package com.roxiun.mellow.feature.nicks;

import com.roxiun.mellow.api.aurora.AuroraApi;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** A manual search for one counter, or players appearing in both counter searches. */
public final class DenickQuery {
    public static final String USAGE = "/denick <finals | beds> <number> [<finals | beds> <number>]";
    private final Integer finals;
    private final Integer beds;

    private DenickQuery(Integer finals, Integer beds) {
        this.finals = finals;
        this.beds = beds;
    }

    public static DenickQuery parse(String[] args) {
        if (args.length != 2 && args.length != 4) {
            throw new IllegalArgumentException("Invalid usage. Use: " + USAGE);
        }
        Integer finals = null;
        Integer beds = null;
        for (int i = 0; i < args.length; i += 2) {
            boolean isFinals = args[i].equalsIgnoreCase("finals");
            if (!isFinals && !args[i].equalsIgnoreCase("beds")) {
                throw new IllegalArgumentException("Invalid type. Use 'finals' or 'beds'.");
            }
            if (isFinals ? finals != null : beds != null) {
                throw new IllegalArgumentException("Specify " + (isFinals ? "finals" : "beds") + " only once.");
            }
            int value;
            try {
                value = Integer.parseInt(args[i + 1].replace(",", ""));
                if (value < 0) throw new NumberFormatException();
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Invalid number: " + args[i + 1] + ". Use a non-negative whole number.");
            }
            if (isFinals) finals = value;
            else beds = value;
        }
        return new DenickQuery(finals, beds);
    }

    public boolean isCombined() {
        return finals != null && beds != null;
    }

    public List<Match> search(AuroraApi api, int finalsRange, int bedsRange, int max, String key) throws IOException {
        List<AuroraApi.PlayerMatch> finalMatches = finals == null ? null : lookup(api, "finals", finals, finalsRange, max, key);
        List<AuroraApi.PlayerMatch> bedMatches = beds == null ? null : lookup(api, "beds", beds, bedsRange, max, key);
        return match(finalMatches, bedMatches);
    }

    private static List<AuroraApi.PlayerMatch> lookup(AuroraApi api, String type, int value, int range, int max, String key) throws IOException {
        AuroraApi.AuroraResponse response = api.queryStats(type, Integer.toString(value), range, max, key);
        if (response == null || !response.success || response.data == null) {
            throw new IOException("Aurora " + type + " lookup failed");
        }
        List<AuroraApi.PlayerMatch> matches = new ArrayList<>();
        for (AuroraApi.PlayerMatch player : response.data) {
            if (player.distance <= range) matches.add(player);
        }
        return matches;
    }

    static List<Match> match(List<AuroraApi.PlayerMatch> finals, List<AuroraApi.PlayerMatch> beds) {
        List<Match> matches = new ArrayList<>();
        List<AuroraApi.PlayerMatch> primary = finals != null ? finals : beds;
        for (AuroraApi.PlayerMatch player : primary) {
            if (matches.stream().anyMatch(match -> match.name.equalsIgnoreCase(player.name))) continue;
            Integer finalDistance = finals == null ? null : player.distance;
            Integer bedDistance = finals == null ? player.distance : null;
            if (finals != null && beds != null) {
                AuroraApi.PlayerMatch bed = beds.stream()
                    .filter(candidate -> candidate.name.equalsIgnoreCase(player.name))
                    .findFirst().orElse(null);
                if (bed == null) continue;
                bedDistance = bed.distance;
            }
            matches.add(new Match(player.name, finalDistance, bedDistance));
        }
        matches.sort(Comparator.comparingLong(Match::totalDistance)
            .thenComparing(match -> match.name, String.CASE_INSENSITIVE_ORDER));
        return matches;
    }

    public static final class Match {
        public final String name;
        public final Integer finalsDistance;
        public final Integer bedsDistance;

        private Match(String name, Integer finalsDistance, Integer bedsDistance) {
            this.name = name;
            this.finalsDistance = finalsDistance;
            this.bedsDistance = bedsDistance;
        }

        private long totalDistance() {
            return (finalsDistance == null ? 0L : finalsDistance.longValue())
                + (bedsDistance == null ? 0L : bedsDistance.longValue());
        }
    }
}
