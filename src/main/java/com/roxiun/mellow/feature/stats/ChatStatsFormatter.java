package com.roxiun.mellow.feature.stats;

import com.roxiun.mellow.stats.StatScope;
import com.roxiun.mellow.data.PlayerProfile;

public final class ChatStatsFormatter {
    private ChatStatsFormatter() {}
    public static String format(PlayerProfile profile, StatScope scope) {
        return profile == null ? "" : profile.chatStats(scope);
    }
}
