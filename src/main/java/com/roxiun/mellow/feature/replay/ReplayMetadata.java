package com.roxiun.mellow.feature.replay;

import java.util.UUID;

public class ReplayMetadata {

    public static final String KIND_REPLAY = "replay";
    public static final String KIND_CLIP = "clip";

    private String replayId;
    private String kind = KIND_REPLAY;
    private String map;
    private String mode;
    private String serverName;
    private String gameType;
    private long startedAt;
    private long endedAt;
    private int durationMs;
    private int playbackStartMs;
    private long savedAt;
    private int packetCount;
    private UUID viewerUuid;
    private String viewerName;
    private Integer recordedPlayerEntityId;
    private UUID recordedPlayerUuid;
    private String recordedPlayerName;
    private int formatVersion = 1;

    public String getReplayId() {
        return replayId;
    }

    public void setReplayId(String replayId) {
        this.replayId = replayId;
    }

    public String getKind() {
        return KIND_CLIP.equalsIgnoreCase(kind) ? KIND_CLIP : KIND_REPLAY;
    }

    public void setKind(String kind) {
        this.kind = KIND_CLIP.equalsIgnoreCase(kind) ? KIND_CLIP : KIND_REPLAY;
    }

    public boolean isClip() {
        return KIND_CLIP.equals(getKind());
    }

    public String getMap() {
        return map == null ? "" : map;
    }

    public void setMap(String map) {
        this.map = map;
    }

    public String getMode() {
        return mode == null ? "" : mode;
    }

    public void setMode(String mode) {
        this.mode = mode;
    }

    public String getServerName() {
        return serverName == null ? "" : serverName;
    }

    public void setServerName(String serverName) {
        this.serverName = serverName;
    }

    public String getGameType() {
        return gameType == null ? "" : gameType;
    }

    public void setGameType(String gameType) {
        this.gameType = gameType;
    }

    public long getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(long startedAt) {
        this.startedAt = startedAt;
    }

    public long getEndedAt() {
        return endedAt;
    }

    public void setEndedAt(long endedAt) {
        this.endedAt = endedAt;
    }

    public int getDurationMs() {
        return durationMs;
    }

    public void setDurationMs(int durationMs) {
        this.durationMs = durationMs;
    }

    public int getPlaybackStartMs() {
        return Math.max(0, Math.min(durationMs, playbackStartMs));
    }

    public void setPlaybackStartMs(int playbackStartMs) {
        this.playbackStartMs = Math.max(0, playbackStartMs);
    }

    public int getVisibleDurationMs() {
        return Math.max(0, durationMs - getPlaybackStartMs());
    }

    public long getSavedAt() {
        return savedAt;
    }

    public void setSavedAt(long savedAt) {
        this.savedAt = savedAt;
    }

    public long getCatalogTimestamp() {
        return savedAt > 0L ? savedAt : startedAt;
    }

    public int getPacketCount() {
        return packetCount;
    }

    public void setPacketCount(int packetCount) {
        this.packetCount = packetCount;
    }

    public UUID getViewerUuid() {
        return viewerUuid;
    }

    public void setViewerUuid(UUID viewerUuid) {
        this.viewerUuid = viewerUuid;
    }

    public String getViewerName() {
        return viewerName == null ? "" : viewerName;
    }

    public void setViewerName(String viewerName) {
        this.viewerName = viewerName;
    }

    public Integer getRecordedPlayerEntityId() {
        return recordedPlayerEntityId;
    }

    public void setRecordedPlayerEntityId(Integer recordedPlayerEntityId) {
        this.recordedPlayerEntityId = recordedPlayerEntityId;
    }

    public UUID getRecordedPlayerUuid() {
        return recordedPlayerUuid;
    }

    public void setRecordedPlayerUuid(UUID recordedPlayerUuid) {
        this.recordedPlayerUuid = recordedPlayerUuid;
    }

    public String getRecordedPlayerName() {
        return recordedPlayerName == null ? "" : recordedPlayerName;
    }

    public void setRecordedPlayerName(String recordedPlayerName) {
        this.recordedPlayerName = recordedPlayerName;
    }

    public int getFormatVersion() {
        return formatVersion;
    }

    public void setFormatVersion(int formatVersion) {
        this.formatVersion = formatVersion;
    }
}
