package com.roxiun.mellow.commands;

import com.roxiun.mellow.api.mojang.MojangApi;
import com.roxiun.mellow.core.async.AsyncExecutor;
import com.roxiun.mellow.core.async.MainThreadDispatcher;
import com.roxiun.mellow.feature.nicks.NickUtils;
import com.roxiun.mellow.util.ChatUtils;
import com.roxiun.mellow.util.UUIDUtils;
import com.roxiun.mellow.util.localdenick.LocalDenickManager;
import com.roxiun.mellow.util.localdenick.LocalDenickedPlayer;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.util.BlockPos;
import net.minecraft.util.ChatComponentText;

public class NicksCommand extends CommandBase {

    private final LocalDenickManager localDenickManager;
    private final MojangApi mojangApi;
    private final NickUtils nickUtils;
    private static final String BASE_COMMAND = "mnick";

    public NicksCommand(
        LocalDenickManager localDenickManager,
        MojangApi mojangApi,
        NickUtils nickUtils
    ) {
        this.localDenickManager = localDenickManager;
        this.mojangApi = mojangApi;
        this.nickUtils = nickUtils;
    }

    public NicksCommand(
        LocalDenickManager localDenickManager,
        MojangApi mojangApi
    ) {
        this(localDenickManager, mojangApi, null);
    }

    @Override
    public String getCommandName() {
        return BASE_COMMAND;
    }

    @Override
    public String getCommandUsage(ICommandSender sender) {
        return "/" + BASE_COMMAND + " <add | remove | list | self | clear>";
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args) {
        if (args.length < 1) {
            ChatUtils.sendCommandMessage(
                sender,
                "§cInvalid usage! Use " + getCommandUsage(sender)
            );
            return;
        }

        String subCommand = args[0];

        if ("list".equalsIgnoreCase(subCommand)) {
            Map<UUID, LocalDenickedPlayer> localDenickList =
                localDenickManager.getLocalDenickList();
            if (localDenickList.isEmpty()) {
                ChatUtils.sendCommandMessage(sender, "§aThe local nicks list is empty.");
                return;
            }

            int page = 1;
            int pageSize = 10;

            if (args.length > 1) {
                try {
                    page = Integer.parseInt(args[1]);
                    if (page < 1) {
                        page = 1;
                    }
                } catch (NumberFormatException e) {
                    ChatUtils.sendCommandMessage(
                        sender,
                        "§cInvalid page number. Using page 1."
                    );
                }
            }

            List<LocalDenickedPlayer> players = new java.util.ArrayList<>(
                localDenickList.values()
            );
            int totalPlayers = players.size();
            int totalPages = (int) Math.ceil((double) totalPlayers / pageSize);

            if (page > totalPages) {
                page = totalPages;
                if (totalPages == 0) {
                    ChatUtils.sendCommandMessage(
                        sender,
                        "§aThe local nicks list is empty."
                    );
                    return;
                }
            }

            int startIndex = (page - 1) * pageSize;
            int endIndex = Math.min(startIndex + pageSize, totalPlayers);

            ChatUtils.sendCommandMessage(
                sender,
                "§aPlayers on your local nicks list (Page " +
                page +
                "/" +
                totalPages +
                "):"
            );
            for (int i = startIndex; i < endIndex; i++) {
                LocalDenickedPlayer player = players.get(i);
                sender.addChatMessage(
                    new ChatComponentText(
                        "§r- " + player.getName() + " -> " + player.getNick()
                    )
                );
            }

            if (totalPages > 1) {
                String navigationMessage =
                    "§7Use §f/" + BASE_COMMAND + " list <page>§7 to navigate";
                if (page < totalPages) {
                    navigationMessage +=
                        " (Next: §f/" +
                        BASE_COMMAND +
                        " list " +
                        (page + 1) +
                        "§7)";
                }
                ChatUtils.sendCommandMessage(sender, navigationMessage);
            }
            return;
        }

        if ("clear".equalsIgnoreCase(subCommand)) {
            int removedCount = localDenickManager.clearAllPlayers();
            if (removedCount == 0) {
                ChatUtils.sendCommandMessage(
                    sender,
                    "§aThe local nicks list is already empty."
                );
                return;
            }

            ChatUtils.sendCommandMessage(
                sender,
                "§aCleared " +
                removedCount +
                " entr" +
                (removedCount == 1 ? "y" : "ies") +
                " from the local nicks list."
            );
            return;
        }

        if ("self".equalsIgnoreCase(subCommand)) {
            if (args.length < 2) {
                ChatUtils.sendCommandMessage(
                    sender,
                    "§cUsage: /" + BASE_COMMAND + " self <nick>"
                );
                return;
            }

            String selfName = sender == null ? null : sender.getName();
            if (selfName == null || selfName.trim().isEmpty()) {
                ChatUtils.sendCommandMessage(sender, "§cCould not resolve your username.");
                return;
            }

            String nick = String.join(
                " ",
                Arrays.copyOfRange(args, 1, args.length)
            ).trim();
            if (nick.isEmpty()) {
                ChatUtils.sendCommandMessage(
                    sender,
                    "§cUsage: /" + BASE_COMMAND + " self <nick>"
                );
                return;
            }

            addPlayerByName(sender, selfName, nick);
            return;
        }

        if ("add".equalsIgnoreCase(subCommand) && args.length < 3) {
            ChatUtils.sendCommandMessage(
                sender,
                "§cUsage: /" + BASE_COMMAND + " add <player> <nick>"
            );
            return;
        }

        if (
            !"add".equalsIgnoreCase(subCommand) &&
            !"remove".equalsIgnoreCase(subCommand)
        ) {
            ChatUtils.sendCommandMessage(
                sender,
                "§cInvalid subcommand! Use 'add', 'remove', 'list', 'self', or 'clear'."
            );
            return;
        }

        if (args.length < 2) {
            ChatUtils.sendCommandMessage(
                sender,
                "§cInvalid usage! Use " + getCommandUsage(sender)
            );
            return;
        }

        String playerName = args[1];

        if ("add".equalsIgnoreCase(subCommand)) {
            String nick = String.join(
                " ",
                Arrays.copyOfRange(args, 2, args.length)
            ).trim();
            if (nick.isEmpty()) {
                ChatUtils.sendCommandMessage(
                    sender,
                    "§cUsage: /" + BASE_COMMAND + " add <player> <nick>"
                );
                return;
            }
            addPlayerByName(sender, playerName, nick);
            return;
        }

        AsyncExecutor.getInstance().command(() -> {
            String uuidString = mojangApi.getUUIDFromName(playerName);
            if (uuidString == null) {
                uuidString = mojangApi.fetchUUID(playerName);
            }

            if (uuidString == null || uuidString.equals("ERROR")) {
                MainThreadDispatcher.run(() ->
                    ChatUtils.sendCommandMessage(
                        sender,
                        "§cCould not find player: " + playerName
                    )
                );
                return;
            }

            UUID uuid = UUIDUtils.fromString(uuidString);

            localDenickManager.removePlayer(uuid);
            MainThreadDispatcher.run(() ->
                ChatUtils.sendCommandMessage(
                    sender,
                    "§aRemoved " + playerName + " from the local nicks list."
                )
            );
        });
    }

