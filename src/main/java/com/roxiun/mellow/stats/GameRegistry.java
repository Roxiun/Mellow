package com.roxiun.mellow.stats;

import com.roxiun.mellow.stats.bedwars.BedwarsGame;
import com.roxiun.mellow.stats.skywars.SkywarsGame;
import com.roxiun.mellow.stats.duels.DuelsGame;
import com.roxiun.mellow.stats.buildbattle.BuildBattleGame;
import com.roxiun.mellow.stats.tntgames.tntrun.TntRunGame;
import com.roxiun.mellow.gamestate.GameSnapshot;
import java.util.*;

public final class GameRegistry {
    public static final BedwarsGame BEDWARS = new BedwarsGame();
    public static final SkywarsGame SKYWARS = new SkywarsGame();
    public static final DuelsGame DUELS = new DuelsGame();
    public static final BuildBattleGame BUILD_BATTLE = new BuildBattleGame();
    public static final TntRunGame TNT_RUN = new TntRunGame();
    private static final List<GameDefinition<?>> GAMES = Collections.unmodifiableList(
        Arrays.asList(BEDWARS, SKYWARS, DUELS, BUILD_BATTLE, TNT_RUN));
    private GameRegistry() {}
    public static List<GameDefinition<?>> all() { return GAMES; }
    public static GameDefinition<?> find(StatScope scope) {
        for (GameDefinition<?> game : GAMES) if (game.scope() == scope) return game;
        return null;
    }
    public static GameDefinition<?> find(String id) {
        for (GameDefinition<?> game : GAMES) if (game.id().equalsIgnoreCase(id)) return game;
        return null;
    }
    public static net.hypixel.data.type.GameType scoreboardType(String heading) {
        for (GameDefinition<?> game : GAMES) {
            net.hypixel.data.type.GameType type = game.scoreboardType(heading);
            if (type != null) return type;
        }
        return null;
    }
    public static StatsSelection detect(GameSnapshot snapshot) {
        if (snapshot != null && snapshot.isOnHypixel()) {
            for (GameDefinition<?> game : GAMES) if (game.matches(snapshot)) return new StatsSelection(game, game.detectMode(snapshot));
        }
        return null;
    }
}
