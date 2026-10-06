package com.roxiun.mellow.feature.replay;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import org.junit.Assert;
import org.junit.Test;

public class ReplayIoTest {

    private static final byte[] XZ_HEADER = new byte[] {
        (byte) 0xFD,
        '7',
        'z',
        'X',
        'Z',
        0x00,
    };

    @Test
    public void saveAndLoadRoundTripsFormatV2Replay() throws Exception {
        File tempDir = Files.createTempDirectory("replay-io-roundtrip").toFile();
        try {
            ReplayIo io = new ReplayIo();
            ReplayMetadata metadata = buildMetadata();

            File directory = io.createReplayDirectory(tempDir, metadata);
            List<ReplayPacketFrame> packets = Arrays.asList(
                new ReplayPacketFrame(0, "net.minecraft.network.play.server.S21PacketChunkData", repeatedPayload(1024, 0x11)),
                new ReplayPacketFrame(50, "net.minecraft.network.play.server.S21PacketChunkData", repeatedPayload(1024, 0x11)),
                new ReplayPacketFrame(95, "net.minecraft.network.play.server.S14PacketEntity$S17PacketEntityLookMove", repeatedPayload(128, 0x22))
            );
            List<ReplayChatEvent> chats = Collections.singletonList(
                new ReplayChatEvent(75, "{\"text\":\"hi\"}", (byte) 0)
            );
            List<ReplayScoreboardFrame> scoreboards = Collections.singletonList(
                new ReplayScoreboardFrame(100, "Bed Wars", Arrays.asList("R Red", "B Blue"))
            );
            List<ReplayLocalPlayerSnapshot> localSnapshots = Arrays.asList(
                new ReplayLocalPlayerSnapshot(0, 1.5D, 64.0D, 1.5D, 90.0F, 10.0F, false, true),
                new ReplayLocalPlayerSnapshot(150, 2.5D, 64.0D, 1.5D, 95.0F, 12.0F, true, true)
            );

            io.saveReplay(
                directory,
                metadata,
                packets,
                chats,
                scoreboards,
                localSnapshots
            );

            Assert.assertTrue(new File(directory, "packets.bin").isFile());
            Assert.assertTrue(new File(directory, "events.bin").isFile());
            Assert.assertFalse(new File(directory, "events.jsonl").exists());
            assertStartsWithXzHeader(new File(directory, "packets.bin"));
            assertStartsWithXzHeader(new File(directory, "events.bin"));

            ReplayLoadedData loaded = io.loadReplay(directory);
            Assert.assertEquals(2, loaded.getMetadata().getFormatVersion());
            Assert.assertEquals("Picnic", loaded.getMetadata().getMap());
            Assert.assertEquals("BEDWARS_TWO_FOUR", loaded.getMetadata().getMode());
            Assert.assertEquals(
                Integer.valueOf(77),
                loaded.getMetadata().getRecordedPlayerEntityId()
            );
            Assert.assertEquals(
                UUID.fromString("11111111-2222-3333-4444-555555555555"),
                loaded.getMetadata().getRecordedPlayerUuid()
            );
            Assert.assertEquals(
                "Recorder",
                loaded.getMetadata().getRecordedPlayerName()
            );
            Assert.assertEquals(3, loaded.getPackets().size());
            assertPacketEquals(packets.get(0), loaded.getPackets().get(0));
            assertPacketEquals(packets.get(1), loaded.getPackets().get(1));
            assertPacketEquals(packets.get(2), loaded.getPackets().get(2));
            Assert.assertEquals(1, loaded.getChats().size());
            Assert.assertEquals("{\"text\":\"hi\"}", loaded.getChats().get(0).getComponentJson());
            Assert.assertEquals(100, loaded.getScoreboards().get(0).getTimestampMs());
            Assert.assertEquals(Arrays.asList("R Red", "B Blue"), loaded.getScoreboards().get(0).getLines());
            Assert.assertEquals(2, loaded.getLocalSnapshots().size());
            Assert.assertEquals(95.0F, loaded.getLocalSnapshots().get(1).getYaw(), 0.0F);
            Assert.assertTrue(loaded.getLocalSnapshots().get(1).isSneaking());
        } finally {
            deleteRecursively(tempDir);
        }
    }

