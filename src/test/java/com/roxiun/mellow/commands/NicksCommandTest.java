package com.roxiun.mellow.commands;

import com.roxiun.mellow.api.mojang.MojangApi;
import com.roxiun.mellow.util.localdenick.LocalDenickManager;
import java.io.File;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.command.ICommandSender;
import net.minecraft.util.IChatComponent;
import org.junit.Assert;
import org.junit.Test;

public class NicksCommandTest {

    @Test
    public void usageContainsExpectedAddOrdering() {
        File tempDir = createTempDir();
        NicksCommand command = new NicksCommand(
            LocalDenickManager.createForTests(tempDir),
            new MojangApi()
        );

        Assert.assertEquals(
            "/mnick <add | remove | list | self | clear>",
            command.getCommandUsage(null)
        );
    }

    @Test
    public void clearAllPlayersRemovesEntriesAndReturnsRemovedCount() {
        File tempDir = createTempDir();
        LocalDenickManager manager = LocalDenickManager.createForTests(tempDir);
        manager.addPlayer(
            UUID.fromString("00000000-0000-0000-0000-000000000010"),
            "Alpha",
            "NickA"
        );
        manager.addPlayer(
            UUID.fromString("00000000-0000-0000-0000-000000000011"),
            "Beta",
            "NickB"
        );

        Assert.assertEquals(2, manager.clearAllPlayers());
        Assert.assertTrue(manager.getLocalDenickList().isEmpty());
    }

    @Test
    public void clearSubcommandEmptiesListAndAcknowledgesCount() {
        File tempDir = createTempDir();
        LocalDenickManager manager = LocalDenickManager.createForTests(tempDir);
        manager.addPlayer(
            UUID.fromString("00000000-0000-0000-0000-000000000012"),
            "Gamma",
            "NickG"
        );

        NicksCommand command = new NicksCommand(manager, new MojangApi());
        List<String> messages = new ArrayList<>();
        ICommandSender sender = createSender("Tester", messages);

        command.processCommand(sender, new String[] { "clear" });

        Assert.assertTrue(manager.getLocalDenickList().isEmpty());
        Assert.assertTrue(
            messages
                .stream()
                .anyMatch(m -> m.contains("Cleared 1 entry from the local nicks list."))
        );
    }

    @Test
    public void clearSubcommandOnEmptyListShowsAlreadyEmptyMessage() {
        File tempDir = createTempDir();
        LocalDenickManager manager = LocalDenickManager.createForTests(tempDir);
        NicksCommand command = new NicksCommand(manager, new MojangApi());
        List<String> messages = new ArrayList<>();
        ICommandSender sender = createSender("Tester", messages);

        command.processCommand(sender, new String[] { "clear" });

        Assert.assertTrue(manager.getLocalDenickList().isEmpty());
        Assert.assertTrue(
            messages
                .stream()
                .anyMatch(m -> m.contains("The local nicks list is already empty."))
        );
    }

    @Test
    public void firstArgTabCompletionIncludesSelfAndClear() {
        File tempDir = createTempDir();
        NicksCommand command = new NicksCommand(
            LocalDenickManager.createForTests(tempDir),
            new MojangApi()
        );

        List<String> completions = command.addTabCompletionOptions(
            null,
            new String[] { "" },
            null
        );

        Assert.assertTrue(completions.contains("self"));
        Assert.assertTrue(completions.contains("clear"));
    }

    @Test
    public void selfSubcommandWithoutNickDoesNotMutateManager() {
        File tempDir = createTempDir();
        LocalDenickManager manager = LocalDenickManager.createForTests(tempDir);
        NicksCommand command = new NicksCommand(manager, new MojangApi());
        List<String> messages = new ArrayList<>();
        ICommandSender sender = createSender("Tester", messages);

        command.processCommand(sender, new String[] { "self" });

        Assert.assertTrue(manager.getLocalDenickList().isEmpty());
        Assert.assertTrue(
            messages
                .stream()
                .anyMatch(m -> m.contains("Usage: /mnick self <nick>"))
        );
    }

