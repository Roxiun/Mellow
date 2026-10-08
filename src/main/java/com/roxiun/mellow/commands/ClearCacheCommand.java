package com.roxiun.mellow.commands;

import com.roxiun.mellow.cache.PlayerCache;
import com.roxiun.mellow.data.TabStats;
import com.roxiun.mellow.util.ChatUtils;
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
        if (com.roxiun.mellow.Mellow.partyBlacklistWarningService != null)
            com.roxiun.mellow.Mellow.partyBlacklistWarningService.reset();
        if (com.roxiun.mellow.Mellow.inGameTabStatsSyncService != null)
            com.roxiun.mellow.Mellow.inGameTabStatsSyncService.clear();
        ChatUtils.sendCommandMessage(sender, "§aAll caches have been cleared.");
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 0;
    }
}
