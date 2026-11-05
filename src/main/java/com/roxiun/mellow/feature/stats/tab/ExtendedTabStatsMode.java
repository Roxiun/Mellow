package com.roxiun.mellow.feature.stats.tab;

import com.roxiun.mellow.Mellow;
import com.roxiun.mellow.api.hypixel.HypixelFeatures;
import com.roxiun.mellow.stats.StatScope;
import com.roxiun.mellow.feature.stats.StatScopeResolver;
import com.roxiun.mellow.gamestate.GameSnapshot;
import net.minecraft.client.Minecraft;

public final class ExtendedTabStatsMode {

    private ExtendedTabStatsMode() {}

    public static StatScope resolveScope() {
        GameSnapshot snapshot = HypixelFeatures.getInstance().getGameSnapshot();
        if (snapshot == null || !snapshot.isOnHypixel()) {
            return null;
        }
        if (
            snapshot.isLobby() &&
            (Mellow.config == null || !Mellow.config.extendedTabStatsInLobbies)
        ) {
            return null;
        }

        return StatScopeResolver.resolveSupportedScope(snapshot);
    }

    public static boolean isExtendedModeActiveNow() {
        StatScope scope = resolveScope();
        if (scope == null) {
            return false;
        }

        if (
            Mellow.config == null ||
            !Mellow.config.tabStats ||
            !Mellow.config.extendedTabStatsView
        ) {
            return false;
        }

        Minecraft mc = Minecraft.getMinecraft();
        return mc != null && mc.thePlayer != null && mc.theWorld != null;
    }
}
