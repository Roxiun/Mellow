package com.roxiun.mellow.commands;

import com.roxiun.mellow.api.aurora.AuroraApi;
import com.roxiun.mellow.config.MellowOneConfig;
import com.roxiun.mellow.util.ChatUtils;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;
import net.minecraft.client.Minecraft;
import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.util.BlockPos;

public class DenickCommand extends CommandBase {

    private final MellowOneConfig config;
    private final AuroraApi auroraApi;

    public DenickCommand(MellowOneConfig config, AuroraApi auroraApi) {
        this.config = config;
        this.auroraApi = auroraApi;
    }

    @Override
    public String getCommandName() {
        return "denick";
    }

    @Override
    public String getCommandUsage(ICommandSender sender) {
        return "/denick <finals | beds> <number>";
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args) {
        if (args.length != 2) {
            ChatUtils.sendCommandMessage(
                sender,
                "§cInvalid usage. Use: " + getCommandUsage(sender)
            );
            return;
        }

        String type = args[0];
        if (
            !type.equalsIgnoreCase("finals") && !type.equalsIgnoreCase("beds")
        ) {
            ChatUtils.sendCommandMessage(
                sender,
                "§cInvalid type. Use 'finals' or 'beds'."
            );
            return;
        }

        String numberStr = args[1];
        try {
            Integer.parseInt(numberStr.replace(",", ""));
        } catch (NumberFormatException e) {
            ChatUtils.sendCommandMessage(
                sender,
                "§cInvalid number: " + numberStr
            );
            return;
        }

        ChatUtils.sendCommandMessage(sender, "§aSearching for players...");

        new Thread(() -> {
            try {
                int[] rangeValues = { 100, 200, 500, 1000 };
                int[] maxValues = { 5, 10, 20 };

                int rangeIndex = type.equals("finals")
                    ? config.finalsRange
                    : config.bedsRange;
                int maxIndex = config.maxResults;

                if (
                    rangeIndex < 0 || rangeIndex >= rangeValues.length
                ) rangeIndex = 1; // Default to 200
                if (maxIndex < 0 || maxIndex >= maxValues.length) maxIndex = 0; // Default to 5

                int range = rangeValues[rangeIndex];
                int max = maxValues[maxIndex];

                AuroraApi.AuroraResponse response = auroraApi.queryStats(
                    type,
                    numberStr,
                    range,
                    max,
                    config.auroraApiKey
                );

                Minecraft.getMinecraft().addScheduledTask(() -> {
                    if (response != null && response.success) {
                        if (response.data.isEmpty()) {
                            ChatUtils.sendCommandMessage(
                                sender,
                                "§cNo players found."
                            );
                        } else {
                            String players = response.data
                                .stream()
                                .map(
                                    p ->
                                        "§a" +
                                        p.name +
                                        " §7(distance: " +
                                        p.distance +
                                        ")"
                                )
                                .collect(Collectors.joining(", "));
                            ChatUtils.sendCommandMessage(
                                sender,
                                "§aFound players: " + players
                            );
                        }
                    } else {
                        ChatUtils.sendCommandMessage(
                            sender,
                            "§cError fetching data from Aurora API."
                        );
                    }
                });
            } catch (IOException e) {
                Minecraft.getMinecraft().addScheduledTask(() -> {
                    ChatUtils.sendCommandMessage(
                        sender,
                        "§cAn error occurred while fetching data."
                    );
                });
                e.printStackTrace();
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
            return getListOfStringsMatchingLastWord(args, "finals", "beds");
        }
        return null;
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 0;
    }
}