    @Test
    public void spooledReplaySaveMatchesListBasedOutput() throws Exception {
        File tempDir = Files.createTempDirectory("replay-io-spooled").toFile();
        ReplayRecordingSpool spool = null;
        try {
            ReplayIo io = new ReplayIo();
            ReplayMetadata listMetadata = buildMetadata();
            ReplayMetadata spoolMetadata = buildMetadata();
            List<ReplayPacketFrame> packets = Arrays.asList(
                new ReplayPacketFrame(0, "net.minecraft.network.play.server.S21PacketChunkData", repeatedPayload(1024, 0x11)),
                new ReplayPacketFrame(50, "net.minecraft.network.play.server.S21PacketChunkData", repeatedPayload(1024, 0x11)),
                new ReplayPacketFrame(95, "net.minecraft.network.play.server.S14PacketEntity$S17PacketEntityLookMove", repeatedPayload(128, 0x22))
            );
            List<ReplayChatEvent> chats = Collections.singletonList(
                new ReplayChatEvent(75, "{\"text\":\"hi\"}", (byte) 0)
            );
            List<ReplayScoreboardFrame> scoreboards = Collections.singletonList(
                new ReplayScoreboardFrame(100, "Bed Wars", Arrays.asList("R Red", "B Blue"))
            );
            List<ReplayLocalPlayerSnapshot> localSnapshots = Arrays.asList(
                new ReplayLocalPlayerSnapshot(0, 1.5D, 64.0D, 1.5D, 90.0F, 10.0F, false, true),
                new ReplayLocalPlayerSnapshot(150, 2.5D, 64.0D, 1.5D, 95.0F, 12.0F, true, true)
            );

            File listDirectory = new File(tempDir, "list");
            File spoolDirectory = new File(tempDir, "spool");
            Assert.assertTrue(listDirectory.mkdirs());
            Assert.assertTrue(spoolDirectory.mkdirs());

            io.saveReplay(
                listDirectory,
                listMetadata,
                packets,
                chats,
                scoreboards,
                localSnapshots
            );

            spool = io.createRecordingSpool(tempDir);
            for (ReplayPacketFrame packet : packets) {
                spool.appendPacket(packet);
            }
            for (ReplayChatEvent chat : chats) {
                spool.appendChat(chat);
            }
            for (ReplayScoreboardFrame scoreboard : scoreboards) {
                spool.appendScoreboard(scoreboard);
            }
            for (ReplayLocalPlayerSnapshot snapshot : localSnapshots) {
                spool.appendLocalSnapshot(snapshot);
            }

            io.saveReplay(spoolDirectory, spoolMetadata, spool);

            Assert.assertArrayEquals(
                Files.readAllBytes(new File(listDirectory, "packets.bin").toPath()),
                Files.readAllBytes(new File(spoolDirectory, "packets.bin").toPath())
            );
            Assert.assertArrayEquals(
                Files.readAllBytes(new File(listDirectory, "events.bin").toPath()),
                Files.readAllBytes(new File(spoolDirectory, "events.bin").toPath())
            );
            Assert.assertArrayEquals(
                Files.readAllBytes(new File(listDirectory, "index.bin").toPath()),
                Files.readAllBytes(new File(spoolDirectory, "index.bin").toPath())
            );
        } finally {
            if (spool != null) {
                spool.discard();
            }
            deleteRecursively(tempDir);
        }
    }

    @Test
    public void loadReplayRejectsLegacyFormat() throws Exception {
        File tempDir = Files.createTempDirectory("replay-io-legacy").toFile();
        try {
            File directory = new File(tempDir, "legacy");
            Assert.assertTrue(directory.mkdirs());
            Files.write(
                new File(directory, "meta.json").toPath(),
                (
                    "{\n" +
                    "  \"replayId\": \"legacy\",\n" +
                    "  \"formatVersion\": 1\n" +
                    "}\n"
                ).getBytes(StandardCharsets.UTF_8)
            );

            try {
                new ReplayIo().loadReplay(directory);
                Assert.fail("Expected legacy replay load to fail");
            } catch (IOException e) {
                Assert.assertTrue(e.getMessage().contains("no longer supported"));
            }
        } finally {
            deleteRecursively(tempDir);
        }
    }

