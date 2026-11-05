package com.roxiun.mellow.feature.replay;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.Test;
import static org.junit.Assert.*;

public class ReplayRecordingWriterTest {
    private static final class Worker implements Executor {
        final Deque<Runnable> tasks = new ArrayDeque<>();
        public void execute(Runnable task) { tasks.addLast(task); }
        void drain() { while (!tasks.isEmpty()) tasks.removeFirst().run(); }
    }

    @Test
    public void defersDiskWorkAndFinishesAfterOrderedWrites() throws Exception {
        File root = Files.createTempDirectory("replay-writer").toFile();
        Worker worker = new Worker();
        AtomicBoolean created = new AtomicBoolean();
        AtomicBoolean completed = new AtomicBoolean();
        ReplayRecordingWriter writer = new ReplayRecordingWriter(worker, () -> {
            created.set(true);
            return new ReplayRecordingSpool(root);
        });
        writer.write(spool -> spool.appendPacket(new ReplayPacketFrame(10, "first", new byte[]{1})));
        writer.write(spool -> spool.appendPacket(new ReplayPacketFrame(20, "second", new byte[]{2})));
        writer.finish((spool, failure) -> {
            assertNull(failure);
            assertEquals(2, spool.getPacketCount());
            assertEquals("first", spool.getPacketTypes().get(0));
            assertEquals("second", spool.getPacketTypes().get(1));
            completed.set(true);
        });
        writer.write(spool -> fail("Write accepted after finish"));
        assertFalse(created.get());
        assertFalse(completed.get());
        worker.drain();
        assertTrue(completed.get());
        assertEquals(0, new File(root, ".tmp").list().length);
        new File(root, ".tmp").delete();
        root.delete();
    }

    @Test
    public void propagatesDiskFailureAndSkipsRemainingWrites() {
        Worker worker = new Worker();
        IOException expected = new IOException("disk unavailable");
        ReplayRecordingWriter writer = new ReplayRecordingWriter(worker, () -> { throw expected; });
        writer.write(spool -> fail("Write ran after failure"));
        AtomicBoolean completed = new AtomicBoolean();
        writer.finish((spool, failure) -> {
            assertSame(expected, failure);
            assertNull(spool);
            completed.set(true);
        });
        worker.drain();
        assertSame(expected, writer.getFailure());
        assertTrue(completed.get());
    }

    @Test
    public void slowDiskFailsWithBoundedBacklogWithoutBlockingCapture() {
        Worker worker = new Worker();
        ReplayRecordingWriter writer = new ReplayRecordingWriter(worker, () -> {
            fail("Failed recording should not create files");
            return null;
        });
        for (int i = 0; i < 10000; i++) writer.write(spool -> fail("Failed recording wrote data"));
        assertNotNull(writer.getFailure());
        assertTrue(worker.tasks.size() <= 4096);
        AtomicBoolean completed = new AtomicBoolean();
        writer.finish((spool, failure) -> {
            assertNotNull(failure);
            completed.set(true);
        });
        worker.drain();
        assertTrue(completed.get());
    }
}
