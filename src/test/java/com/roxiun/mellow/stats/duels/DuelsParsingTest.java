package com.roxiun.mellow.stats.duels;

import com.roxiun.mellow.api.hypixel.HypixelPlayerData;
import com.roxiun.mellow.api.hypixel.provider.model.ProviderId;
import org.junit.Test;
import static org.junit.Assert.*;

public class DuelsParsingTest {
    private DuelsPlayer parse(DuelsMode mode, String stats) {
        HypixelPlayerData data = HypixelPlayerData.decode(
            "{\"success\":true,\"player\":{\"displayname\":\"Test\",\"stats\":{\"Duels\":{" + stats + "}}}}",
            ProviderId.HYPIXEL_PUBLIC).getValue();
        assertNotNull("Fixture must decode", data);
        com.roxiun.mellow.api.model.ProviderResult<DuelsPlayer> result = DuelsParser.parse(data, mode);
        assertTrue("Duels stats must parse", result.isSuccess());
        return result.getValue();
    }

    @Test public void titlesUseWinsEvenWithMissingOrConflictingPrestigeFields() {
        assertEquals("§2Master", parse(DuelsMode.CLASSIC,
            "\"classic_duel_wins\":900,\"classic_doubles_wins\":100,\"title\":\"Wrong\",\"classic_rookie_title_prestige\":4").getDivision());
        assertEquals("§7-", parse(DuelsMode.CLASSIC,
            "\"classic_duel_wins\":0,\"wins\":100000,\"all_modes_godlike_title_prestige\":4").getDivision());
    }

    @Test public void titleBoundariesAndRequirementMultipliers() {
        assertEquals("§7-", DuelsDivision.format(49, DuelsMode.CLASSIC));
        assertEquals("§7Rookie", DuelsDivision.format(50, DuelsMode.CLASSIC));
        assertEquals("§7Rookie V", DuelsDivision.format(99, DuelsMode.CLASSIC));
        assertEquals("§fIron", DuelsDivision.format(100, DuelsMode.CLASSIC));
        assertEquals("§7-", DuelsDivision.format(99, DuelsMode.OVERALL));
        assertEquals("§7Rookie", DuelsDivision.format(100, DuelsMode.OVERALL));
        assertEquals("§7-", DuelsDivision.format(24, DuelsMode.BOXING));
        assertEquals("§7Rookie", DuelsDivision.format(25, DuelsMode.BOXING));
        assertEquals("§3Diamond", DuelsDivision.format(500, DuelsMode.CLASSIC));
        assertEquals("§4§lLegend", DuelsDivision.format(2000, DuelsMode.CLASSIC));
        assertEquals("§c§lASCENDED VI", DuelsDivision.format(150000, DuelsMode.CLASSIC));
        assertEquals("§c§lASCENDED XL", DuelsDivision.format(490000, DuelsMode.CLASSIC));
        assertEquals("§c§lASCENDED L", DuelsDivision.format(Integer.MAX_VALUE, DuelsMode.CLASSIC));
    }

    @Test public void bridgeIncludesSpecialCountersAndLegacyCapture() {
        DuelsPlayer player = parse(DuelsMode.BRIDGE,
            "\"bridge_duel_wins\":20,\"capture_threes_wins\":5,\"bridge_duel_kills\":2,\"bridge_duel_bridge_kills\":8,\"capture_threes_bridge_kills\":5,\"bridge_duel_bridge_deaths\":3");
        assertEquals(25, player.getWins());
        assertEquals(15, player.getKills());
        assertEquals(3, player.getDeaths());
        assertEquals(5.0, player.getKdr(), 0.0);
        assertEquals("§7Rookie", player.getDivision());
    }

    @Test public void spleefIsSeparateFromBowAndUhcIncludesAllQueues() {
        String stats = "\"bow_duel_wins\":900,\"bowspleef_duel_wins\":30,\"spleef_duel_wins\":20";
        assertEquals(50, parse(DuelsMode.SPLEEF, stats).getWins());
        assertEquals(900, parse(DuelsMode.BOW, stats).getWins());
        assertEquals(100, parse(DuelsMode.UHC,
            "\"uhc_duel_wins\":10,\"uhc_doubles_wins\":20,\"uhc_four_wins\":30,\"uhc_meetup_wins\":40").getWins());
    }

    @Test public void streaksPreserveUnknownAndNeverSubstituteOverallOrAnotherQueue() {
        assertEquals("§7?", parse(DuelsMode.CLASSIC,
            "\"classic_duel_wins\":1,\"current_winstreak_mode_classic_duel\":10,\"current_winstreak\":99").getFormattedWinstreakWithColor());
        assertEquals(3, parse(DuelsMode.CLASSIC,
            "\"classic_duel_wins\":1,\"current_classic_winstreak\":3").getWinstreak());
        assertEquals(0, parse(DuelsMode.BOW,
            "\"bow_duel_wins\":1,\"current_winstreak_mode_bow_duel\":0").getWinstreak());
        assertEquals(4, parse(DuelsMode.OVERALL,
            "\"currentStreak\":4,\"current_winstreak\":9").getWinstreak());
    }
    @Test public void modeDetectionDistinguishesOverlappingNamesAndNewQueues() {
        assertMode(DuelsMode.SPLEEF, "BOWSPLEEF_DUEL", "");
        assertMode(DuelsMode.SPLEEF, "", "BOW SPLEEF DUELS");
        assertMode(DuelsMode.BOW, "BOW_DUEL", "");
        assertMode(DuelsMode.CLASSIC, "CLASSIC_DOUBLES", "");
        assertMode(DuelsMode.BRIDGE, "CAPTURE_THREES", "");
        assertMode(DuelsMode.BEDWARS, "BEDWARS_TWO_ONE_DUELS_RUSH", "");
        assertMode(DuelsMode.QUAKE, "QUAKE_DUEL", "");
        assertMode(DuelsMode.ARENA, "DUEL_ARENA", "");
    }

    private void assertMode(DuelsMode expected, String mode, String title) {
        assertEquals(expected, DuelsMode.fromSnapshot(new com.roxiun.mellow.gamestate.GameSnapshot(
            true, "", net.hypixel.data.type.GameType.DUELS, mode, "",
            com.roxiun.mellow.gamestate.GamePhase.LIVE, title,
            java.util.Collections.emptyList(), null, 0, 0)));
    }

}
