package com.roxiun.mellow.feature.stats.tab;

import com.roxiun.mellow.gamestate.*;
import net.hypixel.data.type.GameType;
import org.junit.Test;
import java.util.Collections;
import static org.junit.Assert.*;

public class TabNameFormatterTest {
    private static final String RANKED = "§b[MVP§c+§b] Player";

    @Test public void safeQueuesUseRankColourAndPreserveSuffix() {
        for (String mode : new String[]{"TNTRUN", "BOWSPLEEF", "TNTAG"}) {
            assertEquals(RANKED + "§r§6 suffix§r", format(GameType.TNTGAMES, mode, "§fPlayer§6 suffix", true, false));
        }
        for (String mode : new String[]{"SUMO_DUEL", "CLASSIC_DUEL"}) {
            assertEquals(RANKED + "§r§r", format(GameType.DUELS, mode, "§fPlayer", true, false));
        }
    }

    @Test public void teamQueuesUnknownQueuesAndMurderKeepServerColours() {
        for (String mode : new String[]{"CLASSIC_DOUBLES", "BRIDGE_DUEL", "UHC_DOUBLES", "CLASSIC", "", "NEW_DUEL"}) {
            assertEquals("§cPlayer§r", format(GameType.DUELS, mode, "§cPlayer", true, false));
        }
        assertEquals("§cPlayer§r", format(GameType.BEDWARS, "eight_one", "§cPlayer", true, false));
        assertEquals("§aPlayer§r", format(GameType.MURDER_MYSTERY, "MURDER_INFECTION", "§aPlayer", true, false));
        assertEquals("§eAaron§r", format(GameType.MURDER_MYSTERY, "MURDER_ASSASSINS", "§eAaron", true, false));
    }

    @Test public void tntPossessionAndSpecialLabelsTakePrecedenceOverRank() {
        assertEquals("§cPlayer§r", format(GameType.TNTGAMES, "TNTAG", "§cPlayer", true, false));
        assertEquals("§c[TNT] §fPlayer§r", format(GameType.TNTGAMES, "TNTAG", "§c[TNT] §fPlayer", true, false));
        assertEquals("§8Player§r", format(GameType.TNTGAMES, "TNTAG", "§8Player", true, false));
    }

    @Test public void offSpectatorsNicksAndMissingRankKeepServerDisplay() {
        assertEquals("§fPlayer§r", format(GameType.TNTGAMES, "TNTRUN", "§fPlayer", false, false));
        assertEquals("§7§oPlayer§r", format(GameType.TNTGAMES, "TNTRUN", "§7§oPlayer", true, true));
        assertEquals("§fPlayerAlias§r", format(GameType.TNTGAMES, "TNTRUN", "§fPlayerAlias", true, false));
        assertEquals("§7[SPECTATOR] Player§r", format(GameType.TNTGAMES, "TNTRUN", "§7[SPECTATOR] Player", true, false));
        assertEquals("§fAlias§r", format(GameType.TNTGAMES, "TNTRUN", "§fAlias", true, false));
        assertEquals("§fPlayer§r", TabNameFormatter.format("Player", "§fPlayer", "", null,
            snapshot(GameType.TNTGAMES, "TNTRUN"), true, false));
        assertEquals("§fPlayer§r", TabNameFormatter.format("Player", "§fPlayer", "", "§b[MVP+] SomeoneElse",
            snapshot(GameType.TNTGAMES, "TNTRUN"), true, false));
    }

    @Test public void separateTeamColumnDoesNotDuplicateTeamPrefix() {
        assertEquals("§c§lPlayer§7 suffix§r", TabNameFormatter.format("Player", "§c§lR Player§7 suffix", "§c§lR ", RANKED,
            snapshot(GameType.BEDWARS, "eight_one"), true, false));
    }

    private String format(GameType type, String mode, String raw, boolean enabled, boolean preserve) {
        return TabNameFormatter.format("Player", raw, "", RANKED, snapshot(type, mode), enabled, preserve);
    }
    private static GameSnapshot snapshot(GameType type, String mode) {
        return new GameSnapshot(true, "mini", type, mode, "", GamePhase.LIVE,
            "", Collections.emptyList(), PartyState.empty(), 1, 1);
    }
}
