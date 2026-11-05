package com.roxiun.mellow.stats.skywars;

import com.roxiun.mellow.stats.*;
import com.roxiun.mellow.stats.StatDefinition.Style;
import com.roxiun.mellow.api.hypixel.HypixelPlayerData;
import com.roxiun.mellow.api.model.ProviderResult;
import com.roxiun.mellow.data.TabStats;
import com.roxiun.mellow.gamestate.GameSnapshot;
import net.hypixel.data.type.GameType;
import static com.roxiun.mellow.stats.StatFormatting.formatTabCountForDisplay;

public final class SkywarsGame extends GameDefinition<SkywarsPlayer> {
    public SkywarsGame() {
        super(StatScope.SKYWARS, "skywars", "SkyWars", SkywarsPlayer.class, new int[] {0, 1, 2, 3, 4, 8},
            StatDefinition.team(),
            new StatDefinition("level", "LEVEL", "Level", 56, 70, Style.BADGE),
            StatDefinition.name(),
            new StatDefinition("kdr", "KDR", "KDR", 40, 72, Style.VALUE),
            new StatDefinition("wlr", "WLR", "WLR", 40, 72, Style.VALUE),
            new StatDefinition("wins", "WINS", "Wins", 42, 72, Style.VALUE),
            new StatDefinition("kills", "KILLS", "Kills", 42, 72, Style.VALUE),
            StatDefinition.none(),
            StatDefinition.health(),
            StatDefinition.tags(),
            StatDefinition.ping());
    }
    @Override public GameType scoreboardType(String heading) { return heading.contains("skywars") || heading.contains("sky wars") ? GameType.SKYWARS : null; }
    @Override public boolean matches(GameSnapshot snapshot) { return snapshot.getGameType() == GameType.SKYWARS; }
    @Override public ProviderResult<SkywarsPlayer> parse(HypixelPlayerData data, String mode) {
        return SkywarsParser.parse(data);
    }
    @Override public TabStats tabStats(SkywarsPlayer player) {
        java.util.Map<String, String> values = new java.util.LinkedHashMap<>();
        values.put("stars", player.getLevelFormattedWithBrackets());
        values.put("fkdr", player.getFormattedKdrWithColor());
        values.put("wlr", player.getFormattedWlrWithColor());
        values.put("wins", formatTabCountForDisplay(player.getFormattedWinsWithColor()));
        values.put("kills", formatTabCountForDisplay(player.getFormattedKillsWithColor()));
        values.put("level", player.getLevelFormattedWithBrackets());
        values.put("kdr", player.getFormattedKdrWithColor());
        return new TabStats(null, player.getFormattedNameWithRank(), values);
    }
    @Override public String chatStats(SkywarsPlayer player) {
        if (player == null) {
            return "";
        }

        String base = String.format(
            "%s §r%s§r§7 |§r KDR: %s§r§7 |§r WLR: %s§r",
            player.getFormattedNameWithRank(),
            player.getLevelFormatted(),
            player.getFormattedKdrWithColor(),
            player.getFormattedWlrWithColor()
        );

        return base;
    }
}
