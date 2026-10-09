package com.roxiun.mellow.stats.tntgames.tntrun;

import com.roxiun.mellow.stats.*;
import com.roxiun.mellow.stats.StatDefinition.Style;
import com.roxiun.mellow.api.hypixel.HypixelPlayerData;
import com.roxiun.mellow.api.model.ProviderResult;
import com.roxiun.mellow.data.TabStats;
import com.roxiun.mellow.gamestate.GameSnapshot;
import net.hypixel.data.type.GameType;
import java.util.List;
import java.util.Locale;
import static com.roxiun.mellow.stats.StatFormatting.formatTabCountForDisplay;

public final class TntRunGame extends GameDefinition<TntRunPlayer> {
    public TntRunGame() {
        super(StatScope.TNT_RUN, "tnt_run", "TNT Run", TntRunPlayer.class, new int[] {0, 2, 1, 3},
            StatDefinition.team(),
            new StatDefinition("wins", "WINS", "Wins", 42, 72, Style.VALUE),
            StatDefinition.name(),
            new StatDefinition("ratio", "RATIO", "Ratio", 44, 72, Style.VALUE),
            StatDefinition.none(),
            StatDefinition.health(),
            StatDefinition.tags(),
            StatDefinition.ping());
    }
    @Override public boolean usesRankNames(GameSnapshot snapshot) { return true; }
    @Override public GameType scoreboardType(String heading) { return heading.contains("tnt games") || heading.contains("tnt run") ? GameType.TNTGAMES : null; }
    @Override public boolean matches(GameSnapshot snapshot) { return snapshot.getGameType() == GameType.TNTGAMES && matchesTntRun(snapshot); }
    @Override public ProviderResult<TntRunPlayer> parse(HypixelPlayerData data, String mode) {
        return TntRunParser.parse(data);
    }
    @Override public TabStats tabStats(TntRunPlayer player) {
        java.util.Map<String, String> values = new java.util.LinkedHashMap<>();
        values.put("wlr", player.getFormattedRatioWithColor());
        values.put("wins", formatTabCountForDisplay(player.getFormattedWinsWithColor()));
        values.put("losses", formatTabCountForDisplay(player.getFormattedDeathsWithColor()));
        values.put("ratio", player.getFormattedRatioWithColor());
        return new TabStats(null, player.getFormattedNameWithRank(), values);
    }
    @Override public String chatStats(TntRunPlayer player) {
        if (player == null) {
            return "";
        }

        return String.format(
            "%s §r§7|§r WINS: %s§r§7 |§r RATIO: %s§r",
            player.getFormattedNameWithRank(),
            player.getFormattedWinsWithColor(),
            player.getFormattedRatioWithColor()
        );
    }
    public static boolean matchesTntRun(GameSnapshot snapshot) {
        if (snapshot == null) {
            return false;
        }

        if (snapshot.getMode() != null && !snapshot.getMode().isEmpty()) {
            return "TNTRUN".equalsIgnoreCase(snapshot.getMode());
        }
        if (containsTntRunToken(snapshot.getMap())) {
            return true;
        }
        if (containsTntRunToken(snapshot.getScoreboardTitle())) {
            return true;
        }

        List<String> lines = snapshot.getScoreboardLines();
        if (lines == null || lines.isEmpty()) {
            return false;
        }

        for (String line : lines) {
            if (containsTntRunToken(line)) {
                return true;
            }
        }

        return false;
    }

    private static boolean containsTntRunToken(String value) {
        String normalized = normalize(value);
        return normalized.contains("tntrun") || normalized.contains("tnt run");
    }

    private static String normalize(String value) {
        if (value == null || value.isEmpty()) {
            return "";
        }

        return value
            .replaceAll("§.", "")
            .toLowerCase(Locale.ROOT)
            .replace('_', ' ')
            .replace('-', ' ')
            .replaceAll("\\s+", " ")
            .trim();
    }
}
