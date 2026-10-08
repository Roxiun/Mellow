package com.roxiun.mellow.feature.bedwars;
import org.junit.Test;
import static org.junit.Assert.*;
public class BedwarsSignalsTest {
    @Test public void playerChatCannotPurchaseUpgrades() {
        assertTrue(BedwarsChatSignalParser.isPurchaseMessage("You purchased Reinforced Armor IV"));
        assertTrue(BedwarsChatSignalParser.isPurchaseMessage("Teammate purchased Sharpened Swords"));
        assertFalse(BedwarsChatSignalParser.isPurchaseMessage("[MVP+] Example: I purchased Reinforced Armor IV"));
        assertFalse(BedwarsChatSignalParser.isPurchaseMessage("Example: You purchased Sharpened Swords"));
    }
    @Test public void countdownIsNotLiveEvidence() {
        assertFalse(BedwarsChatSignalParser.isBedwarsStartMessage("The game starts in 1 seconds!"));
        assertTrue(BedwarsChatSignalParser.isBedwarsStartMessage("Protect your bed and destroy the enemy beds."));
        assertTrue(BedwarsChatSignalParser.isBedwarsRespawnMessage("You will respawn because you still have a bed!"));
        assertFalse(BedwarsChatSignalParser.isBedwarsStartMessage("Player: Protect your bed and destroy the enemy beds."));
    }
    @Test public void respawnPreservesPurchasedUpgrades() {
        BedwarsFeatures features = new BedwarsFeatures();
        com.roxiun.mellow.gamestate.GameSnapshot snapshot = new com.roxiun.mellow.gamestate.GameSnapshot(
            true, "mini", net.hypixel.data.type.GameType.BEDWARS, "", "", com.roxiun.mellow.gamestate.GamePhase.LIVE,
            "BED WARS", java.util.Collections.emptyList(), com.roxiun.mellow.gamestate.PartyState.empty(), 1, 1);
        features.onChat("You purchased Reinforced Armor IV", snapshot);
        assertEquals(4, features.getUpgradesService().getReinforcedArmor());
        features.onChat("You will respawn because you still have a bed!", snapshot);
        assertEquals(4, features.getUpgradesService().getReinforcedArmor());
        features.onChat("Player: I purchased Reinforced Armor IV", snapshot);
        features.reset();
        assertEquals(0, features.getUpgradesService().getReinforcedArmor());
    }
}
