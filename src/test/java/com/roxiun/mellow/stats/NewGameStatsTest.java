package com.roxiun.mellow.stats;

import com.google.gson.JsonParser;
import com.roxiun.mellow.api.hypixel.HypixelPlayerData;
import com.roxiun.mellow.api.hypixel.provider.model.ProviderId;
import com.roxiun.mellow.api.model.FetchFailureReason;
import com.roxiun.mellow.gamestate.*;
import net.hypixel.data.type.GameType;
import org.junit.Test;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import static org.junit.Assert.*;

public class NewGameStatsTest {
    private HypixelPlayerData fixture(String name) throws Exception {
        try (InputStreamReader reader = new InputStreamReader(
            getClass().getResourceAsStream("/stats/" + name + ".json"), StandardCharsets.UTF_8)) {
            return HypixelPlayerData.decode(new JsonParser().parse(reader).getAsJsonObject(), ProviderId.ABYSS).getValue();
        }
    }

    @Test public void liveTntCountersKeepGamesSeparateAndAllowAbsentZeroWins() throws Exception {
        HypixelPlayerData data = fixture("new-games");
        assertEquals(1, GameRegistry.TNT_TAG.parse(data, "overall").getValue().getWins());
        assertEquals(0, GameRegistry.BOW_SPLEEF.parse(data, "overall").getValue().getWins());
        assertEquals(15, GameRegistry.BOW_SPLEEF.parse(data, "overall").getValue().getDeaths());
        com.roxiun.mellow.stats.tntgames.bowspleef.BowSpleefPlayer bow =
            GameRegistry.BOW_SPLEEF.parse(fixture("bow-spleef"), "overall").getValue();
        assertEquals(7, bow.getWins());
        assertEquals(110, bow.getDeaths());
        assertEquals(7.0 / 110, bow.getRatio(), 0.000001);
    }

    @Test public void murderModesUseTheirOwnCountersAndInfectionCombinesRoles() throws Exception {
        HypixelPlayerData data = fixture("new-games");
        assertEquals(113, GameRegistry.MURDER_MYSTERY.parse(data, "overall").getValue().getWins());
        assertEquals(78, GameRegistry.MURDER_MYSTERY.parse(data, "classic").getValue().getWins());
        assertEquals(29, GameRegistry.MURDER_MYSTERY.parse(data, "double_up").getValue().getWins());
        assertEquals(17, GameRegistry.MURDER_MYSTERY.parse(data, "infection").getValue().getKills());
        assertEquals(14, GameRegistry.MURDER_MYSTERY.parse(data, "infection").getValue().getGames());
        assertEquals(4, GameRegistry.MURDER_MYSTERY.parse(data, "assassins").getValue().getKills());
    }

    @Test public void omittedGameAndUnplayedModeDoNotBecomeOverallOrZeroStats() {
        HypixelPlayerData data = HypixelPlayerData.decode("{\"success\":true,\"player\":{\"stats\":{\"TNTGames\":{\"wins_tntrun\":90},\"MurderMystery\":{\"wins\":8}}}}", ProviderId.ABYSS).getValue();
        assertEquals(FetchFailureReason.NO_PLAYER_DATA, GameRegistry.TNT_TAG.parse(data, "overall").getFailureReason());
        assertEquals(FetchFailureReason.NO_PLAYER_DATA, GameRegistry.BOW_SPLEEF.parse(data, "overall").getFailureReason());
        assertEquals(FetchFailureReason.NO_PLAYER_DATA, GameRegistry.MURDER_MYSTERY.parse(data, "classic").getFailureReason());
    }

    @Test public void liveModeIdsSelectCorrectGameAndMurderSubmode() {
        assertSame(GameRegistry.TNT_TAG, GameRegistry.detect(snapshot(GameType.TNTGAMES, "TNTAG", "TNT GAMES")).game());
        assertSame(GameRegistry.BOW_SPLEEF, GameRegistry.detect(snapshot(GameType.TNTGAMES, "BOWSPLEEF", "TNT GAMES")).game());
        assertNull(GameRegistry.detect(snapshot(GameType.TNTGAMES, "PVPRUN", "TNT GAMES")));
        assertSame(GameRegistry.BOW_SPLEEF, GameRegistry.detect(snapshot(GameType.TNTGAMES, "", "§aBOW SPLEEF")).game());
        StatsSelection selection = GameRegistry.detect(snapshot(GameType.MURDER_MYSTERY, "MURDER_DOUBLE_UP", "MURDER MYSTERY"));
        assertSame(GameRegistry.MURDER_MYSTERY, selection.game());
        assertEquals("double_up", selection.mode());
    }

    @Test public void zeroDeathsProducesFiniteBowRatio() {
        assertEquals(5, new com.roxiun.mellow.stats.tntgames.bowspleef.BowSpleefPlayer("Player", 5, 0).getRatio(), 0);
    }

    private GameSnapshot snapshot(GameType type, String mode, String title) {
        return new GameSnapshot(true, "mini", type, mode, "", GamePhase.PREGAME,
            title, Collections.emptyList(), PartyState.empty(), 1, 1);
    }
}
