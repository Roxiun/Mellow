package com.roxiun.mellow.commands;

import com.roxiun.mellow.stats.GameRegistry;
import com.mojang.authlib.GameProfile;
import com.roxiun.mellow.api.hypixel.provider.StatsProvider;
import com.roxiun.mellow.api.hypixel.provider.model.ProviderId;
import com.roxiun.mellow.cache.PlayerCache;
import com.roxiun.mellow.cache.ProfileFetchResult;
import com.roxiun.mellow.config.MellowOneConfig;
import com.roxiun.mellow.core.async.AsyncExecutor;
import com.roxiun.mellow.core.async.MainThreadDispatcher;
import com.roxiun.mellow.data.PlayerProfile;
import com.roxiun.mellow.feature.profileviewer.PVGui;
import com.roxiun.mellow.feature.stats.StatsFetchFailureFormatter;
import com.roxiun.mellow.util.ChatUtils;
import java.util.Arrays;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.util.BlockPos;

public class PVCommand extends CommandBase {

    private final PlayerCache playerCache;
    private final MellowOneConfig config;

    public PVCommand(PlayerCache playerCache, MellowOneConfig config) {
        this.playerCache = playerCache;
        this.config = config;
    }

    @Override
    public String getCommandName() {
        return "pv";
    }

    @Override
    public List<String> getCommandAliases() {
        return Arrays.asList(
            "profileviewer",
            "bedwarsprofileviewer",
            "bwprofileviewer"
        );
    }

    @Override
    public String getCommandUsage(ICommandSender sender) {
        return "/pv [username]";
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args) {
        if (args.length > 1) {
            ChatUtils.sendCommandMessage(sender, "§cInvalid usage! Use /pv [username]");
            return;
        }

        String username = args.length == 0
            ? Minecraft.getMinecraft().thePlayer.getName()
            : args[0];

        ChatUtils.sendCommandMessage(
            sender,
            "§r§7Fetching profile for " + username + "..."
        );

        AsyncExecutor.getInstance().command(() -> {
            ProfileFetchResult result = playerCache.getProfileResult(username);
            PlayerProfile profile = result.getProfile();
            String rawProviderData = playerCache.fetchRawPlayerData(username);
            StatsProvider selectedProvider = playerCache.getSelectedProvider();
            ProviderId providerId = selectedProvider == null
                ? ProviderId.HYPIXEL_PUBLIC
                : selectedProvider.getProviderId();

            if (profile == null || profile.getStats(GameRegistry.BEDWARS) == null) {
                MainThreadDispatcher.run(() ->
                    ChatUtils.sendCommandMessage(
                        sender,
                        "§cFailed to fetch profile for: §r" +
                        username +
                        "§c (" +
                        StatsFetchFailureFormatter.describe(result) +
                        ")"
                    )
                );
                return;
            }

            MainThreadDispatcher.run(() ->
                Minecraft
                    .getMinecraft()
                    .displayGuiScreen(
                        new PVGui(
                            profile,
                            rawProviderData,
                            providerId,
                            playerCache,
                            config
                        )
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
