package com.strawberry.statsify.commands;

import com.mojang.authlib.GameProfile;
import com.strawberry.statsify.api.bedwars.BedwarsPlayer;
import com.strawberry.statsify.api.urchin.UrchinTag;
import com.strawberry.statsify.cache.PlayerCache;
import com.strawberry.statsify.config.StatsifyOneConfig;
import com.strawberry.statsify.data.PlayerProfile;
import com.strawberry.statsify.util.formatting.FormattingUtils;
import java.util.List;
import java.util.stream.Collectors;
import net.minecraft.client.Minecraft;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.util.BlockPos;
import net.minecraft.util.ChatComponentText;

public class BedwarsCommand extends CommandBase {

    private final PlayerCache playerCache;
    private final StatsifyOneConfig config;

    public BedwarsCommand(PlayerCache playerCache, StatsifyOneConfig config) {
        this.playerCache = playerCache;
        this.config = config;
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
            sender.addChatMessage(
                new ChatComponentText(
                    "§r[§bStatsify§r]§c Invalid usage! Use /bw <username>"
                )
            );
            return;
        }

        String username = args[0];
        new Thread(() -> {
            PlayerProfile profile = playerCache.getProfile(username);

            if (profile == null || profile.getBedwarsPlayer() == null) {
                Minecraft.getMinecraft().addScheduledTask(() ->
                    sender.addChatMessage(
                        new ChatComponentText(
                            "§r[§bStatsify§r] §cFailed to fetch stats for: §r" +
                                username
                        )
                    )
                );
                return;
            }

            BedwarsPlayer player = profile.getBedwarsPlayer();
            String statsMessage =
                "§r[§bStatsify§r] " +
                player.getName() +
                " §r" +
                player.getStars() +
                " §7|§r FKDR: " +
                player.getFkdrColor() +
                player.getFormattedFkdr();

            Minecraft.getMinecraft().addScheduledTask(() ->
                sender.addChatMessage(new ChatComponentText(statsMessage))
            );

            if (config.urchin && profile.isUrchinTagged()) {
                String tags = FormattingUtils.formatUrchinTags(
                    profile.getUrchinTags()
                );
                String urchinMessage =
                    "§r[§bStatsify§r] §c" +
                    username +
                    " is tagged for: " +
                    tags;
                Minecraft.getMinecraft().addScheduledTask(() ->
                    sender.addChatMessage(new ChatComponentText(urchinMessage))
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
