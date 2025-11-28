package com.roxiun.mellow.commands;

import com.roxiun.mellow.api.mojang.MojangApi;
import com.roxiun.mellow.util.ChatUtils;
import com.roxiun.mellow.util.blacklist.BlacklistManager;
import com.roxiun.mellow.util.blacklist.BlacklistedPlayer;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.util.BlockPos;
import net.minecraft.util.ChatComponentText;

public class BlacklistCommand extends CommandBase {

    private final BlacklistManager blacklistManager;
    private final MojangApi mojangApi;

    public BlacklistCommand(
        BlacklistManager blacklistManager,
        MojangApi mojangApi
    ) {
        this.blacklistManager = blacklistManager;
        this.mojangApi = mojangApi;
    }

    @Override
    public String getCommandName() {
        return "blacklist";
    }

    @Override
    public List<String> getCommandAliases() {
        return Arrays.asList("bl");
    }

    @Override
    public String getCommandUsage(ICommandSender sender) {
        return "/blacklist <add | remove | list> <player> [reason]";
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

            ChatUtils.sendCommandMessage(sender, "§aBlacklisted players:");
            for (BlacklistedPlayer player : blacklist.values()) {
                sender.addChatMessage(
                    new ChatComponentText(
                        "§r- " + player.getName() + ": " + player.getReason()
                    )
                );
            }
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

        new Thread(() -> {
            String uuidString = mojangApi.getUUIDFromName(playerName);
            if (uuidString == null) {
                uuidString = mojangApi.fetchUUID(playerName);
            }

            if (uuidString == null || uuidString.equals("ERROR")) {
                Minecraft.getMinecraft().addScheduledTask(() ->
                    ChatUtils.sendCommandMessage(
                        sender,
                        "§cCould not find player: " + playerName
                    )
                );
                return;
            }

            // Mojang API returns UUID without dashes, need to re-add them
            if (!uuidString.contains("-")) {
                uuidString = uuidString.replaceFirst(
                    "([0-9a-fA-F]{8})([0-9a-fA-F]{4})([0-9a-fA-F]{4})([0-9a-fA-F]{4})([0-9a-fA-F]{12})",
                    "$1-$2-$3-$4-$5"
                );
            }

            UUID uuid = UUID.fromString(uuidString);

            if ("add".equalsIgnoreCase(subCommand)) {
                String reason = "";
                if (args.length < 3) {
                    reason = "(none)";
                } else {
                    reason = String.join(
                        " ",
                        Arrays.copyOfRange(args, 2, args.length)
                    );
                }

                blacklistManager.addPlayer(uuid, playerName, reason);
                Minecraft.getMinecraft().addScheduledTask(() ->
                    ChatUtils.sendCommandMessage(
                        sender,
                        "§aAdded " + playerName + " to the blacklist."
                    )
                );
            } else if ("remove".equalsIgnoreCase(subCommand)) {
                blacklistManager.removePlayer(uuid);
                Minecraft.getMinecraft().addScheduledTask(() ->
                    ChatUtils.sendCommandMessage(
                        sender,
                        "§aRemoved " + playerName + " from the blacklist."
                    )
                );
            } else {
                Minecraft.getMinecraft().addScheduledTask(() ->
                    ChatUtils.sendCommandMessage(
                        sender,
                        "§cInvalid subcommand! Use 'add', 'remove', or 'list'."
                    )
                );
            }
        })
            .start();
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
                "list"
            );
        }
        return null;
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 0;
    }
}
