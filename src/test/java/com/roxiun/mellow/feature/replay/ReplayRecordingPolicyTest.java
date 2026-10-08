package com.roxiun.mellow.feature.replay;

import com.roxiun.mellow.gamestate.GameSnapshot;
import com.roxiun.mellow.gamestate.PartyState;
import com.roxiun.mellow.gamestate.GamePhase;
import java.util.Collections;
import net.hypixel.data.type.GameType;
import org.junit.Assert;
import org.junit.Test;

public class ReplayRecordingPolicyTest {

    @Test
    public void doesNotTreatBedwarsPregameAsRecordableMatch() {
        Assert.assertFalse(ReplayRecordingPolicy.isRecordableMatch(bedwarsSnapshot(false, true)));
    }

    @Test
    public void treatsStartedBedwarsGameAsRecordableMatch() {
        Assert.assertTrue(ReplayRecordingPolicy.isRecordableMatch(bedwarsSnapshot(false, false)));
    }

    @Test
    public void rejectsLobbyAndNonBedwarsSnapshots() {
        Assert.assertFalse(ReplayRecordingPolicy.isRecordableMatch(bedwarsSnapshot(true, false)));
        Assert.assertFalse(ReplayRecordingPolicy.isRecordableMatch(otherGameSnapshot()));
        Assert.assertFalse(ReplayRecordingPolicy.isRecordableMatch(null));
    }

    private static GameSnapshot bedwarsSnapshot(boolean lobby, boolean pregame) {
        return new GameSnapshot(
            true,
            "mini123A",
            GameType.BEDWARS,
            "BEDWARS_TWO_FOUR",
            "Picnic",
            lobby ? GamePhase.LOBBY : pregame ? GamePhase.PREGAME : GamePhase.LIVE,
            "Bed Wars",
            Collections.singletonList(pregame ? "Players: 8/8" : "Diamond II in 5:00"),
            PartyState.empty(),
            1L,
            1L
        );
    }

    private static GameSnapshot otherGameSnapshot() {
        return new GameSnapshot(
            true,
            "sw123A",
            GameType.SKYWARS,
            "Solo",
            "Shire",
            GamePhase.LIVE,
            "SkyWars",
            Collections.singletonList("Players left: 12"),
            PartyState.empty(),
            1L,
            1L
        );
    }

    private static GameSnapshot snapshotWithLines(
        GameType gameType,
        boolean lobby,
        java.util.List<String> lines
    ) {
        return new GameSnapshot(
            true,
            "mini123A",
            gameType,
            "mode",
            "map",
            lobby ? GamePhase.LOBBY : GamePhase.LIVE,
            "Title",
            lines,
            PartyState.empty(),
            1L,
            1L
        );
    }
}
