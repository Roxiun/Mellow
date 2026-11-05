package com.roxiun.mellow.feature.nicks;

import com.roxiun.mellow.data.PlayerProfile;
import com.roxiun.mellow.data.TabStats;
import com.roxiun.mellow.stats.StatScope;
import org.junit.Test;
import static org.junit.Assert.*;

public class ResolvedNickProfileTest {
    @Test
    public void reusesRowsByScopeAndInvalidatesWhenProfileChanges() {
        NickUtils.ResolvedNickProfile resolved = new NickUtils.ResolvedNickProfile(
            "Player", NickUtils.ResolutionSource.SKIN);
        assertNull(resolved.getTabStats(StatScope.DUELS));
        resolved.setProfile(PlayerProfile.identity("id", "Player"));
        TabStats duels = resolved.getTabStats(StatScope.DUELS);
        assertSame(duels, resolved.getTabStats(StatScope.DUELS));
        assertNotSame(duels, resolved.getTabStats(StatScope.BEDWARS));
        assertSame(duels, resolved.getTabStats(StatScope.DUELS));
        resolved.setProfile(PlayerProfile.identity("id", "Updated"));
        assertNotSame(duels, resolved.getTabStats(StatScope.DUELS));
        assertEquals("Updated", resolved.getTabStats(StatScope.DUELS).getFormattedNameWithRank());
    }
}
