package com.strawberry.statsify.commands;

import com.mojang.authlib.GameProfile;
import com.strawberry.statsify.api.StatsProvider;
import com.strawberry.statsify.api.UrchinApi;
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

    private final StatsProvider statsProvider;
    private final UrchinApi urchinApi;
    private final StatsifyOneConfig config;

    public BedwarsCommand(
        StatsifyOneConfig config,
        StatsProvider statsProvider,
        UrchinApi urchinApi
    ) {
        this.config = config;
        this.statsProvider = statsProvider;
        this.urchinApi = urchinApi;
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
                    "§r[§bF§r]§c Invalid usage!§r Use /bw §5<username>§r"
                )
            );
            return;
        }

        String username = args[0];
        new Thread(() -> {
            try {
                String stats = statsProvider.fetchPlayerStats(username);
                String finalStats = stats;
                Minecraft.getMinecraft().addScheduledTask(() ->
                    Minecraft.getMinecraft().thePlayer.addChatMessage(
                        new ChatComponentText("§r[§bF§r] " + finalStats)
                    )
                );
                if (config.urchin) {
                    fetchTags(username);
                }
            } catch (IOException e) {
                Minecraft.getMinecraft().addScheduledTask(() ->
                    Minecraft.getMinecraft().thePlayer.addChatMessage(
                        new ChatComponentText(
                            "§r[§bF§r] §cFailed to fetch stats for: §r" +
                                username
                        )
                    )
                );
            } catch (Exception e) {
                Minecraft.getMinecraft().addScheduledTask(() ->
                    Minecraft.getMinecraft().thePlayer.addChatMessage(
                        new ChatComponentText(
                            "§r[§bF§r] §cAn unexpected error occurred while fetching stats for: §r" +
                                username
                        )
                    )
                );
                e.printStackTrace();
            }
        })
            .start();
    }

    private void fetchTags(String username) {
        try {
            String tags = urchinApi
                .fetchUrchinTags(username, config.urchinKey)
                .replace("sniper", "§4§lSniper")
                .replace("blatant_cheater", "§4§lBlatant Cheater")
                .replace("closet_cheater", "§e§lCloset Cheater")
                .replace("confirmed_cheater", "§4§lConfirmed Cheater");

            if (!tags.isEmpty()) {
                Minecraft.getMinecraft().addScheduledTask(() ->
                    Minecraft.getMinecraft().thePlayer.addChatMessage(
                        new ChatComponentText(
                            "§r[§bF§r] §c⚠ §r§cTagged§r for: " + tags
                        )
                    )
                );
            }
        } catch (IOException e) {
            Minecraft.getMinecraft().addScheduledTask(() ->
                Minecraft.getMinecraft().thePlayer.addChatMessage(
                    new ChatComponentText(
                        "§r[§bF§r] Failed to fetch tags for " +
                            username +
                            " | " +
                            e.getMessage()
                    )
                )
            );
        }
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
