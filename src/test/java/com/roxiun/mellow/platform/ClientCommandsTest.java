package com.roxiun.mellow.platform;

import java.util.List;
import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

public class ClientCommandsTest {
    @BeforeClass public static void registerCommand() {
        ClientCommands.register(new CommandBase() {
            public String getCommandName() { return "mellow"; }
            public String getCommandUsage(ICommandSender sender) { return "/mellow"; }
            public List<String> getCommandAliases() { return List.of("mellowalias"); }
            public void processCommand(ICommandSender sender, String[] args) {}
        });
    }

    @Test public void completesPartialNamesAndAliases() {
        assertEquals(List.of("/mellow", "/mellowalias"), ClientCommands.completeCommandName("/mel"));
        assertEquals(List.of("/mellowalias"), ClientCommands.completeCommandName("/mellowa"));
    }

    @Test public void leavesChatAndArgumentsOutOfNameCompletion() {
        assertTrue(ClientCommands.completeCommandName("mel").isEmpty());
        assertTrue(ClientCommands.completeCommandName("/mellow ").isEmpty());
        assertTrue(ClientCommands.completeCommandName("/unrelated").isEmpty());
        assertTrue(ClientCommands.owns("/mellow argument"));
        assertFalse(ClientCommands.owns("/mel"));
    }
}