    private void addPlayerByName(ICommandSender sender, String playerName, String nick) {
        AsyncExecutor.getInstance().command(() -> {
            String uuidString = mojangApi.getUUIDFromName(playerName);
            if (uuidString == null) {
                uuidString = mojangApi.fetchUUID(playerName);
            }

            if (uuidString == null || uuidString.equals("ERROR")) {
                MainThreadDispatcher.run(() ->
                    ChatUtils.sendCommandMessage(
                        sender,
                        "§cCould not find player: " + playerName
                    )
                );
                return;
            }

            UUID uuid = UUIDUtils.fromString(uuidString);

            boolean playerAdded = localDenickManager.addPlayer(uuid, playerName, nick);
            if (playerAdded) {
                MainThreadDispatcher.run(() ->
                    ChatUtils.sendCommandMessage(
                        sender,
                        "§aAdded " +
                        playerName +
                        " with local nick " +
                        nick +
                        " to the local nicks list."
                    )
                );
                if (nickUtils != null) {
                    MainThreadDispatcher.run(() -> nickUtils.refreshLocalNickIfVisible(nick));
                }
                return;
            }

            MainThreadDispatcher.run(() ->
                ChatUtils.sendCommandMessage(
                    sender,
                    "§c" +
                    playerName +
                    " is already on the local nicks list with local nick: " +
                    localDenickManager.getLocalDenickedPlayer(uuid).getNick()
                )
            );
        });
    }

    @Override
    public List<String> addTabCompletionOptions(
        ICommandSender sender,
        String[] args,
        BlockPos pos
    ) {
        if (args.length == 1) {
            return getListOfStringsMatchingLastWord(
                args,
                "add",
                "remove",
                "list",
                "self",
                "clear"
            );
        }

        if (args.length == 2 && "list".equalsIgnoreCase(args[0])) {
            List<String> numbers = new java.util.ArrayList<>();
            for (int i = 1; i <= 10; i++) {
                numbers.add(String.valueOf(i));
            }
            return getListOfStringsMatchingLastWord(
                args,
                numbers.toArray(new String[0])
            );
        }

        return null;
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 0;
    }
}
