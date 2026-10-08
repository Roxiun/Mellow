package com.roxiun.mellow.commands;

import com.roxiun.mellow.cache.*;
import com.roxiun.mellow.core.async.AsyncExecutor;
import com.roxiun.mellow.core.async.MainThreadDispatcher;
import com.roxiun.mellow.data.PlayerProfile;
import com.roxiun.mellow.data.TabStats;
import com.roxiun.mellow.api.hypixel.HypixelFeatures;
import com.roxiun.mellow.feature.stats.StatsFetchFailureFormatter;
import com.roxiun.mellow.stats.*;
import com.roxiun.mellow.util.ChatUtils;
import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.util.BlockPos;

/** Shared entrypoint for every registered game; existing /bw and /sw output stays unchanged. */
public final class StatsCommand extends CommandBase {
    private final PlayerCache cache;
    public StatsCommand(PlayerCache cache) { this.cache = cache; }
    @Override public String getCommandName() { return "mellowstats"; }
    @Override public String getCommandUsage(ICommandSender sender) {
        return "/mellowstats <player> [game|auto] [mode]";
    }
    @Override public int getRequiredPermissionLevel() { return 0; }
    @Override public void processCommand(ICommandSender sender, String[] args) {
        if (args.length == 0 || args.length > 3) {
            ChatUtils.sendCommandMessage(sender, "§c" + getCommandUsage(sender));
            return;
        }
        StatsSelection detected = GameRegistry.detect(HypixelFeatures.getInstance().getGameSnapshot());
        GameDefinition<?> game = args.length >= 2 && !"auto".equalsIgnoreCase(args[1])
            ? GameRegistry.find(args[1]) : detected == null ? null : detected.game();
        if (game == null) {
            ChatUtils.sendCommandMessage(sender, "§cChoose a supported game using /mellowstats <player> <game>.");
            return;
        }
        String mode = args.length == 3 ? args[2] : args.length < 2 || "auto".equalsIgnoreCase(args[1])
            ? detected.mode() : "overall";
        final StatsSelection selection;
        try { selection = new StatsSelection(game, mode); }
        catch (IllegalArgumentException e) {
            ChatUtils.sendCommandMessage(sender, "§cUnknown mode. Use tab completion to choose one.");
            return;
        }
        String username = args[0];
        AsyncExecutor.getInstance().command(() -> {
            ProfileFetchResult result = cache.getSelectedProfileResult(username, selection, ProfileFetchContext.GENERAL, true);
            MainThreadDispatcher.run(() -> {
                PlayerProfile profile = result.getProfile();
                if (profile == null || !profile.hasStats(selection.game().scope())) {
                    ChatUtils.sendCommandMessage(sender, "§cFailed to fetch stats for: §r" + username
                        + "§c (" + StatsFetchFailureFormatter.describe(result) + ")");
                } else {
                    TabStats stats = profile.getTabStats(selection.game().scope());
                    List<String> lines = new ArrayList<>();
                    lines.add(stats.getFormattedNameWithRank() + " §7| " + selection.game().displayName()
                        + " · " + selection.game().modeLabel(selection.mode()));
                    for (StatDefinition stat : selection.game().columns()) {
                        switch (stat.style()) {
                            case VALUE: case BADGE: case TITLE: case BEDWARS_STARS: case BEDWARS_WINSTREAK:
                                String value = stat.value(stats);
                                lines.add("§r" + stat.option() + ": " + (value == null || value.isEmpty() ? "§7—" : value));
                                break;
                            default: break;
                        }
                    }
                    ChatUtils.sendMultilineCommandMessage(sender, lines);
                }
                if (profile != null) ChatUtils.sendMultilineCommandMessage(sender, profile.getTags().messages(true));
            });
        });
    }
    @Override public List<String> addTabCompletionOptions(ICommandSender sender, String[] args, BlockPos pos) {
        if (args.length == 1) {
            List<String> choices = new ArrayList<>();
            if (Minecraft.getMinecraft().getNetHandler() != null)
                Minecraft.getMinecraft().getNetHandler().getPlayerInfoMap().forEach(p -> choices.add(p.getGameProfile().getName()));
            return getListOfStringsMatchingLastWord(args, choices.toArray(new String[0]));
        }
        if (args.length == 2) {
            List<String> choices = new ArrayList<>();
            choices.add("auto");
            GameRegistry.all().forEach(game -> choices.add(game.id()));
            return getListOfStringsMatchingLastWord(args, choices.toArray(new String[0]));
        }
        if (args.length == 3) {
            GameDefinition<?> game = GameRegistry.find(args[1]);
            if ("auto".equalsIgnoreCase(args[1])) {
                StatsSelection detected = GameRegistry.detect(HypixelFeatures.getInstance().getGameSnapshot());
                game = detected == null ? null : detected.game();
            }
            if (game != null) return getListOfStringsMatchingLastWord(args, game.modes().toArray(new String[0]));
        }
        return Collections.emptyList();
    }
}
