package com.roxiun.mellow.feature.stats;

import org.junit.Test;
import static org.junit.Assert.*;

public class PregameStatsTest {
    @Test public void commandUsageDoesNotTriggerAPlayerLookup() {
        assertNull(PregameStats.parseChatMessage("Usage: /party chat <message>"));
        assertNull(PregameStats.parseChatMessage("Usage: /party invite <player>"));
    }

    @Test public void ordinaryChatStillExtractsTheSenderAndContent() {
        PregameStats.ParsedChatMessage plain = PregameStats.parseChatMessage("Player_1: hello");
        assertEquals("Player_1", plain.sender);
        assertEquals("hello", plain.content);
        PregameStats.ParsedChatMessage ranked = PregameStats.parseChatMessage("[MVP+] Player_1 » hello");
        assertEquals("Player_1", ranked.sender);
        assertEquals("hello", ranked.content);
    }

    @Test public void usageIsNotBannedAsAPlayerNameOrInsideChat() {
        assertEquals("Usage", PregameStats.parseChatMessage("Usage: hello").sender);
        assertEquals("Usage", PregameStats.parseChatMessage("[MVP+] Usage: /party chat <message>").sender);
        assertEquals("Player_1", PregameStats.parseChatMessage("Player_1: Usage: /party chat <message>").sender);
    }
}
