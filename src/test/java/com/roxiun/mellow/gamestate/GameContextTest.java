package com.roxiun.mellow.gamestate;

import com.roxiun.mellow.feature.stats.StatScopeResolver;
import com.roxiun.mellow.stats.*;
import net.hypixel.data.type.GameType;
import net.hypixel.modapi.packet.impl.clientbound.event.ClientboundLocationPacket;
import org.junit.Test;
import java.util.*;
import static org.junit.Assert.*;

public class GameContextTest {
    private GameSnapshot context(GameType type, String mode, GamePhase phase, String... lines) {
        return new GameSnapshot(true, "mini", type, mode, "", phase, "", Arrays.asList(lines), null, 1, 1);
    }

    private ClientboundLocationPacket location(String server, GameType type, String lobby, String mode) {
        return new ClientboundLocationPacket(server, type, lobby, mode, null);
    }

    @Test public void renderingReadsResolvedContextWithoutRevisitingDetectionInputs() {
        GameSnapshot snapshot = new GameSnapshot(true, "mini", GameType.DUELS, "BRIDGE_DUEL", "",
            GamePhase.UNKNOWN, "", Collections.emptyList(), null, 1, 1) {
            private boolean constructed = true;
            @Override public String getMode() {
                if (constructed) throw new AssertionError("Render-time mode detection");
                return super.getMode();
            }
            @Override public String getScoreboardTitle() {
                if (constructed) throw new AssertionError("Render-time scoreboard detection");
                return super.getScoreboardTitle();
            }
            @Override public List<String> getScoreboardLines() {
                if (constructed) throw new AssertionError("Render-time scoreboard detection");
                return super.getScoreboardLines();
            }
        };
        for (int i = 0; i < 1000; i++) {
            assertEquals(StatScope.DUELS, StatScopeResolver.resolveSupportedScope(snapshot));
            assertTrue(StatScopeResolver.isSupportedStatsSession(snapshot));
        }
        assertEquals("bridge", GameRegistry.detect(snapshot).mode());
    }

    @Test public void lobbyAndUnknownApiModesDoNotGuessFromLeaderboardText() {
        GameSnapshot lobby = context(GameType.DUELS, "", GamePhase.LOBBY, "Bridge Wins: 100");
        assertEquals("overall", GameRegistry.detect(lobby).mode());
        assertFalse(GameStateManager.needsScoreboard(lobby));
        assertFalse(StatScopeResolver.isSupportedStatsSession(lobby));
        GameSnapshot unknown = context(GameType.DUELS, "FUTURE_DUEL", GamePhase.UNKNOWN, "Bridge Wins: 100");
        assertEquals("overall", GameRegistry.detect(unknown).mode());
        assertFalse(GameStateManager.needsScoreboard(unknown));
    }

    @Test public void tntApiModeWinsOverConflictingSidebarAndMap() {
        GameSnapshot tag = new GameSnapshot(true, "mini", GameType.TNTGAMES, "TNTAG", "TNT Run",
            GamePhase.UNKNOWN, "TNT RUN", Collections.emptyList(), null, 1, 1);
        assertSame(GameRegistry.TNT_TAG, tag.getStatsGame());
        assertFalse(GameStateManager.needsScoreboard(tag));
        assertNull(context(GameType.TNTGAMES, "PVPRUN", GamePhase.UNKNOWN, "TNT RUN").getStatsGame());
        assertNull(context(GameType.TNTGAMES, "", GamePhase.LOBBY, "TNT RUN").getStatsGame());
        assertSame(GameRegistry.BOW_SPLEEF,
            context(GameType.TNTGAMES, "", GamePhase.UNKNOWN, "BOW SPLEEF").getStatsGame());
    }

    @Test public void unresolvedModeUsesSlowFallbackThenLateApiTakesPrecedence() {
        GameStateManager manager = new GameStateManager();
        manager.acceptLocation(location("mini", GameType.DUELS, null, null));
        assertTrue(GameStateManager.needsScoreboard(manager.getSnapshot()));
        manager.updateFromScoreboard("DUELS", Arrays.asList("Bridge Duel"));
        assertEquals("bridge", manager.getSnapshot().getStatsMode());
        assertFalse(GameStateManager.needsScoreboard(manager.getSnapshot()));
        manager.acceptLocation(location("mini", GameType.DUELS, null, "CLASSIC_DUEL"));
        assertEquals("classic", manager.getSnapshot().getStatsMode());
        assertTrue(manager.getSnapshot().getScoreboardLines().isEmpty());
    }

    @Test public void worldAndLocationOrderingNeverCarriesOldSidebarForward() {
        GameStateManager manager = new GameStateManager();
        manager.acceptLocation(location("first", GameType.DUELS, null, "BRIDGE_DUEL"));
        manager.onWorldChange(); // Location before world load.
        assertEquals("bridge", manager.getSnapshot().getStatsMode());
        manager.onWorldChange(); // Next world before its location.
        assertNull(manager.getSnapshot().getStatsGame());
        assertTrue(manager.getSnapshot().getScoreboardLines().isEmpty());
        manager.acceptLocation(location("second", GameType.DUELS, "duels-lobby", null));
        assertEquals("overall", manager.getSnapshot().getStatsMode());
        assertFalse(GameStateManager.needsScoreboard(manager.getSnapshot()));
        manager.onDisconnect();
        assertNull(manager.getSnapshot().getStatsGame());
    }

    @Test public void bedwarsStillRequiresMatchEvidenceAndUpdatesTimersAfterStart() {
        GameStateManager manager = new GameStateManager();
        manager.acceptLocation(location("mini", GameType.BEDWARS, null, "BEDWARS_EIGHT_ONE"));
        assertTrue(GameStateManager.needsScoreboard(manager.getSnapshot()));
        assertFalse(StatScopeResolver.isSupportedStatsSession(manager.getSnapshot()));
        manager.updateFromScoreboard("BED WARS", Arrays.asList("Starting in 10s"));
        assertTrue(manager.getSnapshot().isPregame());
        manager.onChat("Protect your bed and destroy the enemy beds.");
        assertTrue(manager.getSnapshot().isInBedwarsMatch());
        assertTrue(StatScopeResolver.isSupportedStatsSession(manager.getSnapshot()));
        manager.updateFromScoreboard("BED WARS", Arrays.asList("Diamond II in 5:30"));
        assertEquals(330, manager.getSnapshot().getObservation().stageRemaining);
        long session = manager.getSnapshot().getSessionId();
        manager.onChat("The game starts in 10 seconds!");
        assertTrue(manager.getSnapshot().isPregame());
        assertTrue(manager.getSnapshot().getSessionId() > session);
    }

    @Test public void partyAndPhaseChangesReuseObservationsWithoutRefreshingCountdownAge() {
        ScoreboardObservation observation = ScoreboardObservation.parse("BED WARS", Arrays.asList("Starting in 5s"));
        GameSnapshot original = new GameSnapshot(true, "mini", GameType.BEDWARS, "", "",
            GamePhase.PREGAME, "BED WARS", Arrays.asList("Starting in 5s"), null, 1, 1, observation, 1000);
        GameSnapshot copy = original.withPartyState(PartyState.empty()).withPhase(GamePhase.PREGAME);
        assertSame(observation, copy.getObservation());
        assertEquals(5, copy.getCountdownSeconds(1000));
        assertEquals(3, copy.getCountdownSeconds(2001));
        assertEquals(-1, copy.getCountdownSeconds(3001));
    }
}
