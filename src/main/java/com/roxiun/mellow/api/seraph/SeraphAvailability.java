package com.roxiun.mellow.api.seraph;

/** Controls deprecated Seraph APIs; the independent Mowojang lookup remains available. */
public final class SeraphAvailability {
    public static final String DISABLED_MESSAGE =
        "Seraph is deprecated in Mellow; blacklist, client, and ping requests are disabled.";

    private SeraphAvailability() {}

    public static boolean isEnabled() {
        return false;
    }
}
