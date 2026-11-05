package com.roxiun.mellow.platform;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.suggestion.Suggestions;
import java.lang.reflect.Proxy;
import java.util.List;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.util.BlockPos;
import net.minecraft.util.IChatComponent;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

public class ClientCommandsTest {
    private final CommandDispatcher<Object> dispatcher = new CommandDispatcher<>();
    private String[] executed;
    private String[] completed;
    private String error;

    @Before public void registerCommand() {
        ICommandSender sender = (ICommandSender) Proxy.newProxyInstance(
            getClass().getClassLoader(), new Class<?>[]{ICommandSender.class}, (proxy, method, args) -> {
                if (method.getName().equals("getPosition")) return new BlockPos(0, 0, 0);
                if (method.getName().equals("addChatMessage")) error = ((IChatComponent) args[0]).getUnformattedText();
                return null;
            });
        CommandBase command = new CommandBase() {
            public String getCommandName() { return "mellow"; }
            public String getCommandUsage(ICommandSender sender) { return "/mellow"; }
            public void processCommand(ICommandSender sender, String[] args) throws CommandException {
                if (args.length > 0 && args[0].equals("fail")) throw new CommandException("Invalid player");
                executed = args;
            }
            public List<String> addTabCompletionOptions(ICommandSender sender, String[] args, BlockPos pos) {
                completed = args;
                return List.of("Alice", "Alex", "Bob");
            }
        };
        dispatcher.getRoot().addChild(ClientCommands.createNode("mellow", command, () -> sender));
        dispatcher.getRoot().addChild(ClientCommands.createNode("mellowalias", command, () -> sender));
    }

    @Test public void dispatchesBareCommandsAndAliasArguments() throws Exception {
        dispatcher.execute("mellow", new Object());
        assertArrayEquals(new String[0], executed);
        dispatcher.execute("mellowalias player   42", new Object());
        assertArrayEquals(new String[]{"player", "42"}, executed);
    }

    @Test public void completesNamesAndReplacesOnlyCurrentArgument() {
        assertEquals(List.of("mellow", "mellowalias"), suggestions("mel").getList().stream().map(s -> s.getText()).toList());
        Suggestions result = suggestions("mellow stats Al");
        assertArrayEquals(new String[]{"stats", "Al"}, completed);
        assertEquals(List.of("mellow stats Alex", "mellow stats Alice"),
            result.getList().stream().map(s -> s.apply("mellow stats Al")).toList());
        suggestions("mellow stats ");
        assertArrayEquals(new String[]{"stats", ""}, completed);
    }

    @Test public void reportsCommandErrorsLocally() throws Exception {
        assertEquals(0, dispatcher.execute("mellow fail", new Object()));
        assertNotNull(error);
        assertTrue(error.contains("Invalid player"));
        assertNull(executed);
    }

    private Suggestions suggestions(String input) {
        return dispatcher.getCompletionSuggestions(dispatcher.parse(input, new Object())).join();
    }
}
