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
        new String[] { "classic_duel" },
        new String[] { "duels_classic_title_prestige" },
        new String[] { "classic" }
    ),
    UHC(
        "UHC",
        new String[] { "UHC", "UHC_DUEL", "UHC_DOUBLES" },
        new String[] { "uhc", "uhc duel" },
        new String[] { "uhc_duel", "uhc_doubles" },
        new String[] { "duels_uhc_title_prestige" },
        new String[] { "uhc" }
    ),
    OP(
        "OP",
        new String[] { "OP", "OP_DUEL", "OP_DOUBLES" },
        new String[] { "op duel", "op duels", "op doubles" },
        new String[] { "op_duel", "op_doubles" },
        new String[] { "duels_op_title_prestige" },
        new String[] { "op" }
    ),
    SKYWARS(
        "SkyWars",
        new String[] { "SW", "SW_DUEL", "SW_DOUBLES", "SKYWARS" },
        new String[] { "skywars duel", "sw duel", "sw doubles" },
        new String[] { "sw_duel", "sw_doubles" },
        new String[] { "duels_sw_title_prestige" },
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
            "BRIDGE_CVC"
        },
        new String[] { "bridge", "bridge duel" },
        new String[] {
            "bridge_duel",
            "bridge_doubles",
            "bridge_threes",
            "bridge_four",
            "bridge_2v2v2v2",
            "bridge_3v3v3v3",
            "bridge_cvc"
        },
        new String[] { "duels_bridge_title_prestige" },
        new String[] { "bridge" }
    ),
    SUMO(
        "Sumo",
        new String[] { "SUMO", "SUMO_DUEL" },
        new String[] { "sumo", "sumo duel" },
        new String[] { "sumo_duel" },
        new String[] { "duels_sumo_title_prestige" },
        new String[] { "sumo" }
    ),
    BOXING(
        "Boxing",
        new String[] { "BOXING", "BOXING_DUEL" },
        new String[] { "boxing", "boxing duel" },
        new String[] { "boxing_duel" },
        new String[] { "duels_boxing_title_prestige" },
        new String[] { "boxing" }
    ),
    COMBO(
        "Combo",
        new String[] { "COMBO", "COMBO_DUEL" },
        new String[] { "combo", "combo duel" },
        new String[] { "combo_duel" },
        new String[] { "duels_combo_title_prestige" },
        new String[] { "combo" }
    ),
    NODEBUFF(
        "NoDebuff",
        new String[] { "POTION", "POTION_DUEL", "NODEBUFF", "NO_DEBUFF" },
        new String[] { "nodebuff", "no debuff", "potion duel" },
        new String[] { "potion_duel", "no_debuff_duel" },
        new String[] { "duels_potion_title_prestige" },
        new String[] { "no_debuff", "potion" }
    ),
    BOW(
        "Bow",
        new String[] { "BOW", "BOW_DUEL", "BOWSPLEEF", "BOWSPLEEF_DUEL" },
        new String[] { "bow duel", "bowspleef", "bow spleef" },
        new String[] { "bow_duel", "bowspleef_duel" },
        new String[] { "duels_bow_title_prestige", "duels_bowspleef_title_prestige" },
        new String[] { "bow", "bowspleef" }
    ),
    BLITZ(
        "Blitz",
        new String[] { "BLITZ", "BLITZ_DUEL" },
        new String[] { "blitz", "blitz duel" },
        new String[] { "blitz_duel" },
        new String[] { "duels_blitz_title_prestige" },
        new String[] { "blitz" }
    ),
    TNT(
        "TNT",
        new String[] { "TNT", "TNT_DUEL", "TNT_GAMES_DUEL" },
        new String[] { "tnt", "tnt duel", "tnt games" },
        new String[] { "tnt_games_duel", "tnt_duel" },
        new String[] { "duels_tnt_games_title_prestige", "duels_tnt_title_prestige" },
        new String[] { "tnt_games", "tnt" }
    ),
    MEGA_WALLS(
        "MegaWalls",
        new String[] { "MW", "MW_DUEL", "MEGA_WALLS", "MEGA_WALLS_DUEL" },
        new String[] { "mega walls", "mw duel", "mega walls duel" },
        new String[] { "mw_duel", "mega_walls_duel" },
        new String[] { "duels_mega_walls_title_prestige", "duels_mw_title_prestige" },
        new String[] { "mega_walls", "mw" }
    ),
    PARKOUR(
        "Parkour",
        new String[] { "PARKOUR", "PARKOUR_DUEL", "PARKOUR_EIGHT" },
        new String[] { "parkour", "parkour duel" },
        new String[] { "parkour_duel", "parkour_eight" },
        new String[] { "duels_parkour_title_prestige" },
        new String[] { "parkour" }
    ),
    OVERALL(
        "Overall",
        new String[] {},
        new String[] {},
        new String[] {},
        new String[] { "duels_title_prestige" },
        new String[] { "all_modes" }
    );

    private final String displayName;
    private final String[] modeTokens;
    private final String[] scoreboardTokens;
    private final String[] statPrefixes;
    private final String[] titlePrestigeKeys;
    private final String[] divisionPrefixes;

    DuelsMode(
        String displayName,
        String[] modeTokens,
        String[] scoreboardTokens,
        String[] statPrefixes,
        String[] titlePrestigeKeys,
        String[] divisionPrefixes
    ) {
        this.displayName = displayName;
        this.modeTokens = modeTokens;
        this.scoreboardTokens = scoreboardTokens;
        this.statPrefixes = statPrefixes;
        this.titlePrestigeKeys = titlePrestigeKeys;
        this.divisionPrefixes = divisionPrefixes;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String[] getStatPrefixes() {
        return statPrefixes;
    }

    public String[] getTitlePrestigeKeys() {
        return titlePrestigeKeys;
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

        for (DuelsMode value : values()) {
            if (value == OVERALL) {
                continue;
            }

            for (String token : value.modeTokens) {
                String normalizedToken = normalize(token);
                if (
                    !normalizedToken.isEmpty() &&
                    (modeToken.equals(normalizedToken) || modeToken.contains(normalizedToken))
                ) {
                    return value;
                }
            }
        }

        return OVERALL;
    }

    private static DuelsMode fromScoreboardText(String text) {
        if (text.isEmpty()) {
            return OVERALL;
        }

        for (DuelsMode value : values()) {
            if (value == OVERALL) {
                continue;
            }

            for (String token : value.scoreboardTokens) {
                String normalizedToken = normalize(token);
                if (!normalizedToken.isEmpty() && text.contains(normalizedToken)) {
                    return value;
                }
            }
        }

        return OVERALL;
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
