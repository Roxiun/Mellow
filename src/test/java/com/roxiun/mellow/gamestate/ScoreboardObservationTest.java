package com.roxiun.mellow.gamestate;
import java.util.*;
import org.junit.Test;
import static org.junit.Assert.*;
public class ScoreboardObservationTest {
    @Test public void missingSidebarDoesNotStartOrEndMatch() {
        ScoreboardObservation empty = ScoreboardObservation.parse("", Collections.emptyList());
        assertEquals(GamePhase.UNKNOWN, ScoreboardObservation.resolve(GamePhase.UNKNOWN, false, empty));
        assertEquals(GamePhase.PREGAME, ScoreboardObservation.resolve(GamePhase.PREGAME, false, empty));
        assertEquals(GamePhase.LIVE, ScoreboardObservation.resolve(GamePhase.LIVE, false, empty));
        assertEquals(GamePhase.LOBBY, ScoreboardObservation.resolve(GamePhase.LIVE, true, empty));
    }
    @Test public void stageTimerStartsMatchAndCountdownDoesNot() {
        ScoreboardObservation pre = ScoreboardObservation.parse("BED WARS", Arrays.asList("Players: 8/8", "Starting in 00:05"));
        assertEquals(5, pre.countdownSeconds);
        assertEquals(GamePhase.PREGAME, ScoreboardObservation.resolve(GamePhase.UNKNOWN, false, pre));
        ScoreboardObservation live = ScoreboardObservation.parse("BED WARS", Arrays.asList("Diamond II in 5:59"));
        assertEquals(360, live.stageSeconds);
        assertEquals(GamePhase.LIVE, ScoreboardObservation.resolve(GamePhase.PREGAME, false, live));
    }
    @Test public void phaseAndPartyUpdatesPreserveSessionAndDefensivelyCopyInputs() {
        java.util.Map<java.util.UUID, PartyState.PartyRole> members = new java.util.HashMap<>();
        java.util.UUID id = java.util.UUID.randomUUID();
        members.put(id, PartyState.PartyRole.MEMBER);
        PartyState party = new PartyState(true, null, members);
        GameSnapshot snapshot = new GameSnapshot(true, "mini", net.hypixel.data.type.GameType.BEDWARS,
            "", "", GamePhase.PREGAME, "BED WARS", Collections.emptyList(), party, 3, 42);
        members.clear();
        assertTrue(snapshot.getPartyState().getMembers().containsKey(id));
        GameSnapshot live = snapshot.withPhase(GamePhase.LIVE).withPartyState(PartyState.empty());
        assertEquals(42, live.getSessionId());
        assertEquals(5, live.getStateVersion());
        assertEquals(GamePhase.PREGAME, snapshot.getPhase());
        assertTrue(live.isInBedwarsMatch());
    }
}