    @Test
    public void selfSubcommandAddsSenderWithJoinedNick() {
        File tempDir = createTempDir();
        LocalDenickManager manager = LocalDenickManager.createForTests(tempDir);
        NicksCommand command = new NicksCommand(
            manager,
            new StubMojangApi("00000000-0000-0000-0000-000000000013")
        );
        ICommandSender sender = createSender("Tester", new ArrayList<>());

        command.processCommand(sender, new String[] { "self", "my", "nick" });

        UUID uuid = UUID.fromString("00000000-0000-0000-0000-000000000013");
        waitFor(() -> manager.getLocalDenickedPlayer(uuid) != null, 1000L);

        Assert.assertEquals("Tester", manager.getLocalDenickedPlayer(uuid).getName());
        Assert.assertEquals("my nick", manager.getLocalDenickedPlayer(uuid).getNick());
    }

    @Test
    public void addSubcommandStoresCanonicalMojangName() {
        File tempDir = createTempDir();
        LocalDenickManager manager = LocalDenickManager.createForTests(tempDir);
        NicksCommand command = new NicksCommand(
            manager,
            new StubMojangApi(
                null,
                "00000000-0000-0000-0000-000000000014",
                "RealSuper"
            )
        );
        ICommandSender sender = createSender("Tester", new ArrayList<>());

        command.processCommand(
            sender,
            new String[] { "add", "realsuper", "nick" }
        );

        UUID uuid = UUID.fromString("00000000-0000-0000-0000-000000000014");
        waitFor(() -> manager.getLocalDenickedPlayer(uuid) != null, 1000L);

        Assert.assertEquals(
            "RealSuper",
            manager.getLocalDenickedPlayer(uuid).getName()
        );
    }

    private static void waitFor(Check condition, long timeoutMillis) {
        long deadline = System.currentTimeMillis() + timeoutMillis;
        while (System.currentTimeMillis() < deadline) {
            if (condition.evaluate()) {
                return;
            }
            try {
                Thread.sleep(10L);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException(e);
            }
        }
        Assert.fail("Timed out waiting for async condition");
    }

    @FunctionalInterface
    private interface Check {
        boolean evaluate();
    }

    private static class StubMojangApi extends MojangApi {

        private final String tabUuid;
        private final String fetchedUuid;
        private final String fetchedName;

        private StubMojangApi(String uuid) {
            this(uuid, uuid, null);
        }

        private StubMojangApi(String tabUuid, String fetchedUuid, String fetchedName) {
            this.tabUuid = tabUuid;
            this.fetchedUuid = fetchedUuid;
            this.fetchedName = fetchedName;
        }

        @Override
        public String getUUIDFromName(String playerName) {
            return tabUuid;
        }

        @Override
        public String fetchUUID(String username) {
            return fetchedUuid;
        }

        @Override
        public MojangApi.ProfileLookup fetchProfileByName(String username) {
            if (fetchedUuid == null) {
                return null;
            }
            return new MojangApi.ProfileLookup(fetchedUuid, fetchedName);
        }
    }

    private ICommandSender createSender(String name, List<String> messages) {
        InvocationHandler handler = (Object proxy, Method method, Object[] args) -> {
            String methodName = method.getName();
            if ("getName".equals(methodName)) {
                return name;
            }
            if ("addChatMessage".equals(methodName) && args != null && args.length == 1) {
                Object arg = args[0];
                if (arg instanceof IChatComponent) {
                    messages.add(((IChatComponent) arg).getUnformattedText());
                } else if (arg != null) {
                    messages.add(arg.toString());
                }
                return null;
            }

            Class<?> returnType = method.getReturnType();
            if (returnType.equals(boolean.class)) {
                return false;
            }
            if (returnType.equals(int.class)) {
                return 0;
            }
            if (returnType.equals(float.class)) {
                return 0f;
            }
            if (returnType.equals(double.class)) {
                return 0d;
            }
            if (returnType.equals(long.class)) {
                return 0L;
            }
            return null;
        };

        return (ICommandSender) Proxy.newProxyInstance(
            ICommandSender.class.getClassLoader(),
            new Class<?>[] { ICommandSender.class },
            handler
        );
    }

    private File createTempDir() {
        try {
            File dir = Files.createTempDirectory("nicks-command-test").toFile();
            dir.deleteOnExit();
            return dir;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
