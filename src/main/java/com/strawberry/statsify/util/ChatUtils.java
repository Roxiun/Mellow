package com.strawberry.statsify.util;

import net.minecraft.client.Minecraft;
import net.minecraft.command.ICommandSender;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.IChatComponent;

public class ChatUtils {

    private static final String PREFIX = "§r§8[§5Mellow§8]§r ";

    public static void sendMessage(String message) {
        if (Minecraft.getMinecraft().thePlayer == null) return;
        Minecraft.getMinecraft().thePlayer.addChatMessage(
            new ChatComponentText(PREFIX + message)
        );
    }

    public static void sendMessage(IChatComponent message) {
        if (Minecraft.getMinecraft().thePlayer == null) return;
        IChatComponent prefixComponent = new ChatComponentText(PREFIX);
        Minecraft.getMinecraft().thePlayer.addChatMessage(
            prefixComponent.appendSibling(message)
        );
    }

    public static void sendCommandMessage(
        ICommandSender sender,
        String message
    ) {
        sender.addChatMessage(new ChatComponentText(PREFIX + message));
    }

    public static void sendCommandMessage(
        ICommandSender sender,
        IChatComponent message
    ) {
        IChatComponent prefixComponent = new ChatComponentText(PREFIX);
        sender.addChatMessage(prefixComponent.appendSibling(message));
    }
}
