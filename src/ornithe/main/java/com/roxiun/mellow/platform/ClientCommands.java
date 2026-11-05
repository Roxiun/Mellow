package com.roxiun.mellow.platform;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.tree.LiteralCommandNode;
import java.util.List;
import java.util.Locale;
import java.util.function.Supplier;
import net.minecraft.client.Minecraft;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommand;
import net.minecraft.command.ICommandSender;
import net.minecraft.util.ChatComponentText;
import org.polyfrost.oneconfig.api.commands.v1.CommandManager;

/** Adapts existing Minecraft commands to OneConfig's dispatcher and completion pipeline. */
public final class ClientCommands {
    private ClientCommands() {}

    public static void register(ICommand command) {
        CommandManager.INSTANCE.register(createNode(command.getCommandName(), command,
            () -> Minecraft.getMinecraft().thePlayer));
        for (String alias : command.getCommandAliases()) {
            CommandManager.INSTANCE.register(createNode(alias, command,
                () -> Minecraft.getMinecraft().thePlayer));
        }
    }

    static <S> LiteralCommandNode<S> createNode(String name, ICommand command,
                                               Supplier<ICommandSender> sender) {
        return LiteralArgumentBuilder.<S>literal(name)
            .executes(context -> execute(command, sender.get(), new String[0]))
            .then(RequiredArgumentBuilder.<S, String>argument("arguments", StringArgumentType.greedyString())
                .executes(context -> execute(command, sender.get(),
                    StringArgumentType.getString(context, "arguments").trim().split("\\s+")))
                .suggests((context, builder) -> {
                    ICommandSender player = sender.get();
                    if (player == null) return builder.buildFuture();
                    String remaining = builder.getRemaining();
                    String[] args = remaining.split("\\s+", -1);
                    List<String> suggestions = command.addTabCompletionOptions(player, args, player.getPosition());
                    var word = builder.createOffset(builder.getInput().length() - args[args.length - 1].length());
                    if (suggestions != null) {
                        String prefix = word.getRemaining().toLowerCase(Locale.ROOT);
                        for (String suggestion : suggestions) {
                            if (suggestion.toLowerCase(Locale.ROOT).startsWith(prefix)) word.suggest(suggestion);
                        }
                    }
                    return word.buildFuture();
                }))
            .build();
    }

    private static int execute(ICommand command, ICommandSender sender, String[] args) {
        if (sender == null) return 0;
        try {
            command.processCommand(sender, args);
            return 1;
        } catch (CommandException error) {
            sender.addChatMessage(new ChatComponentText("§c" + error.getMessage()));
            return 0;
        }
    }
}
