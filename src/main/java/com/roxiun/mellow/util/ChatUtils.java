package com.roxiun.mellow.util;

import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.command.ICommandSender;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.IChatComponent;

public class ChatUtils {

    private static final String PREFIX = "§r§8[§5Mellow§8]§r ";
    private static final String MULTILINE_PREFIX = "§r§5▐§r ";
    // Minecraft 1.8.9 limits the entire chat packet, including the command, to 100 characters.
    private static final int MAX_OUTBOUND_CHAT_LENGTH = 100;

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

    public static void sendChatCommandMessage(
        String commandPrefix,
        String message
    ) {
        String command = formatChatCommandMessage(commandPrefix, message);
        if (command != null && Minecraft.getMinecraft().thePlayer != null)
            Minecraft.getMinecraft().thePlayer.sendChatMessage(command);
    }

    static String formatChatCommandMessage(String commandPrefix, String message) {
        if (commandPrefix == null || commandPrefix.trim().isEmpty()) return null;
        if (message == null) return null;
        String prefix = commandPrefix.trim();
        if (!prefix.startsWith("/")) prefix = "/" + prefix;
        String text = stripFormatting(message).replace('\r', ' ').replace('\n', ' ').trim();
        int available = MAX_OUTBOUND_CHAT_LENGTH - prefix.length() - 1;
        if (text.isEmpty() || available < 4) return null;
        if (text.length() > available) {
            int end = available - 3;
            // Do not cut a supplementary Unicode character in half.
            if (Character.isHighSurrogate(text.charAt(end - 1))) end--;
            text = text.substring(0, end) + "...";
        }
        return prefix + " " + text;
    }

    public static String stripFormatting(String value) {
        if (value == null || value.isEmpty()) {
            return "";
        }
        return value.replaceAll("§.", "");
    }
}
