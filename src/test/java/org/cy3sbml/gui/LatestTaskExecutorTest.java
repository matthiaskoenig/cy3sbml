package org.cy3sbml.gui;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.Test;

class LatestTaskExecutorTest {

    @Test
    void latestSelectionWins() throws InterruptedException {
        List<Integer> recorded = new CopyOnWriteArrayList<>();
        CountDownLatch lastTaskDone = new CountDownLatch(1);
        int lastIndex = 9;

        try (LatestTaskExecutor executor = new LatestTaskExecutor()) {
            for (int i = 0; i < 10; i++) {
                int index = i;
                executor.submit(() -> {
                    try {
                        Thread.sleep(50);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        return;
                    }
                    recorded.add(index);
                    if (index == lastIndex) {
                        lastTaskDone.countDown();
                    }
                });
            }

            assertTrue(lastTaskDone.await(5, TimeUnit.SECONDS), "the last submitted task should run to completion");
        }

        assertTrue(recorded.size() < 10, "superseded tasks should have been cancelled: " + recorded);
        assertEquals(lastIndex, recorded.get(recorded.size() - 1));
    }

    /**
     * Reproduces the real WebViewPanel pattern: a render (task A) is still in flight when
     * a newer selection (task B) is submitted from the "EDT". A must never be able to
     * cancel B, since A only ever continues its own work as a plain inline call (never a
     * nested {@code submit}); once A notices it was interrupted, it must stop without
     * recording a result, and B must still run to completion and be the last result.
     */
    @Test
    void newerSubmissionWinsOverStillRunningOlderTask() throws InterruptedException {
        List<String> recorded = new CopyOnWriteArrayList<>();
        CountDownLatch aBlocked = new CountDownLatch(1);
        CountDownLatch releaseA = new CountDownLatch(1);
        CountDownLatch bDone = new CountDownLatch(1);

        try (LatestTaskExecutor executor = new LatestTaskExecutor()) {
            executor.submit(() -> {
                aBlocked.countDown();
                awaitUninterruptibly(releaseA);
                // The "nested" continuation of A's own work (what WebViewPanel's
                // showSBaseInfo does inline after PanelUpdater's lookups): a plain call,
                // never executor.submit(...). It must respect interruption.
                if (!Thread.currentThread().isInterrupted()) {
                    recorded.add("A");
                }
            });
            assertTrue(aBlocked.await(5, TimeUnit.SECONDS), "task A should have started");

            // The EDT selects something newer while A is still blocked/running.
            executor.submit(() -> {
                recorded.add("B");
                bDone.countDown();
            });

            releaseA.countDown();
            assertTrue(bDone.await(5, TimeUnit.SECONDS), "B must still run even though A was still in flight");
        }

        assertEquals(List.of("B"), recorded, "A must have noticed the interrupt and never recorded a stale result");
    }

    /**
     * Variant of {@link #newerSubmissionWinsOverStillRunningOlderTask} that removes A's
     * own interrupt check, isolating the structural guarantee: {@code submit} never lets
     * a task cancel anything other than the exact task that was current when it was
     * called. Because A's continuation is a plain inline call rather than a nested
     * {@code submit}, it can never touch which task is "current" - so B (already
     * current at the time it was submitted) is guaranteed to run, regardless of whether A
     * itself is diligent about checking interruption. Both A and B run, strictly in
     * submission order, on the single render thread.
     */
    @Test
    void nestedInlineContinuationCanNeverCancelANewerSubmission() throws InterruptedException {
        List<String> recorded = new CopyOnWriteArrayList<>();
        CountDownLatch aBlocked = new CountDownLatch(1);
        CountDownLatch releaseA = new CountDownLatch(1);
        CountDownLatch bDone = new CountDownLatch(1);

        try (LatestTaskExecutor executor = new LatestTaskExecutor()) {
            executor.submit(() -> {
                aBlocked.countDown();
                awaitUninterruptibly(releaseA);
                // Deliberately ignores its own interrupted flag: a plain inline call
                // that is never itself a nested submit(), so it has no way to reach
                // (let alone cancel) B's already-submitted task.
                recorded.add("A");
            });
            assertTrue(aBlocked.await(5, TimeUnit.SECONDS), "task A should have started");

            executor.submit(() -> {
                recorded.add("B");
                bDone.countDown();
            });

            releaseA.countDown();
            assertTrue(bDone.await(5, TimeUnit.SECONDS), "B must run to completion");
        }

        assertEquals(List.of("A", "B"), recorded, "both run, strictly in submission order; B was never cancelled");
    }

    @Test
    void closeStopsExecutor() throws InterruptedException {
        LatestTaskExecutor executor = new LatestTaskExecutor();
        CountDownLatch started = new CountDownLatch(1);
        CountDownLatch interrupted = new CountDownLatch(1);

        executor.submit(() -> {
            started.countDown();
            try {
                Thread.sleep(TimeUnit.SECONDS.toMillis(30));
            } catch (InterruptedException e) {
                interrupted.countDown();
            }
        });

        assertTrue(started.await(5, TimeUnit.SECONDS), "task should have started");
        executor.close();

        assertTrue(interrupted.await(5, TimeUnit.SECONDS), "the running task should be interrupted on close");
    }

    @Test
    void submitAfterCloseIsNoOp() {
        LatestTaskExecutor executor = new LatestTaskExecutor();
        executor.close();

        AtomicBoolean ran = new AtomicBoolean(false);
        // Simulates a Cytoscape event still arriving on the EDT after CyActivator.shutDown
        // has already closed the executor: submit() must not throw.
        assertDoesNotThrow(() -> executor.submit(() -> ran.set(true)));
        assertFalse(ran.get(), "a task submitted after close() must never run");
    }

    private static void awaitUninterruptibly(CountDownLatch latch) {
        boolean interrupted = false;
        while (true) {
            try {
                latch.await();
                break;
            } catch (InterruptedException e) {
                interrupted = true;
            }
        }
        if (interrupted) {
            Thread.currentThread().interrupt();
        }
    }
}
