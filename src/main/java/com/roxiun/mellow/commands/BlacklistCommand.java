package com.roxiun.mellow.commands;

import com.mojang.authlib.GameProfile;
import com.roxiun.mellow.Mellow;
import com.roxiun.mellow.api.mojang.MojangApi;
import com.roxiun.mellow.core.async.AsyncExecutor;
import com.roxiun.mellow.core.async.MainThreadDispatcher;
import com.roxiun.mellow.util.ChatUtils;
import com.roxiun.mellow.util.UUIDUtils;
import com.roxiun.mellow.util.blacklist.BlacklistCommandResolver;
import com.roxiun.mellow.util.blacklist.BlacklistManager;
import com.roxiun.mellow.util.blacklist.BlacklistedPlayer;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.util.BlockPos;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.ChatStyle;
import net.minecraft.event.ClickEvent;

public class BlacklistCommand extends CommandBase {

    private final BlacklistManager blacklistManager;
    private final MojangApi mojangApi;
    private final String baseCommand;

    public BlacklistCommand(
        BlacklistManager blacklistManager,
        MojangApi mojangApi
    ) {
        this.blacklistManager = blacklistManager;
        this.mojangApi = mojangApi;
        this.baseCommand = BlacklistCommandResolver.getBaseCommand();
    }

    @Override
    public String getCommandName() {
        return baseCommand;
    }

    @Override
    public List<String> getCommandAliases() {
        return Arrays.asList("bl", "mblacklist");
    }

