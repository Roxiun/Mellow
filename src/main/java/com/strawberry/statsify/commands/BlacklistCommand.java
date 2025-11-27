package com.strawberry.statsify.commands;

import com.strawberry.statsify.api.mojang.MojangApi;
import com.strawberry.statsify.util.blacklist.BlacklistManager;
import com.strawberry.statsify.util.blacklist.BlacklistedPlayer;
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
    public String getCommandUsage(ICommandSender sender) {
        return "/blacklist <add | remove | list> <player> [reason]";
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args) {
        if (args.length < 1) {
            sender.addChatMessage(
                new ChatComponentText(
                    "§r[§bStatsify§r]§c Invalid usage! Use " +
                        getCommandUsage(sender)
                )
            );
            return;
        }

        String subCommand = args[0];

        if ("list".equalsIgnoreCase(subCommand)) {
            Map<UUID, BlacklistedPlayer> blacklist =
                blacklistManager.getBlacklist();
            if (blacklist.isEmpty()) {
                sender.addChatMessage(
                    new ChatComponentText(
                        "§r[§bStatsify§r]§a The blacklist is empty."
                    )
                );
                return;
            }

            sender.addChatMessage(
                new ChatComponentText("§r[§bStatsify§r]§a Blacklisted players:")
            );
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
            sender.addChatMessage(
                new ChatComponentText(
                    "§r[§bStatsify§r]§c Invalid usage! Use " +
                        getCommandUsage(sender)
                )
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
                    sender.addChatMessage(
                        new ChatComponentText(
                            "§r[§bStatsify§r]§c Could not find player: " +
                                playerName
                        )
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
                if (args.length < 3) {
                    Minecraft.getMinecraft().addScheduledTask(() ->
                        sender.addChatMessage(
                            new ChatComponentText(
                                "§r[§bStatsify§r]§c Invalid usage! Use /blacklist add <player> <reason>"
                            )
                        )
                    );
                    return;
                }
                String reason = String.join(
                    " ",
                    Arrays.copyOfRange(args, 2, args.length)
                );
                blacklistManager.addPlayer(uuid, playerName, reason);
                Minecraft.getMinecraft().addScheduledTask(() ->
                    sender.addChatMessage(
                        new ChatComponentText(
                            "§r[§bStatsify§r]§a Added " +
                                playerName +
                                " to the blacklist."
                        )
                    )
                );
            } else if ("remove".equalsIgnoreCase(subCommand)) {
                blacklistManager.removePlayer(uuid);
                Minecraft.getMinecraft().addScheduledTask(() ->
                    sender.addChatMessage(
                        new ChatComponentText(
                            "§r[§bStatsify§r]§a Removed " +
                                playerName +
                                " from the blacklist."
                        )
                    )
                );
            } else {
                Minecraft.getMinecraft().addScheduledTask(() ->
                    sender.addChatMessage(
                        new ChatComponentText(
                            "§r[§bStatsify§r]§c Invalid subcommand! Use 'add', 'remove', or 'list'."
                        )
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
