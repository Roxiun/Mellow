package com.roxiun.mellow.api.seraph;

/** Seraph is retained for configuration compatibility, but no longer contacted. */
public final class SeraphAvailability {
    public static final String DISABLED_MESSAGE =
        "Seraph is deprecated in Mellow; all Seraph network requests are disabled.";

    private SeraphAvailability() {}

    public static boolean isEnabled() {
        return false;
    }
}
