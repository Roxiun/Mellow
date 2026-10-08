package com.roxiun.mellow.util.cache;

import org.junit.Test;
import java.util.concurrent.atomic.AtomicLong;
import static org.junit.Assert.*;

public class LookupTrackerTest {
    @Test public void successIsRetainedRegardlessOfElapsedTimeAndNewPlayersStillResolve() {
        AtomicLong clock = new AtomicLong();
        LookupTracker<String> tracker = new LookupTracker<>(clock::get);
        LookupTracker.Attempt first = tracker.begin("alex");
        assertNull(tracker.begin("alex"));
        tracker.finish("alex", first, true);
        clock.set(3_600_000);
        assertNull(tracker.begin("alex"));
        assertNotNull(tracker.begin("sam"));
    }
    @Test public void failuresGetOnlyOneDelayedRetry() {
        AtomicLong clock = new AtomicLong();
        LookupTracker<String> tracker = new LookupTracker<>(clock::get);
        tracker.finish("alex", tracker.begin("alex"), false);
        assertNull(tracker.begin("alex"));
        clock.set(30_000);
        LookupTracker.Attempt retry = tracker.begin("alex");
        assertNotNull(retry);
        tracker.finish("alex", retry, false);
        clock.set(3_600_000);
        assertNull(tracker.begin("alex"));
        assertTrue(tracker.finished("alex"));
    }
    @Test public void invalidatingOnePlayerRejectsTheirOldResponseButPreservesOthers() {
        LookupTracker<String> tracker = new LookupTracker<>();
        LookupTracker.Attempt alex = tracker.begin("alex"), sam = tracker.begin("sam");
        tracker.remove("alex");
        assertFalse(tracker.finish("alex", alex, true));
        assertTrue(tracker.finish("sam", sam, true));
        assertNotNull(tracker.begin("alex"));
        assertNull(tracker.begin("sam"));
        tracker.clear();
        assertNotNull(tracker.begin("sam"));
    }
}
