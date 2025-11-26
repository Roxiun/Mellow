package com.strawberry.statsify.commands;

import com.strawberry.statsify.util.skins.SkinUtils;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;

public class SkinDenickCommand extends CommandBase {

    @Override
    public String getCommandName() {
        return "skindenick";
    }

    @Override
    public String getCommandUsage(ICommandSender sender) {
        return "/skindenick <player>";
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args) {
        if (args.length != 1) {
            sender.addChatMessage(
                new ChatComponentText(
                    EnumChatFormatting.RED + "Usage: " + getCommandUsage(sender)
                )
            );
            return;
        }

        String playerName = args[0];
        NetworkPlayerInfo playerInfo = null;

        for (NetworkPlayerInfo info : Minecraft.getMinecraft()
            .getNetHandler()
            .getPlayerInfoMap()) {
            if (info.getGameProfile().getName().equalsIgnoreCase(playerName)) {
                playerInfo = info;
                break;
            }
        }

        if (playerInfo == null) {
            sender.addChatMessage(
                new ChatComponentText(
                    EnumChatFormatting.RED + "Player not found: " + playerName
                )
            );
            return;
        }

        String realName = SkinUtils.getRealName(playerInfo);

        if (realName != null) {
            sender.addChatMessage(
                new ChatComponentText(
                    EnumChatFormatting.GREEN +
                        "The real name of " +
                        playerName +
                        " is " +
                        realName
                )
            );
        } else {
            sender.addChatMessage(
                new ChatComponentText(
                    EnumChatFormatting.RED +
                        "Could not retrieve the real name for " +
                        playerName +
                        ". They might be using a default skin."
                )
            );
        }
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 0;
    }

    @Override
    public List<String> addTabCompletionOptions(
        ICommandSender sender,
        String[] args,
        net.minecraft.util.BlockPos pos
    ) {
        if (args.length == 1) {
            return getListOfStringsMatchingLastWord(
                args,
                Minecraft.getMinecraft()
                    .getNetHandler()
                    .getPlayerInfoMap()
                    .stream()
                    .map(info -> info.getGameProfile().getName())
                    .toArray(String[]::new)
            );
        }
        return null;
    }
}
