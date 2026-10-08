package com.roxiun.mellow.stats.murdermystery;

import com.roxiun.mellow.stats.*;
import com.roxiun.mellow.stats.StatDefinition.Style;
import com.roxiun.mellow.api.hypixel.HypixelPlayerData;
import com.roxiun.mellow.api.model.ProviderResult;
import com.roxiun.mellow.data.TabStats;
import com.roxiun.mellow.gamestate.GameSnapshot;
import net.hypixel.data.type.GameType;

public final class MurderMysteryGame extends GameDefinition<MurderMysteryPlayer> {
    public MurderMysteryGame() {
        super(StatScope.MURDER_MYSTERY, "murder_mystery", "Murder Mystery", MurderMysteryPlayer.class, new int[] {0, 2, 1, 3, 4},
            StatDefinition.team(),
            new StatDefinition("wins", "WINS", "Wins", 42, 72, Style.VALUE),
            StatDefinition.name(),
            new StatDefinition("kills", "KILLS", "Kills", 44, 72, Style.VALUE),
            new StatDefinition("games", "GAMES", "Games", 44, 72, Style.VALUE),
            StatDefinition.none(),
            StatDefinition.health(),
            StatDefinition.tags(),
            StatDefinition.ping());
    }
    @Override public GameType scoreboardType(String heading) {
        return heading.contains("murder mystery") ? GameType.MURDER_MYSTERY : null;
    }
    @Override public boolean matches(GameSnapshot snapshot) { return snapshot.getGameType() == GameType.MURDER_MYSTERY; }
    @Override public java.util.List<String> modes() {
        return java.util.Arrays.asList("overall", "classic", "double_up", "assassins", "infection");
    }
    @Override public String modeLabel(String mode) {
        if ("double_up".equals(mode)) return "Double Up";
        return Character.toUpperCase(mode.charAt(0)) + mode.substring(1);
    }
    @Override public String detectMode(GameSnapshot snapshot) {
        String mode = snapshot.getMode();
        if (mode != null) {
            mode = mode.toLowerCase(java.util.Locale.ROOT).replace("murder_", "");
            if (modes().contains(mode)) return mode;
        }
        return "overall";
    }

    @Override public ProviderResult<MurderMysteryPlayer> parse(HypixelPlayerData data, String mode) {
        return MurderMysteryParser.parse(data, mode);
    }
    @Override public TabStats tabStats(MurderMysteryPlayer player) {
        java.util.Map<String, String> values = new java.util.LinkedHashMap<>();
        values.put("wins", StatFormatting.formatTabCountForDisplay("§7" + player.getWins()));
        values.put("kills", StatFormatting.formatTabCountForDisplay("§7" + player.getKills()));
        values.put("games", StatFormatting.formatTabCountForDisplay("§7" + player.getGames()));
        return new TabStats(null, player.getFormattedName(), values);
    }
    @Override public String chatStats(MurderMysteryPlayer player) {
        if (player == null) return "";
        return player.getFormattedName() + " §r§7|§r WINS: " + player.getWins() + " §r§7|§r KILLS: " + player.getKills() + " §r§7|§r GAMES: " + player.getGames() + "§r";
    }
}
