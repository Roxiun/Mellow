package com.roxiun.mellow.stats.tntgames.bowspleef;

import com.roxiun.mellow.stats.*;
import com.roxiun.mellow.stats.StatDefinition.Style;
import com.roxiun.mellow.api.hypixel.HypixelPlayerData;
import com.roxiun.mellow.api.model.ProviderResult;
import com.roxiun.mellow.data.TabStats;
import com.roxiun.mellow.gamestate.GameSnapshot;
import net.hypixel.data.type.GameType;

public final class BowSpleefGame extends GameDefinition<BowSpleefPlayer> {
    public BowSpleefGame() {
        super(StatScope.BOW_SPLEEF, "bow_spleef", "Bow Spleef", BowSpleefPlayer.class, new int[] {0, 2, 1, 3, 4},
            StatDefinition.team(),
            new StatDefinition("wins", "WINS", "Wins", 42, 72, Style.VALUE),
            StatDefinition.name(),
            new StatDefinition("deaths", "DEATHS", "Deaths", 44, 72, Style.VALUE),
            new StatDefinition("ratio", "RATIO", "Ratio", 44, 72, Style.VALUE),
            StatDefinition.none(),
            StatDefinition.health(),
            StatDefinition.tags(),
            StatDefinition.ping());
    }
    @Override public boolean usesRankNames(GameSnapshot snapshot) { return true; }
    @Override public GameType scoreboardType(String heading) {
        return heading.contains("bow spleef") ? GameType.TNTGAMES : null;
    }
    @Override public boolean matches(GameSnapshot snapshot) {
        if (snapshot.getGameType() != GameType.TNTGAMES) return false;
        if (snapshot.getMode() != null && !snapshot.getMode().isEmpty()) {
            return "BOWSPLEEF".equalsIgnoreCase(snapshot.getMode());
        }
        if (containsTitle(snapshot.getScoreboardTitle())) return true;
        for (String line : snapshot.getScoreboardLines()) if (containsTitle(line)) return true;
        return false;
    }
    private boolean containsTitle(String text) {
        return text != null && text.replaceAll("§.", "").toLowerCase(java.util.Locale.ROOT).contains("bow spleef");
    }

    @Override public ProviderResult<BowSpleefPlayer> parse(HypixelPlayerData data, String mode) {
        return BowSpleefParser.parse(data, mode);
    }
    @Override public TabStats tabStats(BowSpleefPlayer player) {
        java.util.Map<String, String> values = new java.util.LinkedHashMap<>();
        values.put("wins", StatFormatting.formatTabCountForDisplay("§7" + player.getWins()));
        values.put("deaths", StatFormatting.formatTabCountForDisplay("§7" + player.getDeaths()));
        values.put("ratio", "§7" + new java.text.DecimalFormat("#.##").format(player.getRatio()));
        return new TabStats(null, player.getFormattedName(), values);
    }
    @Override public String chatStats(BowSpleefPlayer player) {
        if (player == null) return "";
        return player.getFormattedName() + " §r§7|§r WINS: " + player.getWins() + " §r§7|§r DEATHS: " + player.getDeaths() + " §r§7|§r RATIO: " + new java.text.DecimalFormat("#.##").format(player.getRatio()) + "§r";
    }
}
