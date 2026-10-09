package com.roxiun.mellow.gamestate;

import com.roxiun.mellow.stats.GameDefinition;
import com.roxiun.mellow.stats.GameRegistry;
import java.util.Collections;
import java.util.ArrayList;
import java.util.List;
import net.hypixel.data.type.GameType;

public class GameSnapshot {

    private static final GameSnapshot EMPTY = new GameSnapshot(false, "", null, "", "",
        GamePhase.UNKNOWN, "", Collections.emptyList(), PartyState.empty(), 0, 0);
    private final GameDefinition<?> statsGame;
    private final String statsMode;
    private final long scoreboardObservedAt;
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
        this(onHypixel, server, type, mode, map, phase, title, lines, party, version, sessionId,
            ScoreboardObservation.parse(title, lines), System.currentTimeMillis());
    }

    GameSnapshot(boolean onHypixel, String server, GameType type, String mode, String map,
        GamePhase phase, String title, List<String> lines, PartyState party, long version, long sessionId,
        ScoreboardObservation observation, long scoreboardObservedAt) {
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
        this.observation = observation;
        this.scoreboardObservedAt = scoreboardObservedAt;
        this.partyState = party == null ? PartyState.empty() : party;
        this.updatedAt = System.currentTimeMillis();
        this.stateVersion = version;
        this.statsGame = GameRegistry.identify(this);
        this.statsMode = statsGame == null || isLobby() ? "overall" : statsGame.detectMode(this);
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

    // Party/phase updates keep the same observed sidebar and resolved stats context.
    private GameSnapshot(GameSnapshot source, PartyState party, GamePhase phase, long sessionId) {
        this.onHypixel = source.onHypixel;
        this.serverName = source.serverName;
        this.gameType = source.gameType;
        this.mode = source.mode;
        this.map = source.map;
        this.phase = phase;
        this.sessionId = sessionId;
        this.scoreboardTitle = source.scoreboardTitle;
        this.scoreboardLines = source.scoreboardLines;
        this.observation = source.observation;
        this.scoreboardObservedAt = source.scoreboardObservedAt;
        this.statsGame = source.statsGame;
        this.statsMode = source.statsMode;
        this.partyState = party;
        this.updatedAt = System.currentTimeMillis();
        this.stateVersion = source.stateVersion + 1;
    }

    public GameSnapshot withPartyState(PartyState party) {
        return new GameSnapshot(this, party, phase, sessionId);
    }

    public GameSnapshot withPhase(GamePhase nextPhase) {
        return withPhase(nextPhase, sessionId);
    }

    GameSnapshot withPhase(GamePhase nextPhase, long sessionId) {
        return new GameSnapshot(this, partyState, nextPhase, sessionId);
    }

    public GameDefinition<?> getStatsGame() { return statsGame; }
    public String getStatsMode() { return statsMode; }
    long getScoreboardObservedAt() { return scoreboardObservedAt; }

    /** Countdown evidence expires; party updates must not make an old observation fresh. */
    public int getCountdownSeconds(long now) {
        long age = now - scoreboardObservedAt;
        if (age < 0 || age > 2000 || observation.countdownSeconds < 0) return -1;
        return observation.countdownSeconds - (int) ((age + 999) / 1000);
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
