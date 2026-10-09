package com.roxiun.mellow.feature.replay;

import com.roxiun.mellow.gamestate.GameSnapshot;

final class ReplayRecordingPolicy {
    private ReplayRecordingPolicy() {}
    // Keep world-start packets while location is unknown, but never buffer a known unrelated game.
    static boolean shouldBuffer(GameSnapshot snapshot, long transitionDeadline, long now) {
        if (snapshot != null && snapshot.isLobby()) return false;
        if (snapshot != null && snapshot.getGameType() != null) {
            return snapshot.getGameType() == net.hypixel.data.type.GameType.BEDWARS && !snapshot.isLobby();
        }
        return now < transitionDeadline;
    }
    static boolean isRecordableMatch(GameSnapshot s) { return s != null && s.isInBedwarsMatch(); }
    static boolean isBedwarsSession(GameSnapshot s) { return s != null && s.isInBedwarsSession(); }
}
