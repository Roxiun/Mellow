package com.roxiun.mellow.stats.duels;

import com.roxiun.mellow.gamestate.GameSnapshot;
import java.util.List;
import java.util.Locale;
import net.hypixel.data.type.GameType;

public enum DuelsMode {
    CLASSIC(
        "Classic",
        new String[] { "CLASSIC", "CLASSIC_DUEL" },
        new String[] { "classic", "classic duel" },
        new String[] { "classic_duel", "classic_doubles" },
        new String[] { "classic" }
    ),
    UHC(
        "UHC",
        new String[] { "UHC", "UHC_DUEL", "UHC_DOUBLES" },
        new String[] { "uhc", "uhc duel" },
        new String[] { "uhc_duel", "uhc_doubles", "uhc_four", "uhc_meetup" },
        new String[] { "uhc" }
    ),
    OP(
        "OP",
        new String[] { "OP", "OP_DUEL", "OP_DOUBLES" },
        new String[] { "op duel", "op duels", "op doubles" },
        new String[] { "op_duel", "op_doubles" },
        new String[] { "op" }
    ),
    SKYWARS(
        "SkyWars",
        new String[] { "SW", "SW_DUEL", "SW_DOUBLES", "SKYWARS" },
        new String[] { "skywars duel", "sw duel", "sw doubles" },
        new String[] { "sw_duel", "sw_doubles" },
        new String[] { "skywars" }
    ),
    BRIDGE(
        "Bridge",
        new String[] {
            "BRIDGE",
            "BRIDGE_DUEL",
            "BRIDGE_2V2",
            "BRIDGE_3V3V3V3",
            "BRIDGE_2V2V2V2",
            "BRIDGE_FOUR",
            "BRIDGE_THREES",
            "BRIDGE_CVC", "CAPTURE_THREES"
        },
        new String[] { "bridge", "bridge duel" },
        new String[] {
            "bridge_duel",
            "bridge_doubles",
            "bridge_threes",
            "bridge_four",
            "bridge_2v2v2v2",
            "bridge_3v3v3v3",
            "capture_threes"
        },
        new String[] { "bridge" }
    ),
    SUMO(
        "Sumo",
        new String[] { "SUMO", "SUMO_DUEL" },
        new String[] { "sumo", "sumo duel" },
        new String[] { "sumo_duel" },
        new String[] { "sumo" }
    ),
    BOXING(
        "Boxing",
        new String[] { "BOXING", "BOXING_DUEL" },
        new String[] { "boxing", "boxing duel" },
        new String[] { "boxing_duel" },
        new String[] { "boxing" }
    ),
    COMBO(
        "Combo",
        new String[] { "COMBO", "COMBO_DUEL" },
        new String[] { "combo", "combo duel" },
        new String[] { "combo_duel" },
        new String[] { "combo" }
    ),
    NODEBUFF(
        "NoDebuff",
        new String[] { "POTION", "POTION_DUEL", "NODEBUFF", "NO_DEBUFF" },
        new String[] { "nodebuff", "no debuff", "potion duel" },
        new String[] { "potion_duel" },
        new String[] { "no_debuff", "potion" }
    ),
    BOW(
        "Bow",
        new String[] { "BOW", "BOW_DUEL" },
        new String[] { "bow duel" },
        new String[] { "bow_duel" },
        new String[] { "bow" }
    ),
    BLITZ(
        "Blitz",
        new String[] { "BLITZ", "BLITZ_DUEL" },
        new String[] { "blitz", "blitz duel" },
        new String[] { "blitz_duel" },
        new String[] { "blitz" }
    ),
    TNT(
        "TNT",
        new String[] { "TNT", "TNT_DUEL", "TNT_GAMES_DUEL" },
        new String[] { "tnt", "tnt duel", "tnt games" },
        new String[] { "tnt_games_duel", "tnt_duel" },
        new String[] { "tnt_games", "tnt" }
    ),
    MEGA_WALLS(
        "MegaWalls",
        new String[] { "MW", "MW_DUEL", "MEGA_WALLS", "MEGA_WALLS_DUEL" },
        new String[] { "mega walls", "mw duel", "mega walls duel" },
        new String[] { "mw_duel", "mw_doubles" },
        new String[] { "mega_walls", "mw" }
    ),
    PARKOUR(
        "Parkour",
        new String[] { "PARKOUR", "PARKOUR_DUEL", "PARKOUR_EIGHT" },
        new String[] { "parkour", "parkour duel" },
        new String[] { "parkour_eight" },
        new String[] { "parkour" }
    ),
    SPLEEF("Spleef", new String[] { "SPLEEF", "SPLEEF_DUEL", "BOWSPLEEF", "BOWSPLEEF_DUEL" },
        new String[] { "spleef", "bow spleef" }, new String[] { "spleef_duel", "bowspleef_duel" },
        new String[] { "spleef" }),
    QUAKE("Quakecraft", new String[] { "QUAKE", "QUAKE_DUEL", "QUAKECRAFT" },
        new String[] { "quake" }, new String[] { "quake_duel" },
        new String[] { "quakecraft" }),
    BEDWARS("Bed Wars", new String[] { "BEDWARS_TWO_ONE_DUELS", "BEDWARS_TWO_ONE_DUELS_RUSH" },
        new String[] { "bed wars", "bedwars", "bed rush" }, new String[] { "bedwars_two_one_duels", "bedwars_two_one_duels_rush" },
        new String[] { "bedwars" }),
    ARENA("Duel Arena", new String[] { "DUEL_ARENA" },
        new String[] { "duel arena", "duels arena" }, new String[] { "duel_arena" },
        new String[] { "" }),
    OVERALL(
        "Overall",
        new String[] {},
        new String[] {},
        new String[] {},
        new String[] { "all_modes" }
    );

