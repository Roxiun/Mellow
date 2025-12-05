package com.roxiun.mellow.util;

import net.minecraft.util.EnumChatFormatting;

public enum MinecraftColor {
    BLACK("Black", EnumChatFormatting.BLACK, 0, 0, 0),
    DARK_BLUE("Dark Blue", EnumChatFormatting.DARK_BLUE, 0, 0, 170),
    DARK_GREEN("Dark Green", EnumChatFormatting.DARK_GREEN, 0, 170, 0),
    DARK_AQUA("Dark Aqua", EnumChatFormatting.DARK_AQUA, 0, 170, 170),
    DARK_RED("Dark Red", EnumChatFormatting.DARK_RED, 170, 0, 0),
    DARK_PURPLE("Dark Purple", EnumChatFormatting.DARK_PURPLE, 170, 0, 170),
    GOLD("Gold", EnumChatFormatting.GOLD, 255, 170, 0),
    GRAY("Gray", EnumChatFormatting.GRAY, 170, 170, 170),
    DARK_GRAY("Dark Gray", EnumChatFormatting.DARK_GRAY, 85, 85, 85),
    BLUE("Blue", EnumChatFormatting.BLUE, 85, 85, 255),
    GREEN("Green", EnumChatFormatting.GREEN, 85, 255, 85),
    AQUA("Aqua", EnumChatFormatting.AQUA, 85, 255, 255),
    RED("Red", EnumChatFormatting.RED, 255, 85, 85),
    LIGHT_PURPLE("Light Purple", EnumChatFormatting.LIGHT_PURPLE, 255, 85, 255),
    YELLOW("Yellow", EnumChatFormatting.YELLOW, 255, 255, 85),
    WHITE("White", EnumChatFormatting.WHITE, 255, 255, 255);

    private final String displayName;
    private final EnumChatFormatting formatting;
    private final int red;
    private final int green;
    private final int blue;

    MinecraftColor(String displayName, EnumChatFormatting formatting, int red, int green, int blue) {
        this.displayName = displayName;
        this.formatting = formatting;
        this.red = red;
        this.green = green;
        this.blue = blue;
    }

    public String getDisplayName() {
        return displayName;
    }

    public EnumChatFormatting getFormatting() {
        return formatting;
    }

    public int getRed() {
        return red;
    }

    public int getGreen() {
        return green;
    }

    public int getBlue() {
        return blue;
    }

    public String getColorCode() {
        return formatting.toString();
    }

    public static String getFormattedText(String text, MinecraftColor color) {
        return color.getColorCode() + text;
    }

    public static MinecraftColor fromIndex(int index) {
        if (index < 0 || index >= values().length) {
            return WHITE; // default to white
        }
        return values()[index];
    }
}
