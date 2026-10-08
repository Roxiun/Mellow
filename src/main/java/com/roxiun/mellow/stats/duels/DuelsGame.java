package com.roxiun.mellow.stats.duels;

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

public final class DuelsGame extends GameDefinition<DuelsPlayer> {
    public DuelsGame() {
        super(StatScope.DUELS, "duels", "Duels", DuelsPlayer.class, new int[] {0, 1, 2, 3, 4, 5, 6, 7, 8, 11},
            StatDefinition.team(),
            new StatDefinition("division", "DIV", "Division", 56, 92, Style.BADGE),
            StatDefinition.name(),
            new StatDefinition("kdr", "KDR", "KDR", 40, 72, Style.VALUE),
            new StatDefinition("wlr", "WLR", "WLR", 40, 72, Style.VALUE),
            new StatDefinition("wins", "WINS", "Wins", 42, 72, Style.VALUE),
            new StatDefinition("losses", "LOSSES", "Losses", 48, 72, Style.VALUE),
            new StatDefinition("kills", "KILLS", "Kills", 42, 72, Style.VALUE),
            new StatDefinition("deaths", "DEATHS", "Deaths", 50, 72, Style.VALUE),
            new StatDefinition("winstreak", "WS", "Winstreak", 36, 72, Style.VALUE),
            StatDefinition.none(),
            StatDefinition.health(),
            StatDefinition.tags(),
            StatDefinition.ping());
    }
    @Override public GameType scoreboardType(String heading) { return heading.contains("duel") ? GameType.DUELS : null; }
    @Override public boolean matches(GameSnapshot snapshot) { return snapshot.getGameType() == GameType.DUELS; }
    @Override public ProviderResult<DuelsPlayer> parse(HypixelPlayerData data, String mode) {
        return DuelsParser.parse(data, DuelsMode.valueOf(mode.toUpperCase(Locale.ROOT)));
    }
    @Override public TabStats tabStats(DuelsPlayer player) {
        java.util.Map<String, String> values = new java.util.LinkedHashMap<>();
        values.put("stars", player.getDivision());
        values.put("fkdr", player.getFormattedKdrWithColor());
        values.put("winstreak", formatTabCountForDisplay(player.getFormattedWinstreakWithColor()));
        values.put("wlr", player.getFormattedWlrWithColor());
        values.put("wins", formatTabCountForDisplay(player.getFormattedWinsWithColor()));
        values.put("losses", formatTabCountForDisplay(player.getFormattedLossesWithColor()));
        values.put("kills", formatTabCountForDisplay(player.getFormattedKillsWithColor()));
        values.put("deaths", formatTabCountForDisplay(player.getFormattedDeathsWithColor()));
        values.put("division", player.getDivision());
        values.put("kdr", player.getFormattedKdrWithColor());
        return new TabStats(null, player.getFormattedNameWithRank(), values);
    }
    @Override public String chatStats(DuelsPlayer player) {
        if (player == null) {
            return "";
        }

        String modeSuffix = player.getMode() == null || player.getMode().isOverall()
            ? " §7(Overall)"
            : " §7(" + player.getMode().getDisplayName() + ")";

        return String.format(
            "%s §r%s§r%s§7 |§r KDR: %s§r§7 |§r WLR: %s§r§7 |§r WS: %s§r",
            player.getFormattedNameWithRank(),
            player.getDivision(),
            modeSuffix,
            player.getFormattedKdrWithColor(),
            player.getFormattedWlrWithColor(),
            player.getFormattedWinstreakWithColor()
        );
    }
    @Override public String modeLabel(String mode) { return DuelsMode.valueOf(mode.toUpperCase(Locale.ROOT)).getDisplayName(); }
    @Override public java.util.List<String> modes() {
        return java.util.Arrays.stream(DuelsMode.values()).map(m -> m.name().toLowerCase(Locale.ROOT)).collect(java.util.stream.Collectors.toList());
    }
    @Override public String detectMode(GameSnapshot snapshot) { return DuelsMode.fromSnapshot(snapshot).name().toLowerCase(Locale.ROOT); }
}
