package com.roxiun.mellow.commands;

import com.mojang.authlib.GameProfile;
import com.roxiun.mellow.api.mojang.MojangApi;
import com.roxiun.mellow.api.seraph.SeraphApi;
import com.roxiun.mellow.api.seraph.SeraphTag;
import com.roxiun.mellow.config.MellowOneConfig;
import com.roxiun.mellow.core.async.AsyncExecutor;
import com.roxiun.mellow.core.async.MainThreadDispatcher;
import com.roxiun.mellow.util.ChatUtils;
import com.roxiun.mellow.util.blacklist.BlacklistCommandResolver;
import com.roxiun.mellow.util.formatting.FormattingUtils;
import com.roxiun.mellow.util.player.PlayerUtils;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.util.BlockPos;

public class SeraphCommand extends CommandBase {

    private final SeraphApi seraphApi;
    private final MojangApi mojangApi;
    private final MellowOneConfig config;

    public SeraphCommand(
        SeraphApi seraphApi,
        MojangApi mojangApi,
        MellowOneConfig config
    ) {
        this.seraphApi = seraphApi;
        this.mojangApi = mojangApi;
        this.config = config;
    }

    @Override
    public String getCommandName() {
        return "seraph";
    }

    @Override
    public String getCommandUsage(ICommandSender sender) {
        return "/seraph <username>";
    }

    @Override
    public List<String> getCommandAliases() {
        if (!BlacklistCommandResolver.isSeraphLoaded()) {
            return Collections.emptyList();
        }
        return Arrays.asList("mseraph");
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args) {
        if (!com.roxiun.mellow.api.seraph.SeraphAvailability.isEnabled()) {
            ChatUtils.sendCommandMessage(sender, "§e" + com.roxiun.mellow.api.seraph.SeraphAvailability.DISABLED_MESSAGE);
            return;
        }
        if (args.length != 1) {
            ChatUtils.sendCommandMessage(
                sender,
                "§cInvalid usage! Use /seraph <username>"
            );
            return;
        }

        String username = args[0];

        ChatUtils.sendCommandMessage(
            sender,
            "§r§3Fetching Seraph tags for §b" + username + "§3..."
        );
        AsyncExecutor.getInstance().command(() -> {
            try {
                UUID uuid = PlayerUtils.resolveLookupUuid(username, mojangApi);
                if (uuid == null) {
                    MainThreadDispatcher.run(() ->
                        ChatUtils.sendCommandMessage(
                            sender,
                            "§cFailed to fetch UUID for: §r" + username
                        )
                    );
                    return;
                }

                List<SeraphTag> tags = seraphApi.fetchSeraphTags(
                    uuid.toString(),
                    config.seraphKey
                );

                MainThreadDispatcher.run(() -> {
                    if (tags == null || tags.isEmpty()) {
                        ChatUtils.sendCommandMessage(
                            sender,
                            "§aNo Seraph tags found for: §r" + username
                        );
                    } else {
                        String formattedTags = FormattingUtils.formatSeraphTags(
                            tags
                        );
                        if (
                            formattedTags != null &&
                            !formattedTags.trim().isEmpty()
                        ) {
                            // Split the formatted tags by the newline separator and send as separate messages
                            String[] tagMessages = formattedTags.split("\n§c");
                            if (tagMessages.length > 0) {
                                // Send the first tag with the main message
                                ChatUtils.sendCommandMessage(
                                    sender,
                                    "§b" +
                                        username +
                                        " §3is tagged for: " +
                                        tagMessages[0]
                                );
                                // Send additional tags as separate messages
                                for (int i = 1; i < tagMessages.length; i++) {
                                    ChatUtils.sendCommandMessage(
                                        sender,
                                        "§c" + tagMessages[i]
                                    );
                                }
                            }
                        } else {
                            // Even though we couldn't format the tags, let the user know they exist
                            ChatUtils.sendCommandMessage(
                                sender,
                                "§c" +
                                    username +
                                    " has Seraph tags, but they couldn't be formatted."
                            );
                        }
                    }
                });
            } catch (Exception e) {
                MainThreadDispatcher.run(() ->
                    ChatUtils.sendCommandMessage(
                        sender,
                        "§cAn error occurred while fetching Seraph tags for " +
                            username +
                            "."
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
                Minecraft.getMinecraft()
                    .getNetHandler()
                    .getPlayerInfoMap()
                    .stream()
                    .map(NetworkPlayerInfo::getGameProfile)
                    .map(GameProfile::getName)
                    .toArray(String[]::new)
            );
        }
        return null;
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 0;
    }
}
