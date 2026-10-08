package com.roxiun.mellow.commands;

import com.roxiun.mellow.util.blacklist.BlacklistCommandResolver;
import java.util.Arrays;
import java.util.List;
import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.util.ChatComponentText;

public class MellowCommand extends CommandBase {

    @Override
    public String getCommandName() {
        return "mellow";
    }

    @Override
    public String getCommandUsage(ICommandSender sender) {
        return "/mellow";
    }

    @Override
    public List<String> getCommandAliases() {
        return Arrays.asList("st");
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args) {
        sender.addChatMessage(new ChatComponentText("§r§5§l ⋆˙⟡ mellow ✧˚ §r"));
        sender.addChatMessage(new ChatComponentText("§r§d     by roxiun"));
        sender.addChatMessage(
            new ChatComponentText(
                "§r§7original made by§d melissalmao,§r§7 fontaine by§d xanning"
            )
        );
        sender.addChatMessage(new ChatComponentText(""));
        sender.addChatMessage(
            new ChatComponentText(
                "§r§dzifro §7for name & upgrades hud, §r§djqsie §7for original emerald counter, §r§dignmuffin §7for various major new features, §r§derror-PNF §7for profile viewer"
            )
        );
        sender.addChatMessage(new ChatComponentText(""));
        sender.addChatMessage(
            new ChatComponentText(
                "§r§7Settings: OneConfig menu. Help: /mellow or /st."
            )
        );
        sender.addChatMessage(new ChatComponentText(""));

        sender.addChatMessage(new ChatComponentText(""));
        sender.addChatMessage(
            new ChatComponentText(
                "§r§5/bw <username>:§d Check a player's Bedwars stats and blacklist tags from all enabled APIs and the local blacklist.§r"
            )
        );
        sender.addChatMessage(
            new ChatComponentText(
                "§r§5/sw <username>:§d Manually check skywars stats of a player.§r"
            )
        );
        sender.addChatMessage(
            new ChatComponentText(
                "§r§5/pv [username] (aliases: /profileviewer, /bedwarsprofileviewer, /bwprofileviewer):§d Open the profile viewer UI for a player (self by default).§r"
            )
        );
        sender.addChatMessage(
            new ChatComponentText(
                "§r§5" +
                    BlacklistCommandResolver.getCommandPrefix() +
                    " <add | remove | list | import> (alias: /bl):§d Manage your local blacklist.§r"
            )
        );
        sender.addChatMessage(
            new ChatComponentText(
                "§r§5/annoylist <add | remove | list | import> (alias: /annoy):§d Add/remove/sync a player to your local annoy list.§r"
            )
        );
        sender.addChatMessage(
            new ChatComponentText(
                "§r§5/tagignore <add | remove | list | import> (alias: /tignore):§d Suppress Coral/Xadia tag alert lines for selected players.§r"
            )
        );
        sender.addChatMessage(
            new ChatComponentText(
                "§r§5" +
                    "/coral|/urchin|/murchin <username>" +
                    ":§d View a player's Coral tags.§r"
            )
        );

        sender.addChatMessage(
            new ChatComponentText(
                "§r§5/xadia|/mxadia <username>:§d View a player's Xadia tags.§r"
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
                "§r§5/refresh:§d Re-fetch tab stats for all visible players in the current live match.§r"
            )
        );
        sender.addChatMessage(
            new ChatComponentText(
                "§r§5/mstatus <username>:§d Show Hypixel online status, last login, and Luna lobby message data.§r"
            )
        );
        sender.addChatMessage(
            new ChatComponentText(
                "§r§5/namehistory|/nameh|/names|/nh <username>:§d View merged name history from Ashcon, Laby.net, and NameMC.§r"
            )
        );

        sender.addChatMessage(
            new ChatComponentText(
                "§r§5/winstreak <username>:§d Show visible BedWars winstreak first, then Aurora fallback if configured.§r"
            )
        );
        sender.addChatMessage(
            new ChatComponentText(
                "§r§5/clearcache:§d Clear profile, tab, ping, and client caches if you're having issues.§r"
            )
        );
        sender.addChatMessage(
            new ChatComponentText(
                "§r§5/mdebug <all|state|scoreboard|pregame> (alias: /mellowdebug; board = scoreboard):§d Debug Mod API and game-state detection.§r"
            )
        );
        sender.addChatMessage(
            new ChatComponentText(
                "§r§5/mreplay (alias: /mellowreplay):§d Open the replay browser. Use list, open/info/delete <id|index>, or tp/spectate <player>.§r"
            )
        );
        sender.addChatMessage(
            new ChatComponentText(
                "§r§5/who:§d Hypixel command (optional auto-send); tab stats now fetch dynamically in-game.§r"
            )
        );
        sender.addChatMessage(new ChatComponentText(""));
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 0;
    }
}
