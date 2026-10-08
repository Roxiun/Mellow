package com.roxiun.mellow.commands;
import org.junit.Test;
import static org.junit.Assert.*;
public class BlacklistAddRequestTest {
    @Test public void preservesLocalReason() {
        BlacklistAddRequest r = BlacklistAddRequest.parse("/blacklist", new String[]{"add", "Player", "Repeated", "sniping"});
        assertEquals("Player", r.getPlayerName());
        assertEquals("Repeated sniping", r.getLocalReason());
    }
    @Test public void defaultsMissingReason() {
        assertEquals("(none)", BlacklistAddRequest.parse("/blacklist", new String[]{"add", "Player"}).getLocalReason());
    }
}
