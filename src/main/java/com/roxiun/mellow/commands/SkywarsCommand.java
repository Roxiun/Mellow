package com.roxiun.mellow.commands;

import com.mojang.authlib.GameProfile;
import com.roxiun.mellow.api.provider.model.StatScope;
import com.roxiun.mellow.api.skywars.SkywarsPlayer;
import com.roxiun.mellow.cache.PlayerCache;
import com.roxiun.mellow.cache.ProfileFetchContext;
import com.roxiun.mellow.cache.ProfileFetchResult;
import com.roxiun.mellow.config.MellowOneConfig;
import com.roxiun.mellow.core.async.AsyncExecutor;
import com.roxiun.mellow.core.async.MainThreadDispatcher;
import com.roxiun.mellow.data.PlayerProfile;
import com.roxiun.mellow.feature.stats.StatsFetchFailureFormatter;
import com.roxiun.mellow.util.ChatUtils;
import java.util.Arrays;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.util.BlockPos;

public class SkywarsCommand extends CommandBase {

    private final PlayerCache playerCache;
    private final MellowOneConfig config;

    public SkywarsCommand(PlayerCache playerCache, MellowOneConfig config) {
        this.playerCache = playerCache;
        this.config = config;
    }

    @Override
    public String getCommandName() {
        return "sw";
    }

    @Override
    public String getCommandUsage(ICommandSender sender) {
        return "/sw <username>";
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args) {
        if (args.length != 1) {
            ChatUtils.sendCommandMessage(
                sender,
                "§cInvalid usage! Use /sw <username>"
            );
            return;
        }

        String username = args[0];

        ChatUtils.sendCommandMessage(
            sender,
            "§r§7Fetching SkyWars stats for " + username + "..."
        );
        AsyncExecutor.getInstance().command(() -> {
            ProfileFetchResult result = playerCache.getScopedProfileResult(
                username,
                StatScope.SKYWARS,
                ProfileFetchContext.GENERAL,
                true
            );
            PlayerProfile profile = result.getProfile();

            if (profile == null || profile.getSkywarsPlayer() == null) {
                MainThreadDispatcher.run(() ->
                    ChatUtils.sendCommandMessage(
                        sender,
                        "§cFailed to fetch SkyWars stats for: §r" +
                        username +
                        "§c (" +
                        StatsFetchFailureFormatter.describe(result) +
                        ")"
                    )
                );
                if (profile != null) MainThreadDispatcher.run(() -> {
                    ChatUtils.sendMultilineCommandMessage(sender, profile.getTags().messages(true));
                });
                return;
            }

            SkywarsPlayer player = profile.getSkywarsPlayer();
            List<String> statsLines = Arrays.asList(
                player.getLevelFormatted() +
                " §r" +
                player.getFormattedNameWithRank(),
                "§rKDR: " + player.getFormattedKdrWithColor(),
                "§rWLR: " + player.getFormattedWlrWithColor(),
                "§rWins: " + player.getFormattedWinsWithColor(),
                "§rKills: " + player.getFormattedKillsWithColor()
            );

            MainThreadDispatcher.run(() ->
                ChatUtils.sendMultilineCommandMessage(sender, statsLines)
            );

            if (profile != null) MainThreadDispatcher.run(() -> {
                ChatUtils.sendMultilineCommandMessage(sender, profile.getTags().messages(true));
            });


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
