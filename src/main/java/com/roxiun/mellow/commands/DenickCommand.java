package com.roxiun.mellow.commands;

import com.roxiun.mellow.api.aurora.AuroraApi;
import com.roxiun.mellow.config.MellowOneConfig;
import com.roxiun.mellow.feature.nicks.DenickSearchOptions;
import com.roxiun.mellow.core.async.AsyncExecutor;
import com.roxiun.mellow.core.async.MainThreadDispatcher;
import com.roxiun.mellow.util.ChatUtils;
import java.io.IOException;
import java.util.List;
import com.roxiun.mellow.feature.nicks.DenickQuery;
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
        return DenickQuery.USAGE;
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args) {
        final DenickQuery query;
        try {
            query = DenickQuery.parse(args);
        } catch (IllegalArgumentException e) {
            ChatUtils.sendCommandMessage(sender, "§c" + e.getMessage());
            return;
        }

        int finalsRange = DenickSearchOptions.range(config.finalsRange);
        int bedsRange = DenickSearchOptions.range(config.bedsRange);
        int max = DenickSearchOptions.limit(config.maxResults);
        String key = config.auroraApiKey;
        ChatUtils.sendCommandMessage(sender, "§aSearching for players...");
        AsyncExecutor.getInstance().command(() -> {
            try {
                List<DenickQuery.Match> matches = query.search(auroraApi, finalsRange, bedsRange, max, key);
                MainThreadDispatcher.run(() -> {
                    if (matches.isEmpty()) {
                        ChatUtils.sendCommandMessage(sender, query.isCombined()
                            ? "§cNo shared matches in the returned results." : "§cNo players found.");
                        return;
                    }
                    ChatUtils.sendCommandMessage(sender, "§a" + matches.size()
                        + (query.isCombined() ? " players matched both stats:" : " players found:"));
                    for (DenickQuery.Match match : matches) {
                        String distances = match.bedsDistance == null ? "" : "beds: " + distance(match.bedsDistance);
                        if (match.finalsDistance != null) {
                            if (!distances.isEmpty()) distances += ", ";
                            distances += "finals: " + distance(match.finalsDistance);
                        }
                        ChatUtils.sendCommandMessage(sender, "§a" + match.name + " §7— " + distances);
                    }
                });
            } catch (IOException | RuntimeException e) {
                MainThreadDispatcher.run(() -> ChatUtils.sendCommandMessage(sender,
                    "§cSearch failed while fetching data from Aurora API. Please try again."));
                e.printStackTrace();
            }
        });
    }

    private static String distance(int distance) {
        return distance == 0 ? "exact" : distance + " away";
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
        if (args.length == 3) {
            if (args[0].equalsIgnoreCase("finals")) return getListOfStringsMatchingLastWord(args, "beds");
            if (args[0].equalsIgnoreCase("beds")) return getListOfStringsMatchingLastWord(args, "finals");
        }
        return null;
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 0;
    }
}
