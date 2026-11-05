package com.roxiun.mellow.stats.buildbattle;

import com.roxiun.mellow.stats.*;
import com.roxiun.mellow.stats.StatDefinition.Style;
import com.roxiun.mellow.api.hypixel.HypixelPlayerData;
import com.roxiun.mellow.api.model.ProviderResult;
import com.roxiun.mellow.data.TabStats;
import com.roxiun.mellow.gamestate.GameSnapshot;
import net.hypixel.data.type.GameType;
import static com.roxiun.mellow.stats.StatFormatting.formatTabCountForDisplay;

public final class BuildBattleGame extends GameDefinition<BuildBattlePlayer> {
    public BuildBattleGame() {
        super(StatScope.BUILD_BATTLE, "build_battle", "Build Battle", BuildBattlePlayer.class, new int[] {0, 1, 2, 3},
            StatDefinition.team(),
            new StatDefinition("title", "TITLE", "Title", 86, 136, Style.TITLE),
            StatDefinition.name(),
            new StatDefinition("wins", "WINS", "Wins", 42, 72, Style.VALUE),
            StatDefinition.none(),
            StatDefinition.health(),
            StatDefinition.tags(),
            StatDefinition.ping());
    }
    @Override public GameType scoreboardType(String heading) { return heading.contains("build battle") ? GameType.BUILD_BATTLE : null; }
    @Override public boolean matches(GameSnapshot snapshot) { return snapshot.getGameType() == GameType.BUILD_BATTLE; }
    @Override public ProviderResult<BuildBattlePlayer> parse(HypixelPlayerData data, String mode) {
        return BuildBattleParser.parse(data);
    }
    @Override public TabStats tabStats(BuildBattlePlayer player) {
        java.util.Map<String, String> values = new java.util.LinkedHashMap<>();
        values.put("stars", player.getFormattedTitle());
        values.put("wins", formatTabCountForDisplay(player.getFormattedWinsWithColor()));
        values.put("title", player.getFormattedTitle());
        return new TabStats(null, player.getFormattedNameWithRank(), values);
    }
    @Override public String chatStats(BuildBattlePlayer player) {
        if (player == null) {
            return "";
        }

        return String.format(
            "%s §r%s§r§7 |§r WINS: %s§r",
            player.getFormattedNameWithRank(),
            player.getFormattedTitle(),
            player.getFormattedWinsWithColor()
        );
    }
}
