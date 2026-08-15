package com.roxiun.mellow.commands;

import com.roxiun.mellow.feature.replay.ReplayManager;
import com.roxiun.mellow.util.ChatUtils;
import java.util.Arrays;
import java.util.List;
import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;

public class ClipCommand extends CommandBase {

    private final ReplayManager replayManager;

    public ClipCommand(ReplayManager replayManager) {
        this.replayManager = replayManager;
    }

    @Override
    public String getCommandName() {
        return "clip";
    }

    @Override
    public List<String> getCommandAliases() {
        return Arrays.asList("mclip", "mellowclip");
    }

    @Override
    public String getCommandUsage(ICommandSender sender) {
        return "/clip";
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 0;
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args) {
        if (args != null && args.length > 0) {
            ChatUtils.sendCommandMessage(sender, "§cUsage: /clip");
            return;
        }

        ReplayManager.ClipRequestResult result = replayManager.requestClip();
        switch (result) {
            case ACCEPTED:
                ChatUtils.sendCommandMessage(sender, "§7Saving recent gameplay as a clip...");
                return;
            case DISABLED:
                ChatUtils.sendCommandMessage(sender, "§cReplay clipping is disabled in Mellow settings.");
                return;
            case NO_MULTIPLAYER_SESSION:
                ChatUtils.sendCommandMessage(sender, "§cJoin a multiplayer world before creating a clip.");
                return;
            case PLAYBACK_ACTIVE:
                ChatUtils.sendCommandMessage(sender, "§cClips cannot be created during replay playback.");
                return;
            case BUFFER_CAPPED:
                ChatUtils.sendCommandMessage(sender, "§cThe clip buffer reached its size limit; join a new world to restart it.");
                return;
            default:
                ChatUtils.sendCommandMessage(sender, "§cThe clip buffer is not ready in this world.");
        }
    }
}
