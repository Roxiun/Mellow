package com.roxiun.mellow.util.blacklist;

public class BlacklistedPlayer {
    private final String name;
    private final String reason;

    public BlacklistedPlayer(String name, String reason) {
        this.name = name;
        this.reason = reason;
    }

    public String getName() {
        return name;
    }

    public String getReason() {
        return reason;
    }
}
