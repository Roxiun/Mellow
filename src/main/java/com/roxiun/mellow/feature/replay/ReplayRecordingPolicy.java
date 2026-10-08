package com.roxiun.mellow.feature.replay;

import com.roxiun.mellow.gamestate.GameSnapshot;

final class ReplayRecordingPolicy {
    private ReplayRecordingPolicy() {}
    static boolean isRecordableMatch(GameSnapshot s) { return s != null && s.isInBedwarsMatch(); }
    static boolean isBedwarsSession(GameSnapshot s) { return s != null && s.isInBedwarsSession(); }
}