    @Test
    public void loadReplaySupportsGzipV2Streams() throws Exception {
        File tempDir = Files.createTempDirectory("replay-io-gzip-v2").toFile();
        try {
            File directory = new File(tempDir, "gzip-v2");
            Assert.assertTrue(directory.mkdirs());
            // Frozen gzip v2 files keep backward compatibility independent of the current writer.
            for (String name : Arrays.asList("meta.json", "packets.bin", "events.bin")) {
                try (InputStream fixture = ReplayIoTest.class.getResourceAsStream("/replay/gzip-v2/" + name)) {
                    Assert.assertNotNull("Missing replay fixture: " + name, fixture);
                    Files.copy(fixture, new File(directory, name).toPath());
                }
            }

            ReplayLoadedData loaded = new ReplayIo().loadReplay(directory);
            Assert.assertEquals(1, loaded.getPackets().size());
            Assert.assertEquals("net.minecraft.network.play.server.S14PacketEntity$S17PacketEntityLookMove", loaded.getPackets().get(0).getClassName());
            Assert.assertEquals(1, loaded.getChats().size());
            Assert.assertEquals(1, loaded.getScoreboards().size());
            Assert.assertEquals(1, loaded.getLocalSnapshots().size());
            Assert.assertEquals(50, loaded.getPackets().get(0).getTimestampMs());
            Assert.assertArrayEquals(repeatedPayload(16, 0x33), loaded.getPackets().get(0).getPayload());
            Assert.assertEquals("{\"text\":\"gzip\"}", loaded.getChats().get(0).getComponentJson());
            Assert.assertEquals(Collections.singletonList("Line"), loaded.getScoreboards().get(0).getLines());
            Assert.assertEquals(64.0D, loaded.getLocalSnapshots().get(0).getY(), 0.0D);
        } finally {
            deleteRecursively(tempDir);
        }
    }

    private static void assertPacketEquals(ReplayPacketFrame expected, ReplayPacketFrame actual) {
        Assert.assertEquals(expected.getTimestampMs(), actual.getTimestampMs());
        Assert.assertEquals(expected.getClassName(), actual.getClassName());
        Assert.assertArrayEquals(expected.getPayload(), actual.getPayload());
    }

    private static byte[] repeatedPayload(int length, int value) {
        byte[] payload = new byte[length];
        Arrays.fill(payload, (byte) value);
        return payload;
    }

    private static ReplayMetadata buildMetadata() {
        ReplayMetadata metadata = new ReplayMetadata();
        metadata.setMap("Picnic");
        metadata.setMode("BEDWARS_TWO_FOUR");
        metadata.setServerName("mini74CS");
        metadata.setGameType("bedwars");
        metadata.setStartedAt(1_772_818_863_383L);
        metadata.setEndedAt(1_772_818_962_191L);
        metadata.setDurationMs(98_776);
        metadata.setViewerName("Roxiun");
        metadata.setRecordedPlayerEntityId(Integer.valueOf(77));
        metadata.setRecordedPlayerUuid(
            UUID.fromString("11111111-2222-3333-4444-555555555555")
        );
        metadata.setRecordedPlayerName("Recorder");
        metadata.setFormatVersion(1);
        return metadata;
    }

    private static void deleteRecursively(File file) {
        if (file == null || !file.exists()) {
            return;
        }
        if (file.isDirectory()) {
            File[] children = file.listFiles();
            if (children != null) {
                for (File child : children) {
                    deleteRecursively(child);
                }
            }
        }
        file.delete();
    }

    private static void assertStartsWithXzHeader(File file) throws IOException {
        byte[] bytes = Files.readAllBytes(file.toPath());
        Assert.assertTrue(bytes.length >= XZ_HEADER.length);
        for (int i = 0; i < XZ_HEADER.length; i++) {
            Assert.assertEquals(XZ_HEADER[i], bytes[i]);
        }
    }
}
