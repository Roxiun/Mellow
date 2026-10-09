package com.roxiun.mellow.stats;

import com.roxiun.mellow.stats.bedwars.BedwarsGame;
import com.roxiun.mellow.stats.skywars.SkywarsGame;
import com.roxiun.mellow.stats.duels.DuelsGame;
import com.roxiun.mellow.stats.buildbattle.BuildBattleGame;
import com.roxiun.mellow.stats.tntgames.tntrun.TntRunGame;
import com.roxiun.mellow.gamestate.GameSnapshot;
import java.util.*;
import com.roxiun.mellow.stats.tntgames.tnttag.TntTagGame;
import com.roxiun.mellow.stats.tntgames.bowspleef.BowSpleefGame;
import com.roxiun.mellow.stats.murdermystery.MurderMysteryGame;

public final class GameRegistry {
    public static final BedwarsGame BEDWARS = new BedwarsGame();
    public static final SkywarsGame SKYWARS = new SkywarsGame();
    public static final DuelsGame DUELS = new DuelsGame();
    public static final BuildBattleGame BUILD_BATTLE = new BuildBattleGame();
    public static final TntRunGame TNT_RUN = new TntRunGame();
    public static final TntTagGame TNT_TAG = new TntTagGame();
    public static final BowSpleefGame BOW_SPLEEF = new BowSpleefGame();
    public static final MurderMysteryGame MURDER_MYSTERY = new MurderMysteryGame();
    private static final List<GameDefinition<?>> GAMES = Collections.unmodifiableList(
        Arrays.asList(BEDWARS, SKYWARS, DUELS, BUILD_BATTLE, TNT_RUN, TNT_TAG, BOW_SPLEEF, MURDER_MYSTERY));
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
    /** Called when constructing context, never from a render hook. */
    public static GameDefinition<?> identify(GameSnapshot snapshot) {
        if (snapshot != null && snapshot.isOnHypixel()) {
            // A TNT lobby has no selected game. Its leaderboard is not location evidence.
            if (snapshot.isLobby() && snapshot.getGameType() == net.hypixel.data.type.GameType.TNTGAMES) return null;
            for (GameDefinition<?> game : GAMES) if (game.matches(snapshot)) return game;
        }
        return null;
    }
    public static StatsSelection detect(GameSnapshot snapshot) {
        return snapshot == null || snapshot.getStatsGame() == null ? null
            : new StatsSelection(snapshot.getStatsGame(), snapshot.getStatsMode());
    }
}
