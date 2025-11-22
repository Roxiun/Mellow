package com.strawberry.statsify.commands;

import com.mojang.authlib.GameProfile;
import com.strawberry.statsify.api.NadeshikoApi;
import com.strawberry.statsify.api.WinstreakApi;
import com.strawberry.statsify.config.StatsifyOneConfig;
import java.io.IOException;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.util.BlockPos;
import net.minecraft.util.ChatComponentText;

public class BedwarsCommand extends CommandBase {

    private final NadeshikoApi nadeshikoApi;
    private final WinstreakApi winstreakApi;
    private final StatsifyOneConfig config;

    public BedwarsCommand(
        StatsifyOneConfig config,
        NadeshikoApi nadeshikoApi,
        WinstreakApi winstreakApi
    ) {
        this.config = config;
        this.nadeshikoApi = nadeshikoApi;
        this.winstreakApi = winstreakApi;
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
                    "§r[§bF§r]§cInvalid usage!§r Use /bw §5<username>§r"
                )
            );
            return;
        }

        String username = args[0];
        new Thread(() -> {
            try {
                String stats;
                if (config.statsSource == 1) {
                    // Winstreak.ws
                    stats = winstreakApi.fetchPlayerStats(username);
                } else {
                    // Nadeshiko
                    stats = nadeshikoApi.fetchPlayerStats(username);
                }
                String finalStats = stats;
                Minecraft.getMinecraft().addScheduledTask(() ->
                    Minecraft.getMinecraft().thePlayer.addChatMessage(
                        new ChatComponentText("§r[§bF§r] " + finalStats)
                    )
                );
            } catch (IOException e) {
                Minecraft.getMinecraft().addScheduledTask(() ->
                    Minecraft.getMinecraft().thePlayer.addChatMessage(
                        new ChatComponentText(
                            "§r[§bF§r] §cFailed to fetch stats for: §r" +
                                username
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
