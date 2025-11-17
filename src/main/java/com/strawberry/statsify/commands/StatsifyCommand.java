package com.strawberry.statsify.commands;

import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.util.ChatComponentText;

public class StatsifyCommand extends CommandBase {

    @Override
    public String getCommandName() {
        return "st";
    }

    @Override
    public String getCommandUsage(ICommandSender sender) {
        return "/st";
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args) {
        sender.addChatMessage(
            new ChatComponentText(
                "\u00a7r\u00a7b\u00a7lstat\u00a79\u00a7tsi\u00a73\u00a7lfy\u00a7r"
            )
        );
        sender.addChatMessage(
            new ChatComponentText(
                "\u00a7r\u00a7boriginal made by\u00a7e melissalmao\u00a7r updated by\u00a7e roxiun\u00a7r"
            )
        );
        sender.addChatMessage(new ChatComponentText(""));
        sender.addChatMessage(
            new ChatComponentText(
                "\u00a7r\u00a73/bw <username>:\u00a7b Manually check bedwars stats of a player.\u00a7r"
            )
        );
        sender.addChatMessage(
            new ChatComponentText(
                "\u00a7r\u00a73/denick <finals | beds> <number>:\u00a7b Manually denick a player based on finals or beds.\u00a7r"
            )
        );
        sender.addChatMessage(
            new ChatComponentText(
                "\u00a7r\u00a73/cleartabcache:\u00a7b Clear stats cache of players if you're having issues.\u00a7r"
            )
        );
        sender.addChatMessage(
            new ChatComponentText(
                "\u00a7r\u00a73/who:\u00a7b Check and print the stats of the players in your lobby.\u00a7r"
            )
        );
        sender.addChatMessage(new ChatComponentText(""));
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 0;
    }
}
