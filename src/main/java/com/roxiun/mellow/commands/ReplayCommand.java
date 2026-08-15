package com.roxiun.mellow.commands;

import com.roxiun.mellow.feature.replay.ReplayCatalogEntry;
import com.roxiun.mellow.feature.replay.ReplayManager;
import com.roxiun.mellow.feature.replay.ReplayMetadata;
import com.roxiun.mellow.util.ChatUtils;
import java.util.Arrays;
import java.util.List;
import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.util.BlockPos;

public class ReplayCommand extends CommandBase {

    private final ReplayManager replayManager;

    public ReplayCommand(ReplayManager replayManager) {
        this.replayManager = replayManager;
    }

    @Override
    public String getCommandName() {
        return "mreplay";
    }

    @Override
    public List<String> getCommandAliases() {
        return Arrays.asList("mellowreplay");
    }

    @Override
    public String getCommandUsage(ICommandSender sender) {
        return "/mreplay [list|open|info|delete|tp]";
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args) {
        if (args.length == 0 || (args.length == 1 && args[0].trim().isEmpty())) {
            replayManager.openReplayBrowser();
            return;
        }

        if ("list".equalsIgnoreCase(args[0])) {
            replayManager.sendReplayList(sender);
            return;
        }

        String sub = args[0].toLowerCase();
        switch (sub) {
            case "open":
                requireArg(sender, args, 1, "/mreplay open <id|index>");
                if (args.length < 2) {
                    return;
                }
                ReplayCatalogEntry openEntry = replayManager.resolveReplayEntry(args[1]);
                if (openEntry == null) {
                    ChatUtils.sendCommandMessage(sender, "§cReplay not found: §f" + args[1]);
                    return;
                }
                if (replayManager.openReplay(openEntry.getMetadata().getReplayId())) {
                    ChatUtils.sendCommandMessage(sender, "§7Opening replay §f" + args[1] + "§7.");
                } else {
                    ChatUtils.sendCommandMessage(
                        sender,
                        "§cFailed to open replay: §f" + openEntry.getMetadata().getReplayId()
                    );
                }
                return;
            case "info":
                requireArg(sender, args, 1, "/mreplay info <id|index>");
                if (args.length < 2) {
                    return;
                }
                ReplayCatalogEntry entry = replayManager.resolveReplayEntry(args[1]);
                if (entry == null) {
                    ChatUtils.sendCommandMessage(sender, "§cReplay not found: §f" + args[1]);
                    return;
                }
                sendInfo(sender, entry.getMetadata());
                return;
            case "delete":
                requireArg(sender, args, 1, "/mreplay delete <id|index>");
                if (args.length < 2) {
                    return;
                }
                if (replayManager.deleteReplay(args[1])) {
                    ChatUtils.sendCommandMessage(sender, "§7Deleted replay §f" + args[1] + "§7.");
                } else {
                    ChatUtils.sendCommandMessage(sender, "§cReplay not found: §f" + args[1]);
                }
                return;
            case "tp":
            case "spectate":
                requireArg(sender, args, 1, "/mreplay tp <player>");
                if (args.length < 2) {
                    return;
                }
                if (!replayManager.teleportToPlayer(args[1])) {
                    ChatUtils.sendCommandMessage(
                        sender,
                        "§cReplay player not available: §f" + args[1]
                    );
                }
                return;
            default:
                ChatUtils.sendCommandMessage(sender, "§cUsage: " + getCommandUsage(sender));
        }
    }

    private void sendInfo(ICommandSender sender, ReplayMetadata metadata) {
        ChatUtils.sendCommandMessage(sender, "§d§lReplay Info");
        ChatUtils.sendMultilineCommandMessage(
            sender,
            "§7Type: §f" + (metadata.isClip() ? "Clip" : "Replay")
        );
        ChatUtils.sendMultilineCommandMessage(
            sender,
            "§7Id: §f" + metadata.getReplayId()
        );
        ChatUtils.sendMultilineCommandMessage(
            sender,
            "§7Map: §f" + safe(metadata.getMap())
        );
        ChatUtils.sendMultilineCommandMessage(
            sender,
            "§7Mode: §f" + safe(metadata.getMode())
        );
        ChatUtils.sendMultilineCommandMessage(
            sender,
            "§7Duration: §f" + Math.max(0, metadata.getVisibleDurationMs() / 1000) + "s"
        );
        ChatUtils.sendMultilineCommandMessage(
            sender,
            "§7Packets: §f" + metadata.getPacketCount()
        );
    }

    private void requireArg(
        ICommandSender sender,
        String[] args,
        int index,
        String usage
    ) {
        if (args.length <= index) {
            ChatUtils.sendCommandMessage(sender, "§cUsage: " + usage);
        }
    }

    private String safe(String value) {
        return value == null || value.trim().isEmpty() ? "Unknown" : value;
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
                "list",
                "open",
                "info",
                "delete",
                "tp"
            );
        }
        return null;
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 0;
    }
}
