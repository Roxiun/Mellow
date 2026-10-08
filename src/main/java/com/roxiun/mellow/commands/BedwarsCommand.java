package com.roxiun.mellow.commands;

import com.mojang.authlib.GameProfile;
import com.roxiun.mellow.api.bedwars.BedwarsPlayer;
import com.roxiun.mellow.api.provider.model.StatScope;
import com.roxiun.mellow.cache.PlayerCache;
import com.roxiun.mellow.cache.ProfileFetchContext;
import com.roxiun.mellow.cache.ProfileFetchResult;
import com.roxiun.mellow.config.MellowOneConfig;
import com.roxiun.mellow.core.async.AsyncExecutor;
import com.roxiun.mellow.core.async.MainThreadDispatcher;
import com.roxiun.mellow.data.PlayerProfile;
import com.roxiun.mellow.feature.stats.StatsFetchFailureFormatter;
import com.roxiun.mellow.util.ChatUtils;
import com.roxiun.mellow.util.UUIDUtils;
import com.roxiun.mellow.util.blacklist.BlacklistManager;
import com.roxiun.mellow.util.blacklist.BlacklistedPlayer;
import java.util.Arrays;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.util.BlockPos;

public class BedwarsCommand extends CommandBase {

    private final PlayerCache playerCache;
    private final MellowOneConfig config;
    private final BlacklistManager blacklistManager;

    public BedwarsCommand(
        PlayerCache playerCache,
        MellowOneConfig config,
        BlacklistManager blacklistManager
    ) {
        this.playerCache = playerCache;
        this.config = config;
        this.blacklistManager = blacklistManager;
    }

    @Override
    public String getCommandName() {
        return "bw";
    }

    @Override
    public String getCommandUsage(ICommandSender sender) {
        return "/bw <username>";
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args) {
        if (args.length != 1) {
            ChatUtils.sendCommandMessage(
                sender,
                "§cInvalid usage! Use /bw <username>"
            );
            return;
        }

        String username = args[0];

        ChatUtils.sendCommandMessage(
            sender,
            "§r§7Fetching stats for " + username + "..."
        );
        AsyncExecutor.getInstance().command(() -> {
            ProfileFetchResult result = playerCache.getScopedProfileResult(
                username,
                StatScope.BEDWARS,
                ProfileFetchContext.GENERAL,
                true
            );
            PlayerProfile profile = result.getProfile();

            if (profile == null || profile.getBedwarsPlayer() == null) {
                MainThreadDispatcher.run(() ->
                    ChatUtils.sendCommandMessage(
                        sender,
                        "§cFailed to fetch stats for: §r" +
                            username +
                            "§c (" +
                            StatsFetchFailureFormatter.describe(result) +
                            ")"
                    )
                );
                sendTagsAndLocal(sender, profile);
                return;
            }

            BedwarsPlayer player = profile.getBedwarsPlayer();
            List<String> statsLines = Arrays.asList(
                player.getStars() + " §r" + player.getFormattedNameWithRank(),
                "§rFKDR: " + player.getFkdrColor() + player.getFormattedFkdr(),
                "§rWLR: " + player.getFormattedWLRWithColor(),
                "§rBBLR: " + player.getFormattedBBLRWithColor(),
                "§rWins: " + player.getFormattedWinsWithColor(),
                "§rBeds: " + player.getFormattedBedsWithColor(),
                "§rFinals: " + player.getFormattedFinalsWithColor()
            );

            MainThreadDispatcher.run(() ->
                ChatUtils.sendMultilineCommandMessage(sender, statsLines)
            );

            sendTagsAndLocal(sender, profile);

        });
    }

    private void sendTagsAndLocal(ICommandSender sender, PlayerProfile profile) {
        if (profile == null) return;
        BlacklistedPlayer local = blacklistManager.getBlacklistedPlayer(UUIDUtils.fromString(profile.getUuid()));
        MainThreadDispatcher.run(() -> {
            if (local != null) ChatUtils.sendMultilineCommandMessage(sender, formatLocalBlacklistMessage(local));
            for (String line : profile.getTags().messages()) ChatUtils.sendCommandMessage(sender, line);
        });
    }

    static String formatLocalBlacklistMessage(
        BlacklistedPlayer blacklistedPlayer
    ) {
        String message = "§6§lLocal§r§6: This player is on your blacklist";
        String reason = blacklistedPlayer.getReason();
        if (reason == null) {
            return message;
        }

        String trimmedReason = reason.trim();
        if (
            trimmedReason.isEmpty() ||
            "(none)".equalsIgnoreCase(trimmedReason) ||
            BlacklistManager.isExternalFileImportReason(trimmedReason)
        ) {
            return message;
        }
        return message + ": " + trimmedReason;
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
