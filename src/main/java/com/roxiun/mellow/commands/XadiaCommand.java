package com.roxiun.mellow.commands;

import com.roxiun.mellow.api.xadia.XadiaTag;
import com.roxiun.mellow.api.xadia.XadiaApi;
import com.mojang.authlib.GameProfile;
import com.roxiun.mellow.config.MellowOneConfig;
import com.roxiun.mellow.core.async.AsyncExecutor;
import com.roxiun.mellow.core.async.MainThreadDispatcher;
import com.roxiun.mellow.util.ChatUtils;
import com.roxiun.mellow.util.formatting.FormattingUtils;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.util.BlockPos;

public class XadiaCommand extends CommandBase {

    private final XadiaApi xadiaApi;
    private final MellowOneConfig config;

    public XadiaCommand(XadiaApi xadiaApi, MellowOneConfig config) {
        this.xadiaApi = xadiaApi;
        this.config = config;
    }

    @Override
    public String getCommandName() {
        return "xadia";
    }

    @Override
    public String getCommandUsage(ICommandSender sender) {
        return "/xadia <username>";
    }

    @Override
    public List<String> getCommandAliases() {
        return Arrays.asList("mxadia");
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args) {
        if (args.length != 1) {
            ChatUtils.sendCommandMessage(
                sender,
                "§cInvalid usage! Use /xadia <username>"
            );
            return;
        }

        String username = args[0];
        AsyncExecutor.getInstance().command(() -> {
            try {
                List<XadiaTag> tags = xadiaApi.fetchXadiaTags(
                    null,
                    username,
                    config.xadiaKey,
                    config.xadiaVerifiedOnly
                );

                if (tags == null || tags.isEmpty()) {
                    MainThreadDispatcher.run(() ->
                        ChatUtils.sendCommandMessage(
                            sender,
                            "§aNo Xadia tags found for: §r" + username
                        )
                    );
                } else {
                    String formattedTags = FormattingUtils.formatXadiaTags(
                        tags
                    );
                    String xadiaMessage =
                        "§c" + username + " is tagged for: " + formattedTags;
                    MainThreadDispatcher.run(() ->
                        ChatUtils.sendCommandMessage(sender, xadiaMessage)
                    );
                }
            } catch (IOException e) {
                MainThreadDispatcher.run(() ->
                    ChatUtils.sendCommandMessage(
                        sender,
                        "§cCould not fetch Xadia tags for " +
                            username +
                            ": " +
                            e.getMessage()
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
