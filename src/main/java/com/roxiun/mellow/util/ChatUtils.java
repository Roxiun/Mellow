package com.roxiun.mellow.util;

import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.command.ICommandSender;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.IChatComponent;

public class ChatUtils {

    private static final String PREFIX = "§r§8[§5Mellow§8]§r ";
    private static final String MULTILINE_PREFIX = "§r§5▐§r ";

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

    public static void sendMultilineMessage(String message) {
        sendMultilineMessage(new ChatComponentText(message));
    }

    public static void sendMultilineMessage(IChatComponent message) {
        if (Minecraft.getMinecraft().thePlayer == null) return;

        IChatComponent prefixComponent = new ChatComponentText(
            MULTILINE_PREFIX
        );
        Minecraft.getMinecraft().thePlayer.addChatMessage(
            prefixComponent.appendSibling(message)
        );
    }

    public static void sendMultilineMessage(List<String> messages) {
        if (Minecraft.getMinecraft().thePlayer == null) return;

        for (String msg : messages) {
            sendMultilineMessage(msg); // reuse the String version
        }
    }

    public static void sendMultilineCommandMessage(
        ICommandSender sender,
        String message
    ) {
        sendMultilineCommandMessage(sender, new ChatComponentText(message));
    }

    public static void sendMultilineCommandMessage(
        ICommandSender sender,
        IChatComponent message
    ) {
        IChatComponent prefixComponent = new ChatComponentText(
            MULTILINE_PREFIX
        );
        sender.addChatMessage(prefixComponent.appendSibling(message));
    }

    public static void sendMultilineCommandMessage(
        ICommandSender sender,
        List<String> messages
    ) {
        for (String msg : messages) {
            sendMultilineCommandMessage(sender, msg); // reuse the String version
        }
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