    @Override
    public String getCommandUsage(ICommandSender sender) {
        return getCommandPrefix() + " <add | remove | list | import>";
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
            Map<UUID, BlacklistedPlayer> blacklist =
                blacklistManager.getBlacklist();
            if (blacklist.isEmpty()) {
                ChatUtils.sendCommandMessage(
                    sender,
                    "§aThe blacklist is empty."
                );
                return;
            }

            // Pagination: default to page 1 if no page number provided
            int page = 1;
            int pageSize = 10; // Show 10 entries per page

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

            // Convert the map values to a list for pagination
            List<Map.Entry<UUID, BlacklistedPlayer>> players = new ArrayList<>(
                blacklist.entrySet()
            );
            int totalPlayers = players.size();
            int totalPages = (int) Math.ceil((double) totalPlayers / pageSize);

            if (page > totalPages) {
                page = totalPages;
                if (totalPages == 0) {
                    ChatUtils.sendCommandMessage(
                        sender,
                        "§aThe blacklist is empty."
                    );
                    return;
                }
            }

            int startIndex = (page - 1) * pageSize;
            int endIndex = Math.min(startIndex + pageSize, totalPlayers);

            ChatUtils.sendCommandMessage(
                sender,
                "§aBlacklisted players (Page " + page + "/" + totalPages + "):"
            );
            for (int i = startIndex; i < endIndex; i++) {
                Map.Entry<UUID, BlacklistedPlayer> entry = players.get(i);
                BlacklistedPlayer player = entry.getValue();
                ChatComponentText line = new ChatComponentText(
                    "§r- " + player.getName() + ": " + player.getReason()
                );
                line.appendSibling(new ChatComponentText(" §c[Remove]").setChatStyle(
                    new ChatStyle().setChatClickEvent(new ClickEvent(
                        ClickEvent.Action.SUGGEST_COMMAND,
                        getCommandPrefix() + " remove " + entry.getKey()
                    ))
                ));
                sender.addChatMessage(line);
            }

            // Show navigation help if there are multiple pages
            if (totalPages > 1) {
                String navigationMessage =
                    "§7Use §f" + getCommandPrefix() + " list <page>§7 to navigate";
                if (page < totalPages) {
                    navigationMessage +=
                        " (Next: §f" +
                        getCommandPrefix() +
                        " list " +
                        (page + 1) +
                        "§7)";
                }
                ChatUtils.sendCommandMessage(sender, navigationMessage);
            }
            return;
        } else if ("import".equalsIgnoreCase(subCommand)) {
            // Handle sync command: <baseCommand> import <filename>
            if (args.length < 2) {
                ChatUtils.sendCommandMessage(
                    sender,
                    "§cUsage: " + getCommandPrefix() + " import <filename>"
                );
                return;
            }

            String filename = args[1];

            // Construct file path - allow both relative to config/mellow and absolute paths
            File syncFile;
            if (filename.startsWith("/")) {
                // Absolute path
                syncFile = new File(filename);
            } else {
                // Relative to the config/mellow directory
                File configDir = new File(
                    Minecraft.getMinecraft().mcDataDir,
                    "config/mellow"
                );
                syncFile = new File(configDir, filename);
            }

            AsyncExecutor.getInstance().command(() -> {
                MainThreadDispatcher.run(() -> {
                    ChatUtils.sendCommandMessage(
                        sender,
                        "§eImporting blacklist with file: " +
                            syncFile.getAbsolutePath()
                    );
                });

                try {
                    int newEntries = blacklistManager.syncWithExternalFile(
                        syncFile
                    );
                    String message;
                    if (newEntries > 0) {
                        message =
                            "§aSuccessfully imported! Added " +
                            newEntries +
                            " new entries from " +
                            filename;
                    } else if (syncFile.exists()) {
                        message =
                            "§eImport completed! No new entries found in " +
                            filename;
                    } else {
                        message =
                            "§cFile not found: " + syncFile.getAbsolutePath();
                        MainThreadDispatcher.run(() ->
                            ChatUtils.sendCommandMessage(sender, message)
                        );
                        return;
                    }

                    MainThreadDispatcher.run(() ->
                        ChatUtils.sendCommandMessage(sender, message)
                    );
                } catch (Exception e) {
                    MainThreadDispatcher.run(() ->
                        ChatUtils.sendCommandMessage(
                            sender,
                            "§cError importing file: " + e.getMessage()
                        )
                    );
                }
            });
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
        final BlacklistAddRequest addRequest;
        if ("add".equalsIgnoreCase(subCommand)) {
            try {
                addRequest = BlacklistAddRequest.parse(getCommandPrefix(), args);
            } catch (IllegalArgumentException e) {
                ChatUtils.sendCommandMessage(sender, "§c" + e.getMessage());
                return;
            }
        } else {
            addRequest = null;
        }

        AsyncExecutor.getInstance().command(() -> {
            if ("remove".equalsIgnoreCase(subCommand)) {
                UUID storedUuid;
                try {
                    storedUuid = UUIDUtils.fromString(playerName);
                } catch (IllegalArgumentException invalidUuid) {
                    try {
                        storedUuid = blacklistManager.findPlayerByName(playerName);
                    } catch (IllegalArgumentException ambiguousName) {
                        MainThreadDispatcher.run(() -> ChatUtils.sendCommandMessage(
                            sender, "§c" + ambiguousName.getMessage()
                        ));
                        return;
                    }
                }
                if (storedUuid != null) {
                    removePlayer(sender, storedUuid, playerName);
                    return;
                }
            }

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

            if ("add".equalsIgnoreCase(subCommand)) {
                boolean playerAdded = blacklistManager.addPlayer(
                    uuid,
                    playerName,
                    addRequest.getLocalReason()
                );
                if (playerAdded) {

                    MainThreadDispatcher.run(() ->
                        {
                            ChatUtils.sendCommandMessage(
                                sender,
                                "§aAdded " + playerName + " to the blacklist."
                            );

                            maybeAlsoBlockNickedInGamePlayer(sender, playerName);
                        }
                    );
                } else {
                    BlacklistedPlayer existingPlayer =
                        blacklistManager.getBlacklistedPlayer(uuid);
                    String existingReason = existingPlayer == null
                        ? "(unknown)"
                        : existingPlayer.getReason();

                    MainThreadDispatcher.run(() ->
                        {
                            ChatUtils.sendCommandMessage(
                                sender,
                                "§c" +
                                    playerName +
                                    " is already on the blacklist for reason: " +
                                    existingReason
                            );

                            maybeAlsoBlockNickedInGamePlayer(sender, playerName);
                        }
                    );
                }
            } else if ("remove".equalsIgnoreCase(subCommand)) {
                removePlayer(sender, uuid, playerName);
            } else {
                MainThreadDispatcher.run(() ->
                    ChatUtils.sendCommandMessage(
                        sender,
                        "§cInvalid subcommand! Use 'add', 'remove', or 'list'."
                    )
                );
            }
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
                "import"
            );
        }

        if (args.length == 2 && "list".equalsIgnoreCase(args[0])) {
            // Provide number suggestions for page parameter
            List<String> numbers = new java.util.ArrayList<>();
            for (int i = 1; i <= 10; i++) {
                numbers.add(String.valueOf(i));
            }
            return getListOfStringsMatchingLastWord(
                args,
                numbers.toArray(new String[0])
            );
        }

        // For add/remove commands, provide player name completion
        if (
            args.length == 2 &&
            ("add".equalsIgnoreCase(args[0]) ||
                "remove".equalsIgnoreCase(args[0]))
        ) {
            String subCommand = args[0];
            List<String> suggestions = "remove".equalsIgnoreCase(subCommand)
                ? getStoredAndOnlinePlayerNames()
                : getOnlinePlayerNames();

            if (suggestions.isEmpty()) {
                return null;
            }

            return getListOfStringsMatchingLastWord(
                args,
                suggestions.toArray(new String[0])
            );
        }

        // For import command, return null to allow file name completion (or default behavior)
        if (args.length == 2 && "import".equalsIgnoreCase(args[0])) {
            return null; // Let the client handle file name completion
        }

        return null;
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 0;
    }

