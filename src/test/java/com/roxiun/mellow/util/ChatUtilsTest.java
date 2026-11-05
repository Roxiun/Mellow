package com.roxiun.mellow.util;

import org.junit.Test;
import static org.junit.Assert.*;

public class ChatUtilsTest {
    @Test public void limitsTheWholeCommandIncludingItsPrefix() {
        String longText = String.join("", java.util.Collections.nCopies(120, "x"));
        for (String prefix : new String[]{"/pc", "ac", "/msg Player"}) {
            String command = ChatUtils.formatChatCommandMessage(prefix, longText);
            assertEquals(100, command.length());
            assertTrue(command.endsWith("..."));
            String normalized = prefix.startsWith("/") ? prefix : "/" + prefix;
            assertEquals(normalized + " " + longText.substring(0, 99 - normalized.length()),
                ChatUtils.formatChatCommandMessage(prefix, longText.substring(0, 99 - normalized.length())));
        }
    }

    @Test public void removesFormattingAndLineBreaksAndRejectsEmptyMessages() {
        assertEquals("/pc Hello world !", ChatUtils.formatChatCommandMessage(" pc ", "§cHello\nworld\r!"));
        assertNull(ChatUtils.formatChatCommandMessage("/pc", "§c "));
        assertNull(ChatUtils.formatChatCommandMessage(null, "hello"));
        assertNull(ChatUtils.formatChatCommandMessage("/pc", null));
        assertNull(ChatUtils.formatChatCommandMessage(String.join("", java.util.Collections.nCopies(100, "x")), "hello"));
    }

    @Test public void truncationDoesNotSplitUnicodeCharacters() {
        String text = String.join("", java.util.Collections.nCopies(92, "x")) + "\uD83D\uDE00more text";
        String result = ChatUtils.formatChatCommandMessage("/pc", text);
        assertEquals("/pc " + text.substring(0, 92) + "...", result);
    }
}
