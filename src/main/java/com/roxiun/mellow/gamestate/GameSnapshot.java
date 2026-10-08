package com.roxiun.mellow.gamestate;

import java.util.Collections;
import java.util.ArrayList;
import java.util.List;
import net.hypixel.data.type.GameType;

public class GameSnapshot {

    private static final GameSnapshot EMPTY = new GameSnapshot(false, "", null, "", "",
        GamePhase.UNKNOWN, "", Collections.emptyList(), PartyState.empty(), 0, 0);
    private final GamePhase phase;
    private final long sessionId;
    private final ScoreboardObservation observation;
    private final boolean onHypixel;
    private final String serverName;
    private final GameType gameType;
    private final String mode;
    private final String map;
    private final String scoreboardTitle;
    private final List<String> scoreboardLines;
    private final PartyState partyState;
    private final long updatedAt;
    private final long stateVersion;

    public GameSnapshot(boolean onHypixel, String server, GameType type, String mode, String map,
        GamePhase phase, String title, List<String> lines, PartyState party, long version, long sessionId) {
        this.onHypixel = onHypixel;
        this.serverName = server;
        this.gameType = type;
        this.mode = mode;
        this.map = map;
        this.phase = phase;
        this.sessionId = sessionId;
        this.scoreboardTitle = title == null ? "" : title;
        this.scoreboardLines = lines == null ? Collections.emptyList()
            : Collections.unmodifiableList(new ArrayList<>(lines));
        this.observation = ScoreboardObservation.parse(scoreboardTitle, scoreboardLines);
        this.partyState = party == null ? PartyState.empty() : party;
        this.updatedAt = System.currentTimeMillis();
        this.stateVersion = version;
    }

    public static GameSnapshot empty() {
        return EMPTY;
    }

    public boolean isOnHypixel() {
        return onHypixel;
    }

    public String getServerName() {
        return serverName;
    }

    public GameType getGameType() {
        return gameType;
    }

    public String getMode() {
        return mode;
    }

    public String getMap() {
        return map;
    }

    public boolean isLobby() {
        return phase == GamePhase.LOBBY;
    }

    public boolean isPregame() {
        return phase == GamePhase.PREGAME;
    }

    public String getScoreboardTitle() {
        return scoreboardTitle;
    }

    public List<String> getScoreboardLines() {
        return scoreboardLines;
    }

    public PartyState getPartyState() {
        return partyState;
    }

    public long getUpdatedAt() {
        return updatedAt;
    }

    public long getStateVersion() {
        return stateVersion;
    }

    public boolean isInBedwarsSession() {
        return onHypixel && gameType == GameType.BEDWARS && (phase == GamePhase.PREGAME || phase == GamePhase.LIVE);
    }

    public boolean isInBedwarsMatch() {
        return onHypixel && gameType == GameType.BEDWARS && phase == GamePhase.LIVE;
    }

    public GameSnapshot withPartyState(PartyState party) {
        return new GameSnapshot(onHypixel, serverName, gameType, mode, map, phase,
            scoreboardTitle, scoreboardLines, party, stateVersion + 1, sessionId);
    }

    public GameSnapshot withPhase(GamePhase nextPhase) {
        return new GameSnapshot(onHypixel, serverName, gameType, mode, map, nextPhase,
            scoreboardTitle, scoreboardLines, partyState, stateVersion + 1, sessionId);
    }

    public GamePhase getPhase() { return phase; }
    public long getSessionId() { return sessionId; }
    public ScoreboardObservation getObservation() { return observation; }

    public boolean hasSameState(GameSnapshot other) {
        if (other == null) {
            return false;
        }

        return (
            sessionId == other.sessionId && phase == other.phase &&
            onHypixel == other.onHypixel &&
            gameType == other.gameType &&
            safe(serverName).equals(safe(other.serverName)) &&
            safe(mode).equals(safe(other.mode)) &&
            safe(map).equals(safe(other.map)) &&
            safe(scoreboardTitle).equals(safe(other.scoreboardTitle)) &&
            scoreboardLines.equals(other.scoreboardLines) &&
            partyState.equals(other.partyState)
        );
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }
}
