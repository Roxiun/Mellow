package com.roxiun.mellow.util.ping;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class PingRetryGate {

    private final Map<String, Long> requestedPlayers = new ConcurrentHashMap<>();

    public synchronized boolean tryMarkRequested(String playerId) {
        if (playerId == null || playerId.trim().isEmpty()) {
            return false;
        }

        long now = System.currentTimeMillis();
        requestedPlayers.entrySet().removeIf(entry -> entry.getValue() <= now);
        if (requestedPlayers.containsKey(playerId)) return false;
        requestedPlayers.put(playerId, now + 10_000L);
        return true;
    }

    public void clearPlayer(String playerId) {
        if (playerId == null || playerId.trim().isEmpty()) {
            return;
        }

        requestedPlayers.remove(playerId);
    }

    public void clear() {
        requestedPlayers.clear();
    }
}
