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
    @Test public void staleWaitingSidebarDoesNotUndoStartAndUnrelatedTimersAreNotGuessed() {
        ScoreboardObservation waiting = ScoreboardObservation.parse("BED WARS", Arrays.asList("Starting in 5s"));
        assertEquals(GamePhase.LIVE, ScoreboardObservation.resolve(GamePhase.LIVE, false, waiting));
        assertFalse(ScoreboardObservation.parse("DUELS", Arrays.asList("Time Left: 05:00")).live);
    }
    @Test public void preservesLegacyStageTimerVariants() {
        String[] lines = {"Diamond Upgrade II in 5:00", "Next Event: Emerald Upgrade 2 in 5:00",
            "Diamond III in 5:00", "Emerald tier 3 in 5:00", "Beds Gone! in 5:00",
            "Bed destroyed in 5:00", "Sudden Death! in 5:00", "Game End! in 5:00", "End Game in 5:00"};
        int[] schedules = {360, 720, 1080, 1440, 1800, 1800, 2400, 3000, 3000};
        for (int i = 0; i < lines.length; i++) {
            ScoreboardObservation observation = ScoreboardObservation.parse("BED WARS", Collections.singletonList(lines[i]));
            assertEquals(lines[i], schedules[i], observation.stageSeconds);
            assertEquals(300, observation.stageRemaining);
        }
        assertEquals(360, ScoreboardObservation.parse("BED WARS", Arrays.asList(
            "Diamond II in 5:00", "Emerald II in 11:00")).stageSeconds);
        for (String line : Arrays.asList("Diamond VIII in 5:00", "Emerald 12 in 5:00", "Unrelated event in 5:00"))
            assertEquals(-1, ScoreboardObservation.parse("BED WARS", Collections.singletonList(line)).stageSeconds);
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
