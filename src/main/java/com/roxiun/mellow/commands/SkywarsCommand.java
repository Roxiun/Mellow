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
import com.roxiun.mellow.util.formatting.FormattingUtils;
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

            if (config.isCoralEnabled() && profile.isCoralTagged()) {
                String tags = FormattingUtils.formatCoralTags(
                    profile.getCoralTags()
                );
                String coralMessage = "§5§lCoral§r§5: " + tags;
                MainThreadDispatcher.run(() ->
                    ChatUtils.sendMultilineCommandMessage(sender, coralMessage)
                );
            }

            if (config.xadia && profile.isXadiaTagged()) {
                String tags = FormattingUtils.formatXadiaTags(
                    profile.getXadiaTags()
                );
                String xadiaMessage = "§5§lXadia§r§5: " + tags;
                MainThreadDispatcher.run(() ->
                    ChatUtils.sendMultilineCommandMessage(sender, xadiaMessage)
                );
            }

            if (config.seraph && profile.isSeraphTagged()) {
                String formattedTags = FormattingUtils.formatSeraphTags(
                    profile.getSeraphTags()
                );
                String[] tagMessages = formattedTags.split("\n§c");
                if (
                    tagMessages.length > 0 && !tagMessages[0].trim().isEmpty()
                ) {
                    String firstMessage = "§3§lSeraph§r§3: " + tagMessages[0];
                    MainThreadDispatcher.run(() ->
                        ChatUtils.sendMultilineCommandMessage(
                            sender,
                            firstMessage
                        )
                    );
                    for (int i = 1; i < tagMessages.length; i++) {
                        if (!tagMessages[i].trim().isEmpty()) {
                            String additionalMessage = "§c" + tagMessages[i];
                            MainThreadDispatcher.run(() ->
                                ChatUtils.sendMultilineCommandMessage(
                                    sender,
                                    additionalMessage
                                )
                            );
                        }
                    }
                }
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
