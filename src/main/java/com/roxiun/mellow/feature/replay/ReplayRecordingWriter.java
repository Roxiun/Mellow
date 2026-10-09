package com.roxiun.mellow.feature.replay;

import java.io.IOException;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;

/** Ordered disk work on the existing replay worker; capture never waits for disk. */
final class ReplayRecordingWriter {
    private static final int MAX_PENDING_WRITES = 4096;

    interface Factory { ReplayRecordingSpool create() throws IOException; }
    interface Write { void run(ReplayRecordingSpool spool) throws IOException; }

    private final Executor executor;
    private final AtomicInteger pending = new AtomicInteger();
    private volatile IOException failure;
    private ReplayRecordingSpool spool; // Only accessed by the replay worker.
    private boolean finished; // Submissions are serialized by ReplayManager.

    ReplayRecordingWriter(Executor executor, Factory factory) {
        this.executor = executor;
        write(ignored -> spool = factory.create());
    }

    void write(Write action) {
        if (finished || failure != null) return;
        if (pending.incrementAndGet() > MAX_PENDING_WRITES) {
            pending.decrementAndGet();
            failure = new IOException("Replay disk writer could not keep up.");
            return;
        }
        executor.execute(() -> {
            try {
                if (failure == null) action.run(spool);
            } catch (Exception e) {
                failure = e instanceof IOException ? (IOException) e : new IOException(e);
            } finally {
                pending.decrementAndGet();
            }
        });
    }

    IOException getFailure() { return failure; }

    // Runs after all accepted writes, including when recording failed. Never saves a partial replay.
    void finish(BiConsumer<ReplayRecordingSpool, IOException> completion) {
        if (finished) return;
        finished = true;
        executor.execute(() -> {
            try {
                completion.accept(spool, failure);
            } finally {
                if (spool != null) spool.discard();
            }
        });
    }
}
