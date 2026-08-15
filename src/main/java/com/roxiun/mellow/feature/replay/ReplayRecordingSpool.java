package com.roxiun.mellow.feature.replay;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.Closeable;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class ReplayRecordingSpool implements Closeable {

    static final class IndexEntry {
        private final int timestampMs;
        private final int packetIndex;

        private IndexEntry(int timestampMs, int packetIndex) {
            this.timestampMs = timestampMs;
            this.packetIndex = packetIndex;
        }

        int getTimestampMs() {
            return timestampMs;
        }

        int getPacketIndex() {
            return packetIndex;
        }
    }

    private final File directory;
    private final File packetsFile;
    private final File chatsFile;
    private final File scoreboardsFile;
    private final File localSnapshotsFile;
    private final List<String> packetTypes = new ArrayList<>();
    private final Map<String, Integer> packetTypeIds = new LinkedHashMap<>();
    private final List<IndexEntry> indexEntries = new ArrayList<>();
    private final DataOutputStream packetsOut;
    private final DataOutputStream chatsOut;
    private final DataOutputStream scoreboardsOut;
    private final DataOutputStream localSnapshotsOut;

    private int packetCount;
    private int chatCount;
    private int scoreboardCount;
    private int localSnapshotCount;
    private int nextSecondMark;
    private long bytesWritten;
    private int activeSnapshots;
    private boolean deleteRequested;
    private boolean closed;

    ReplayRecordingSpool(File replayRoot) throws IOException {
        this(replayRoot, "recording-");
    }

    ReplayRecordingSpool(File replayRoot, String prefix) throws IOException {
        File tempRoot = new File(replayRoot, ".tmp");
        if (!tempRoot.exists()) {
            tempRoot.mkdirs();
        }
        this.directory = Files
            .createTempDirectory(tempRoot.toPath(), prefix == null ? "recording-" : prefix)
            .toFile();
        this.packetsFile = new File(directory, "packets.tmp");
        this.chatsFile = new File(directory, "chats.tmp");
        this.scoreboardsFile = new File(directory, "scoreboards.tmp");
        this.localSnapshotsFile = new File(directory, "locals.tmp");
        this.packetsOut = openOutput(packetsFile);
        this.chatsOut = openOutput(chatsFile);
        this.scoreboardsOut = openOutput(scoreboardsFile);
        this.localSnapshotsOut = openOutput(localSnapshotsFile);
    }

    void appendPacket(ReplayPacketFrame frame) throws IOException {
        ensureOpen();
        int timestamp = frame == null ? 0 : frame.getTimestampMs();
        while (timestamp >= nextSecondMark) {
            indexEntries.add(new IndexEntry(nextSecondMark, packetCount));
            nextSecondMark += 1000;
        }

        int typeId = ensurePacketTypeId(frame == null ? "" : frame.getClassName());
        packetsOut.writeInt(timestamp);
        packetsOut.writeInt(typeId);
        writeByteArray(packetsOut, frame == null ? null : frame.getPayload());
        bytesWritten += 12L + (frame == null || frame.getPayload() == null ? 0L : frame.getPayload().length);
        packetCount++;
    }

    void appendChat(ReplayChatEvent event) throws IOException {
        ensureOpen();
        chatsOut.writeInt(event == null ? 0 : event.getTimestampMs());
        writeString(chatsOut, event == null ? "" : event.getComponentJson());
        chatsOut.writeByte(event == null ? 0 : (event.getType() & 0xFF));
        bytesWritten += 9L + utf8Length(event == null ? "" : event.getComponentJson());
        chatCount++;
    }

    void appendScoreboard(ReplayScoreboardFrame frame) throws IOException {
        ensureOpen();
        scoreboardsOut.writeInt(frame == null ? 0 : frame.getTimestampMs());
        writeString(scoreboardsOut, frame == null ? "" : frame.getTitle());
        List<String> lines = frame == null
            ? Collections.<String>emptyList()
            : frame.getLines();
        scoreboardsOut.writeInt(lines.size());
        for (String line : lines) {
            writeString(scoreboardsOut, line);
        }
        bytesWritten += 12L + utf8Length(frame == null ? "" : frame.getTitle());
        for (String line : lines) {
            bytesWritten += 4L + utf8Length(line);
        }
        scoreboardCount++;
    }

    void appendLocalSnapshot(ReplayLocalPlayerSnapshot snapshot) throws IOException {
        ensureOpen();
        localSnapshotsOut.writeInt(snapshot == null ? 0 : snapshot.getTimestampMs());
        localSnapshotsOut.writeDouble(snapshot == null ? 0.0D : snapshot.getX());
        localSnapshotsOut.writeDouble(snapshot == null ? 0.0D : snapshot.getY());
        localSnapshotsOut.writeDouble(snapshot == null ? 0.0D : snapshot.getZ());
        localSnapshotsOut.writeFloat(snapshot == null ? 0.0F : snapshot.getYaw());
        localSnapshotsOut.writeFloat(snapshot == null ? 0.0F : snapshot.getPitch());
        localSnapshotsOut.writeBoolean(snapshot != null && snapshot.isSneaking());
        localSnapshotsOut.writeBoolean(snapshot != null && snapshot.isSprinting());
        bytesWritten += 38L;
        localSnapshotCount++;
    }

    long getBytesWritten() {
        return bytesWritten;
    }

    synchronized ReplayRecordingSpoolSnapshot snapshot() throws IOException {
        ensureOpen();
        flush();
        activeSnapshots++;
        return new ReplayRecordingSpoolSnapshot(
            this,
            packetsFile,
            chatsFile,
            scoreboardsFile,
            localSnapshotsFile,
            new ArrayList<>(packetTypes),
            new ArrayList<>(indexEntries),
            packetCount,
            chatCount,
            scoreboardCount,
            localSnapshotCount,
            bytesWritten
        );
    }

    void flush() throws IOException {
        ensureOpen();
        packetsOut.flush();
        chatsOut.flush();
        scoreboardsOut.flush();
        localSnapshotsOut.flush();
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

    List<String> getPacketTypes() {
        return Collections.unmodifiableList(packetTypes);
    }

    List<IndexEntry> getIndexEntries() {
        return Collections.unmodifiableList(indexEntries);
    }

    File getPacketsFile() {
        return packetsFile;
    }

    File getChatsFile() {
        return chatsFile;
    }

    File getScoreboardsFile() {
        return scoreboardsFile;
    }

    File getLocalSnapshotsFile() {
        return localSnapshotsFile;
    }

    DataInputStream openPacketsInput() throws IOException {
        finish();
        return openInput(packetsFile);
    }

    DataInputStream openChatsInput() throws IOException {
        finish();
        return openInput(chatsFile);
    }

    DataInputStream openScoreboardsInput() throws IOException {
        finish();
        return openInput(scoreboardsFile);
    }

    DataInputStream openLocalSnapshotsInput() throws IOException {
        finish();
        return openInput(localSnapshotsFile);
    }

    void finish() throws IOException {
        if (closed) {
            return;
        }
        IOException failure = null;
        failure = closeQuietly(packetsOut, failure);
        failure = closeQuietly(chatsOut, failure);
        failure = closeQuietly(scoreboardsOut, failure);
        failure = closeQuietly(localSnapshotsOut, failure);
        closed = true;
        if (failure != null) {
            throw failure;
        }
    }

    synchronized void discard() {
        try {
            finish();
        } catch (IOException ignored) {}
        deleteRequested = true;
        deleteIfReleased();
    }

    @Override
    public void close() throws IOException {
        finish();
    }

    synchronized void releaseSnapshot() {
        if (activeSnapshots > 0) {
            activeSnapshots--;
        }
        deleteIfReleased();
    }

    private void deleteIfReleased() {
        if (deleteRequested && activeSnapshots == 0) {
            deleteRecursively(directory);
        }
    }

    private int ensurePacketTypeId(String className) {
        String safeClassName = className == null ? "" : className;
        Integer existing = packetTypeIds.get(safeClassName);
        if (existing != null) {
            return existing.intValue();
        }
        int nextId = packetTypes.size();
        packetTypeIds.put(safeClassName, nextId);
        packetTypes.add(safeClassName);
        return nextId;
    }

    private void ensureOpen() throws IOException {
        if (closed) {
            throw new IOException("Replay recording spool is already closed.");
        }
    }

    private static DataOutputStream openOutput(File file) throws IOException {
        return new DataOutputStream(
            new BufferedOutputStream(new FileOutputStream(file))
        );
    }

    private static DataInputStream openInput(File file) throws IOException {
        return new DataInputStream(
            new BufferedInputStream(new FileInputStream(file))
        );
    }

    private static IOException closeQuietly(
        Closeable closeable,
        IOException existing
    ) {
        try {
            closeable.close();
            return existing;
        } catch (IOException e) {
            return existing == null ? e : existing;
        }
    }

    private static void writeByteArray(DataOutputStream out, byte[] bytes)
        throws IOException {
        byte[] value = bytes == null ? new byte[0] : bytes;
        out.writeInt(value.length);
        out.write(value);
    }

    private static void writeString(DataOutputStream out, String value)
        throws IOException {
        byte[] bytes = (value == null ? "" : value).getBytes(StandardCharsets.UTF_8);
        out.writeInt(bytes.length);
        out.write(bytes);
    }

    private static int utf8Length(String value) {
        return (value == null ? "" : value).getBytes(StandardCharsets.UTF_8).length;
    }

    private static boolean deleteRecursively(File file) {
        if (file == null || !file.exists()) {
            return true;
        }
        if (file.isDirectory()) {
            File[] children = file.listFiles();
            if (children != null) {
                for (File child : children) {
                    if (!deleteRecursively(child)) {
                        return false;
                    }
                }
            }
        }
        return file.delete();
    }
}
