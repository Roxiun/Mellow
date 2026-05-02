package com.roxiun.mellow.commands;

import com.roxiun.mellow.api.aurora.AuroraApi;
import com.roxiun.mellow.api.frosty.FrostyApi;
import com.roxiun.mellow.api.frosty.FrostyReponse;
import com.roxiun.mellow.config.MellowOneConfig;
import com.roxiun.mellow.core.async.AsyncExecutor;
import com.roxiun.mellow.core.async.MainThreadDispatcher;
import com.roxiun.mellow.util.ChatUtils;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.util.BlockPos;

public class DenickCommand extends CommandBase {

    private final MellowOneConfig config;
    private final AuroraApi auroraApi;
    private final FrostyApi frostyApi;

    public DenickCommand(
        MellowOneConfig config,
        AuroraApi auroraApi,
        FrostyApi frostyApi
    ) {
        this.config = config;
        this.auroraApi = auroraApi;
        this.frostyApi = frostyApi;
    }

    @Override
    public String getCommandName() {
        return "denick";
    }

    @Override
    public String getCommandUsage(ICommandSender sender) {
        return config != null && config.numberDenickerProvider == 1
            ? "/denick <finals_count> <beds_count>"
            : "/denick <finals | beds> <number>";
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args) throws CommandException {
        if (config != null && config.numberDenickerProvider == 1) {
            processFrostyCommand(sender, args);
            return;
        }

        processAuroraCommand(sender, args);
    }

    @Override
    public List<String> addTabCompletionOptions(
        ICommandSender sender,
        String[] args,
        BlockPos pos
    ) {
        if (config != null && config.numberDenickerProvider == 1) {
            return null;
        }

        if (args.length == 1) {
            return getListOfStringsMatchingLastWord(args, "finals", "beds");
        }
        return null;
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 0;
    }

    private void processAuroraCommand(ICommandSender sender, String[] args) {
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

        AsyncExecutor.getInstance().command(() -> {
            try {
                int range = config.getAuroraDenickRange(type);
                int max = config.getAuroraDenickMaxResults();

                AuroraApi.AuroraResponse response = auroraApi.queryStats(
                    type,
                    numberStr,
                    range,
                    max,
                    config.auroraApiKey
                );

                MainThreadDispatcher.run(() -> {
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
                MainThreadDispatcher.run(() -> {
                    ChatUtils.sendCommandMessage(
                        sender,
                        "§cAn error occurred while fetching data."
                    );
                });
                e.printStackTrace();
            }
        });
    }

    private void processFrostyCommand(ICommandSender sender, String[] args) {
        if (args.length != 2) {
            ChatUtils.sendCommandMessage(
                sender,
                "§cInvalid usage. Use: " + getCommandUsage(sender)
            );
            return;
        }

        if (config.frostyApiKey == null || config.frostyApiKey.trim().isEmpty()) {
            ChatUtils.sendCommandMessage(
                sender,
                "§cMissing Frosty API key. Set it in OneConfig: API Keys > Frosty."
            );
            return;
        }

        int finalsCount;
        int bedsCount;
        try {
            finalsCount = Integer.parseInt(args[0].replace(",", ""));
            bedsCount = Integer.parseInt(args[1].replace(",", ""));
        } catch (NumberFormatException e) {
            ChatUtils.sendCommandMessage(
                sender,
                "§cBoth finals_count and beds_count must be valid numbers."
            );
            return;
        }

        if (finalsCount < 0 || bedsCount < 0) {
            ChatUtils.sendCommandMessage(
                sender,
                "§cfinals_count and beds_count must be 0 or higher."
            );
            return;
        }

        if (finalsCount == 0 && bedsCount == 0) {
            ChatUtils.sendCommandMessage(
                sender,
                "§cAt least one value must be above 0."
            );
            return;
        }

        ChatUtils.sendCommandMessage(sender, "§aSearching for players...");

        AsyncExecutor.getInstance().command(() -> {
            try {
                FrostyReponse response = frostyApi.queryStats(
                    finalsCount,
                    bedsCount,
                    config.frostyApiKey
                );

                MainThreadDispatcher.run(() -> {
                    if (response == null || !response.success) {
                        ChatUtils.sendCommandMessage(
                            sender,
                            "§cError fetching data from Frosty API."
                        );
                        return;
                    }

                    if (response.data == null || response.data.isEmpty()) {
                        ChatUtils.sendCommandMessage(sender, "§cNo players found.");
                        return;
                    }

                    String players = response.data
                        .stream()
                        .map(
                            p -> {
                                String finalsText = formatFinalsOutput(
                                    p.totalFinalKills,
                                    finalsCount
                                );
                                String bedsText = formatBedsOutput(
                                    p.totalBedsBroken,
                                    bedsCount
                                );
                                return (
                                    "§a" +
                                    p.username +
                                    " §7(" +
                                    finalsText +
                                    ", " +
                                    bedsText +
                                    ")"
                                );
                            }
                        )
                        .collect(Collectors.joining(", "));

                    ChatUtils.sendCommandMessage(
                        sender,
                        "§aFound players: " + players
                    );
                });
            } catch (IOException e) {
                MainThreadDispatcher.run(() -> {
                    ChatUtils.sendCommandMessage(
                        sender,
                        "§cAn error occurred while fetching data."
                    );
                });
                e.printStackTrace();
            }
        });
    }

    private String formatSignedDelta(long delta) {
        return delta >= 0 ? "+" + delta : String.valueOf(delta);
    }

    private String formatFinalsOutput(long totalFinalKills, int finalsCount) {
        if (finalsCount == 0) {
            return "FK: " + totalFinalKills;
        }
        return (
            "FK: " +
            totalFinalKills +
            " [" +
            formatSignedDelta(totalFinalKills - finalsCount) +
            "]"
        );
    }

    private String formatBedsOutput(long totalBedsBroken, int bedsCount) {
        if (bedsCount == 0) {
            return "Beds: " + totalBedsBroken;
        }
        return (
            "Beds: " +
            totalBedsBroken +
            " [" +
            formatSignedDelta(totalBedsBroken - bedsCount) +
            "]"
        );
    }
}
