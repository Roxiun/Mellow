package com.roxiun.mellow.stats.bedwars;

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

public final class BedwarsGame extends GameDefinition<BedwarsPlayer> {
    public BedwarsGame() {
        super(StatScope.BEDWARS, "bedwars", "Bed Wars", BedwarsPlayer.class, new int[] {0, 1, 2, 3, 4, 11},
            StatDefinition.team(),
            new StatDefinition("stars", "STARS", "Stars", 56, 70, Style.BEDWARS_STARS),
            StatDefinition.name(),
            new StatDefinition("fkdr", "FKDR", "FKDR", 40, 72, Style.VALUE),
            new StatDefinition("winstreak", "WS", "Winstreak", 42, 72, Style.BEDWARS_WINSTREAK),
            new StatDefinition("wlr", "WLR", "WLR", 40, 72, Style.VALUE),
            new StatDefinition("bblr", "BBLR", "BBLR", 44, 72, Style.VALUE),
            new StatDefinition("wins", "WINS", "Wins", 42, 72, Style.VALUE),
            new StatDefinition("beds", "BEDS", "Beds", 42, 72, Style.VALUE),
            new StatDefinition("finals", "FINALS", "Finals", 46, 72, Style.VALUE),
            StatDefinition.none(),
            StatDefinition.health(),
            StatDefinition.tags(),
            StatDefinition.ping());
    }
    @Override public GameType scoreboardType(String heading) { return heading.contains("bed wars") ? GameType.BEDWARS : null; }
    @Override public boolean matches(GameSnapshot snapshot) { return snapshot.getGameType() == GameType.BEDWARS; }
    @Override public ProviderResult<BedwarsPlayer> parse(HypixelPlayerData data, String mode) {
        return BedwarsParser.parse(data, BedwarsMode.valueOf(mode.toUpperCase(Locale.ROOT)));
    }
    @Override public String modeLabel(String mode) { return BedwarsMode.valueOf(mode.toUpperCase(Locale.ROOT)).getFullName(); }
    @Override public java.util.List<String> modes() {
        return java.util.Arrays.stream(BedwarsMode.values()).map(m -> m.name().toLowerCase(Locale.ROOT)).collect(java.util.stream.Collectors.toList());
    }
    @Override public TabStats tabStats(BedwarsPlayer player) {
        // Format numbers with appropriate formatting including colors
        String formattedWins = formatTabCountForDisplay(
            player.getFormattedWinsWithColor()
        );
        String formattedBeds = formatTabCountForDisplay(
            player.getFormattedBedsWithColor()
        );
        String formattedFinals = formatTabCountForDisplay(
            player.getFormattedFinalsWithColor()
        );
        String formattedFkdr =
            player.getFkdrColor() + player.getFormattedFkdr();
        String formattedWinstreak = formatTabCountForDisplay(
            player.getFormattedWinstreakWithColor()
        );
        String formattedWLR = player.getFormattedWLRWithColor();
        String formattedBBLR = player.getFormattedBBLRWithColor();

        java.util.Map<String, String> values = new java.util.LinkedHashMap<>();
        values.put("stars", player.getStars());
        values.put("fkdr", formattedFkdr);
        values.put("winstreak", formattedWinstreak);
        values.put("wlr", formattedWLR);
        values.put("bblr", formattedBBLR);
        values.put("wins", formattedWins);
        values.put("beds", formattedBeds);
        values.put("finals", formattedFinals);
        return new TabStats(null, player.getFormattedNameWithRank(), values);
    }
    @Override public String chatStats(BedwarsPlayer player) {
        return player == null ? "" : player.getStars() + " §r" + player.getFormattedNameWithRank()
            + " §7|§r FKDR: " + player.getFkdrColor() + player.getFormattedFkdr();
    }
}
