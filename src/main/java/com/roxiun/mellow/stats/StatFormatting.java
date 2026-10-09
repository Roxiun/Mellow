package com.roxiun.mellow.stats;

import java.util.Locale;

public final class StatFormatting {
    private StatFormatting() {}
    public static String formatTabCountForDisplay(String value) {
        if (value == null || value.isEmpty()) {
            return value;
        }

        int index = 0;
        while (index + 1 < value.length() && value.charAt(index) == '§') {
            index += 2;
        }

        String prefix = value.substring(0, index);
        String numericPart = value.substring(index);
        if (numericPart.isEmpty()) {
            return value;
        }

        try {
            long parsed = Long.parseLong(numericPart);
            return prefix + String.format(Locale.US, "%,d", parsed);
        } catch (NumberFormatException ignored) {
            return value;
        }
    }
}
