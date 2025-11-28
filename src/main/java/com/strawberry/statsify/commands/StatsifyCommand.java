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
        sender.addChatMessage(new ChatComponentText("§r§5§l ⋆˙⟡ mellow ✧˚ §r"));
        sender.addChatMessage(new ChatComponentText("§r§d     by roxiun"));
        sender.addChatMessage(
            new ChatComponentText(
                "§r§7original made by§d melissalmao,§r§7 fontaine by§d xanning, §r§7name by §dzifro"
            )
        );
        sender.addChatMessage(new ChatComponentText(""));
        sender.addChatMessage(
            new ChatComponentText(
                "§r§5/bw <username>:§d Manually check bedwars stats of a player.§r"
            )
        );
        sender.addChatMessage(
            new ChatComponentText(
                "§r§5/blacklist <add | remove | list> <player> <reason>:§d Add a player to your local blacklist.§r"
            )
        );
        sender.addChatMessage(
            new ChatComponentText(
                "§r§5/denick <finals | beds> <number>:§d Manually denick a player based on finals or beds.§r"
            )
        );
        sender.addChatMessage(
            new ChatComponentText(
                "§r§5/skindenick <username>:§d Manually denick a player based on their skin.§r"
            )
        );
        sender.addChatMessage(
            new ChatComponentText(
                "§r§5/cleartabcache:§d Clear stats cache of players if you're having issues.§r"
            )
        );
        sender.addChatMessage(
            new ChatComponentText(
                "§r§5/who:§d Check and print the stats of the players in your lobby.§r"
            )
        );
        sender.addChatMessage(new ChatComponentText(""));
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 0;
    }
}
