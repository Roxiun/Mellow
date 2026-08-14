package com.roxiun.mellow.commands;

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
import net.minecraft.command.ICommandSender;
import net.minecraft.util.BlockPos;

public class FrostyDenickCommand extends CommandBase {

    private final MellowOneConfig config;
    private final FrostyApi frostyApi;

    public FrostyDenickCommand(MellowOneConfig config, FrostyApi frostyApi) {
        this.config = config;
        this.frostyApi = frostyApi;
    }

    @Override
    public String getCommandName() {
        return "frostydenick";
    }

    @Override
    public String getCommandUsage(ICommandSender sender) {
        return "/frostydenick <finals_count> <beds_count>";
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

    @Override
    public List<String> addTabCompletionOptions(
        ICommandSender sender,
        String[] args,
        BlockPos pos
    ) {
        return null;
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 0;
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

