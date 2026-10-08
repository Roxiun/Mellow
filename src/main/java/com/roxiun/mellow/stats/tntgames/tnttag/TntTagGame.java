package com.roxiun.mellow.stats.tntgames.tnttag;

import com.roxiun.mellow.stats.*;
import com.roxiun.mellow.stats.StatDefinition.Style;
import com.roxiun.mellow.api.hypixel.HypixelPlayerData;
import com.roxiun.mellow.api.model.ProviderResult;
import com.roxiun.mellow.data.TabStats;
import com.roxiun.mellow.gamestate.GameSnapshot;
import net.hypixel.data.type.GameType;

public final class TntTagGame extends GameDefinition<TntTagPlayer> {
    public TntTagGame() {
        super(StatScope.TNT_TAG, "tnt_tag", "TNT Tag", TntTagPlayer.class, new int[] {0, 2, 1},
            StatDefinition.team(),
            new StatDefinition("wins", "WINS", "Wins", 42, 72, Style.VALUE),
            StatDefinition.name(),
            StatDefinition.none(),
            StatDefinition.health(),
            StatDefinition.tags(),
            StatDefinition.ping());
    }
    @Override public boolean usesRankNames(GameSnapshot snapshot) { return true; }
    @Override public GameType scoreboardType(String heading) {
        return heading.contains("tnt tag") ? GameType.TNTGAMES : null;
    }
    @Override public boolean matches(GameSnapshot snapshot) {
        if (snapshot.getGameType() != GameType.TNTGAMES) return false;
        if (snapshot.getMode() != null && !snapshot.getMode().isEmpty()) {
            return "TNTAG".equalsIgnoreCase(snapshot.getMode());
        }
        if (containsTitle(snapshot.getScoreboardTitle())) return true;
        for (String line : snapshot.getScoreboardLines()) if (containsTitle(line)) return true;
        return false;
    }
    private boolean containsTitle(String text) {
        return text != null && text.replaceAll("§.", "").toLowerCase(java.util.Locale.ROOT).contains("tnt tag");
    }

    @Override public ProviderResult<TntTagPlayer> parse(HypixelPlayerData data, String mode) {
        return TntTagParser.parse(data, mode);
    }
    @Override public TabStats tabStats(TntTagPlayer player) {
        java.util.Map<String, String> values = new java.util.LinkedHashMap<>();
        values.put("wins", StatFormatting.formatTabCountForDisplay("§7" + player.getWins()));
        return new TabStats(null, player.getFormattedName(), values);
    }
    @Override public String chatStats(TntTagPlayer player) {
        if (player == null) return "";
        return player.getFormattedName() + " §r§7|§r WINS: " + player.getWins() + "§r";
    }
}
