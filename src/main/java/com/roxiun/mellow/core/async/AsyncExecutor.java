package com.roxiun.mellow.core.async;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public final class AsyncExecutor {

    private static final AsyncExecutor INSTANCE = new AsyncExecutor();

    private final ExecutorService profileIoExecutor = Executors.newFixedThreadPool(
        8,
        namedFactory("Mellow-ProfileIO")
    );
    private final ExecutorService chatExecutor = Executors.newFixedThreadPool(
        4,
        namedFactory("Mellow-Chat")
    );
    private final ExecutorService commandExecutor = Executors.newFixedThreadPool(
        4,
        namedFactory("Mellow-Command")
    );
    private final ExecutorService supplementalIoExecutor = Executors.newFixedThreadPool(
        4,
        namedFactory("Mellow-SupplementalIO")
    );
    private final ExecutorService replayIoExecutor = Executors.newSingleThreadExecutor(
        namedFactory("Mellow-ReplayIO")
    );

    private AsyncExecutor() {}

    public static AsyncExecutor getInstance() {
        return INSTANCE;
    }

    public void profileIo(Runnable task) {
        profileIoExecutor.execute(() -> {
            try { task.run(); }
            catch (Exception e) { org.apache.logging.log4j.LogManager.getLogger("Mellow").error("Background task failed", e); }
        });
    }

    public void chat(Runnable task) {
        chatExecutor.execute(() -> {
            try { task.run(); }
            catch (Exception e) { org.apache.logging.log4j.LogManager.getLogger("Mellow").error("Background task failed", e); }
        });
    }

    public void command(Runnable task) {
        commandExecutor.execute(() -> {
            try { task.run(); }
            catch (Exception e) { org.apache.logging.log4j.LogManager.getLogger("Mellow").error("Background task failed", e); }
        });
    }

    public void supplementalIo(Runnable task) {
        supplementalIoExecutor.execute(() -> {
            try { task.run(); }
            catch (Exception e) { org.apache.logging.log4j.LogManager.getLogger("Mellow").error("Background task failed", e); }
        });
    }

    public void replayIo(Runnable task) {
        replayIoExecutor.execute(() -> {
            try { task.run(); }
            catch (Exception e) { org.apache.logging.log4j.LogManager.getLogger("Mellow").error("Background task failed", e); }
        });
    }

    public void shutdownReplayIoAndAwait() {
        replayIoExecutor.shutdown();
        boolean interrupted = false;
        try {
            while (!replayIoExecutor.awaitTermination(2, TimeUnit.SECONDS)) {}
        } catch (InterruptedException e) {
            interrupted = true;
        } finally {
            if (interrupted) {
                Thread.currentThread().interrupt();
            }
        }
    }

    private static ThreadFactory namedFactory(String prefix) {
        AtomicInteger counter = new AtomicInteger();
        return runnable -> {
            Thread thread = new Thread(
                runnable,
                prefix + "-" + counter.incrementAndGet()
            );
            thread.setDaemon(true);
            return thread;
        };
    }
}
