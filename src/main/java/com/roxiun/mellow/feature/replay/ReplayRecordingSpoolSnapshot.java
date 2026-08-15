package com.roxiun.mellow.feature.replay;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.List;

final class ReplayRecordingSpoolSnapshot implements AutoCloseable {

    private final ReplayRecordingSpool owner;
    private final File packetsFile;
    private final File chatsFile;
    private final File scoreboardsFile;
    private final File localSnapshotsFile;
    private final List<String> packetTypes;
    private final List<ReplayRecordingSpool.IndexEntry> indexEntries;
    private final int packetCount;
    private final int chatCount;
    private final int scoreboardCount;
    private final int localSnapshotCount;
    private final long bytesWritten;
    private boolean released;

    ReplayRecordingSpoolSnapshot(
        ReplayRecordingSpool owner,
        File packetsFile,
        File chatsFile,
        File scoreboardsFile,
        File localSnapshotsFile,
        List<String> packetTypes,
        List<ReplayRecordingSpool.IndexEntry> indexEntries,
        int packetCount,
        int chatCount,
        int scoreboardCount,
        int localSnapshotCount,
        long bytesWritten
    ) {
        this.owner = owner;
        this.packetsFile = packetsFile;
        this.chatsFile = chatsFile;
        this.scoreboardsFile = scoreboardsFile;
        this.localSnapshotsFile = localSnapshotsFile;
        this.packetTypes = Collections.unmodifiableList(packetTypes);
        this.indexEntries = Collections.unmodifiableList(indexEntries);
        this.packetCount = packetCount;
        this.chatCount = chatCount;
        this.scoreboardCount = scoreboardCount;
        this.localSnapshotCount = localSnapshotCount;
        this.bytesWritten = bytesWritten;
    }

    List<String> getPacketTypes() {
        return packetTypes;
    }

    List<ReplayRecordingSpool.IndexEntry> getIndexEntries() {
        return indexEntries;
    }

    int getPacketCount() {
        return packetCount;
    }

    int getChatCount() {
        return chatCount;
    }

    int getScoreboardCount() {
        return scoreboardCount;
    }

    int getLocalSnapshotCount() {
        return localSnapshotCount;
    }

    long getBytesWritten() {
        return bytesWritten;
    }

    DataInputStream openPacketsInput() throws IOException {
        return openInput(packetsFile);
    }

    DataInputStream openChatsInput() throws IOException {
        return openInput(chatsFile);
    }

    DataInputStream openScoreboardsInput() throws IOException {
        return openInput(scoreboardsFile);
    }

    DataInputStream openLocalSnapshotsInput() throws IOException {
        return openInput(localSnapshotsFile);
    }

    @Override
    public synchronized void close() {
        if (released) {
            return;
        }
        released = true;
        owner.releaseSnapshot();
    }

    private static DataInputStream openInput(File file) throws IOException {
        return new DataInputStream(
            new BufferedInputStream(new FileInputStream(file))
        );
    }
}
