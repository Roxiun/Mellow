package com.roxiun.mellow.util;

public class StringUtils {

    public static String parseUsername(String str) {
        str = str.trim();
        String[] words = str.split("\\s+");
        return words.length > 0 ? words[words.length - 1] : "";
    }

    public static int countOccurrences(String str, String subStr) {
        int count = 0;
        int idx = 0;
        while ((idx = str.indexOf(subStr, idx)) != -1) {
            count++;
            idx += subStr.length();
        }
        return count;
    }

    public static String extractValue(
        String text,
        String startDelimiter,
        String endDelimiter
    ) {
        int startIndex = text.indexOf(startDelimiter);
        if (startIndex == -1) return "N/A"; // Not found
        startIndex += startDelimiter.length();
        int endIndex = text.indexOf(endDelimiter, startIndex);
        if (endIndex == -1) return "N/A"; // Not found
        return text.substring(startIndex, endIndex).trim();
    }
}
