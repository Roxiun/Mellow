package com.roxiun.mellow.feature.nicks;

/** Shared interpretation of the saved dropdown indices for automatic lookups and /denick. */
public final class DenickSearchOptions {
    private static final int[] RANGES = {0, 50, 100, 200, 500};
    private static final int[] LIMITS = {5, 10, 20};

    private DenickSearchOptions() {}

    public static int range(int index) {
        return RANGES[Math.max(0, Math.min(RANGES.length - 1, index))];
    }

    public static int limit(int index) {
        return LIMITS[Math.max(0, Math.min(LIMITS.length - 1, index))];
    }
}
