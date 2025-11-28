package com.strawberry.statsify.commands;

import com.strawberry.statsify.cache.PlayerCache;
import com.strawberry.statsify.data.TabStats;
import com.strawberry.statsify.util.ChatUtils;
import java.util.Map;
import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;

public class ClearCacheCommand extends CommandBase {

    private final PlayerCache playerCache;
    private final Map<String, TabStats> tabStats;

    public ClearCacheCommand(
        PlayerCache playerCache,
        Map<String, TabStats> tabStats
    ) {
        this.playerCache = playerCache;
        this.tabStats = tabStats;
    }

    @Override
    public String getCommandName() {
        return "clearcache"; // Renaming for clarity, as it clears more than just tab
    }

    @Override
    public String getCommandUsage(ICommandSender sender) {
        return "/clearcache";
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args) {
        playerCache.clearCache();
        tabStats.clear();
        ChatUtils.sendCommandMessage(sender, "§aAll caches have been cleared.");
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 0;
    }
}
