package com.roxiun.mellow.commands;

import com.mojang.authlib.GameProfile;
import com.roxiun.mellow.Mellow;
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
import com.roxiun.mellow.util.formatting.FormattingUtils;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.util.BlockPos;

public class WinstreakCommand extends CommandBase {

    private final PlayerCache playerCache;
    private final MellowOneConfig config;

    public WinstreakCommand(PlayerCache playerCache, MellowOneConfig config) {
        this.playerCache = playerCache;
        this.config = config;
    }

    @Override
    public String getCommandName() {
        return "winstreak";
    }

    @Override
    public String getCommandUsage(ICommandSender sender) {
        return "/winstreak <username>";
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args) {
        if (args.length != 1) {
            ChatUtils.sendCommandMessage(
                sender,
                "§cInvalid usage! Use /winstreak <username>"
            );
            return;
        }

        String username = args[0];
        ChatUtils.sendCommandMessage(
            sender,
            "§7Fetching winstreak for §f" + username + "§7..."
        );

        AsyncExecutor.getInstance().command(() -> {
            ProfileFetchResult result = playerCache.getScopedProfileResult(
                username,
                StatScope.BEDWARS,
                ProfileFetchContext.GENERAL,
                false
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
                return;
            }

            BedwarsPlayer player = profile.getBedwarsPlayer();
            String wsDisplay = resolveWinstreak(profile, player, sender);

            MainThreadDispatcher.run(() ->
                ChatUtils.sendMultilineCommandMessage(
                    sender,
                    Arrays.asList(
                        player.getStars() + " §r" + player.getFormattedNameWithRank(),
                        "§rWinstreak: " + wsDisplay
                    )
                )
            );
        });
    }

    private String resolveWinstreak(
        PlayerProfile profile,
        BedwarsPlayer player,
        ICommandSender sender
    ) {
        String visibleWinstreak = player.getFormattedWinstreakWithColor();
        if (!FormattingUtils.isHiddenOrEmptyWinstreakDisplay(visibleWinstreak)) {
            return visibleWinstreak;
        }

        if (Mellow.auroraWinstreakService == null) {
            return visibleWinstreak == null || visibleWinstreak.isEmpty()
                ? "§7N/A"
                : visibleWinstreak;
        }

        UUID uuid;
        try {
            uuid = UUIDUtils.fromString(profile.getUuid());
        } catch (Exception ignored) {
            uuid = null;
        }
        if (uuid == null) {
            return visibleWinstreak == null || visibleWinstreak.isEmpty()
                ? "§7N/A"
                : visibleWinstreak;
        }

        String compactUuid = uuid.toString().replace("-", "");
        long requestGeneration = Mellow.auroraWinstreakService.getGeneration();
        int auroraWs = Mellow.auroraWinstreakService.getCachedWinstreak(compactUuid);
        boolean hasCachedAuroraWs = Mellow.auroraWinstreakService.hasCachedWinstreak(
            compactUuid
        );
        if (
            !hasCachedAuroraWs &&
            Mellow.auroraWinstreakService.tryStartFetch(compactUuid)
        ) {
            try {
                auroraWs = Mellow.auroraWinstreakService.fetchWinstreakBlocking(
                    compactUuid
                );
                if (Mellow.auroraWinstreakService.getGeneration() == requestGeneration) Mellow.auroraWinstreakService.storeInCache(compactUuid, auroraWs);
            } catch (Exception e) {
                final String detail = e.getMessage() == null ? "unknown" : e.getMessage();
                MainThreadDispatcher.run(() ->
                    ChatUtils.sendCommandMessage(
                        sender,
                        "§cAurora fetch error: §6" + detail
                    )
                );
            } finally {
                if (Mellow.auroraWinstreakService.getGeneration() == requestGeneration) Mellow.auroraWinstreakService.finishFetch(compactUuid);
            }
        }

        if (auroraWs >= 0) {
            return FormattingUtils.formatBedwarsWinstreakWithColor(auroraWs) +
            " §7(Aurora)";
        }

        return visibleWinstreak == null || visibleWinstreak.isEmpty()
            ? "§7N/A"
            : visibleWinstreak;
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