    private final String displayName;
    private final String[] modeTokens;
    private final String[] scoreboardTokens;
    private final String[] statPrefixes;
    private final String[] divisionPrefixes;

    DuelsMode(
        String displayName,
        String[] modeTokens,
        String[] scoreboardTokens,
        String[] statPrefixes,
        String[] divisionPrefixes
    ) {
        this.displayName = displayName;
        this.modeTokens = modeTokens;
        this.scoreboardTokens = scoreboardTokens;
        this.statPrefixes = statPrefixes;
        this.divisionPrefixes = divisionPrefixes;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String[] getStatPrefixes() {
        return statPrefixes;
    }

    public String[] getDivisionPrefixes() {
        return divisionPrefixes;
    }

    public boolean isOverall() {
        return this == OVERALL;
    }

    public static DuelsMode fromSnapshot(GameSnapshot snapshot) {
        if (snapshot == null || snapshot.getGameType() != GameType.DUELS) {
            return OVERALL;
        }

        String normalizedMode = normalize(snapshot.getMode());
        DuelsMode modeMatch = fromModeToken(normalizedMode);
        if (modeMatch != OVERALL) {
            return modeMatch;
        }

        String normalizedTitle = normalize(snapshot.getScoreboardTitle());
        DuelsMode titleMatch = fromScoreboardText(normalizedTitle);
        if (titleMatch != OVERALL) {
            return titleMatch;
        }

        List<String> lines = snapshot.getScoreboardLines();
        if (lines != null) {
            for (String line : lines) {
                DuelsMode lineMatch = fromScoreboardText(normalize(line));
                if (lineMatch != OVERALL) {
                    return lineMatch;
                }
            }
        }

        return OVERALL;
    }

    private static DuelsMode fromModeToken(String modeToken) {
        if (modeToken.isEmpty()) {
            return OVERALL;
        }

        return longestMatch(modeToken, false);
    }

    private static DuelsMode fromScoreboardText(String text) {
        return longestMatch(text, true);
    }

    private static DuelsMode longestMatch(String text, boolean scoreboard) {
        DuelsMode best = OVERALL;
        int length = 0;
        for (DuelsMode mode : values()) {
            for (String token : scoreboard ? mode.scoreboardTokens : mode.modeTokens) {
                String normalized = normalize(token);
                boolean matches = scoreboard ? text.contains(normalized)
                    : text.equals(normalized) || text.startsWith(normalized + "_");
                if (matches && normalized.length() > length) {
                    best = mode;
                    length = normalized.length();
                }
            }
        }
        return best;
    }

    public boolean hasHalfTitleRequirements() {
        return this == MEGA_WALLS || this == PARKOUR || this == BOXING
            || this == NODEBUFF || this == BRIDGE;
    }

    private static String normalize(String value) {
        if (value == null) {
            return "";
        }

        return value
            .toLowerCase(Locale.ROOT)
            .replace('-', '_')
            .replaceAll("[^a-z0-9_ ]", " ")
            .replaceAll("\\s+", " ")
            .trim();
    }
}
