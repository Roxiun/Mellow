package com.roxiun.mellow.commands;
import java.util.Arrays;
final class BlacklistAddRequest {
    private final String playerName;
    private final String reason;
    private BlacklistAddRequest(String name, String reason) { this.playerName = name; this.reason = reason; }
    public static BlacklistAddRequest parse(String commandPrefix, String[] args) {
        String reason = String.join(" ", Arrays.copyOfRange(args, 2, args.length)).trim();
        return new BlacklistAddRequest(args[1], reason.isEmpty() ? "(none)" : reason);
    }
    public String getPlayerName() { return playerName; }
    public String getLocalReason() { return reason; }
}