    private void removePlayer(ICommandSender sender, UUID uuid, String input) {
        BlacklistedPlayer player = blacklistManager.getBlacklistedPlayer(uuid);
        boolean removed = blacklistManager.removePlayer(uuid);
        String name = player == null ? input : player.getName();
        MainThreadDispatcher.run(() -> ChatUtils.sendCommandMessage(
            sender,
            removed ? "§aRemoved " + name + " from the blacklist."
                : "§c" + input + " is not on your blacklist."
        ));
    }

    private String getCommandPrefix() {
        return "/" + baseCommand;
    }

    private List<String> getOnlinePlayerNames() {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.getNetHandler() == null) {
            return java.util.Collections.emptyList();
        }

        List<String> names = new ArrayList<>();
        for (NetworkPlayerInfo info : mc.getNetHandler().getPlayerInfoMap()) {
            if (info == null) {
                continue;
            }
            GameProfile profile = info.getGameProfile();
            if (profile == null) {
                continue;
            }
            String name = profile.getName();
            if (name == null || name.trim().isEmpty()) {
                continue;
            }
            names.add(name);
        }
        return names;
    }

    private List<String> getStoredAndOnlinePlayerNames() {
        Set<String> names = new LinkedHashSet<>();
        for (BlacklistedPlayer player : blacklistManager.getBlacklist().values()) {
            if (player == null) {
                continue;
            }
            String name = player.getName();
            if (name == null || name.trim().isEmpty()) {
                continue;
            }
            names.add(name);
        }
        names.addAll(getOnlinePlayerNames());
        return new ArrayList<>(names);
    }

    private void maybeAlsoBlockNickedInGamePlayer(
        ICommandSender sender,
        String requestedName
    ) {
        String inGameName = resolveInGamePlayerName(requestedName);
        if (inGameName == null) {
            return;
        }
        if (Mellow.nickUtils == null || !Mellow.nickUtils.isNicked(inGameName)) {
            return;
        }

        Minecraft mc = Minecraft.getMinecraft();
        if (mc.thePlayer == null) {
            return;
        }

        mc.thePlayer.sendChatMessage("/block add " + inGameName);
        ChatUtils.sendCommandMessage(
            sender,
            "§7Also ran §f/block add " +
                inGameName +
                "§7 because they are nicked in your game."
        );
    }

    private String resolveInGamePlayerName(String requestedName) {
        if (requestedName == null) {
            return null;
        }
        String trimmed = requestedName.trim();
        if (trimmed.isEmpty()) {
            return null;
        }

        Minecraft mc = Minecraft.getMinecraft();
        if (mc.thePlayer == null || mc.getNetHandler() == null) {
            return null;
        }

        for (NetworkPlayerInfo info : mc.getNetHandler().getPlayerInfoMap()) {
            if (info == null || info.getGameProfile() == null) {
                continue;
            }
            String inGameName = info.getGameProfile().getName();
            if (inGameName != null && inGameName.equalsIgnoreCase(trimmed)) {
                return inGameName;
            }
        }

        return null;
    }

}
