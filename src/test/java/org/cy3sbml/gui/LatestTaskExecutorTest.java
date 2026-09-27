package org.cy3sbml.gui;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BooleanSupplier;
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
                // a fresh key per submission: each is a genuinely different selection,
                // never coalesced with the previous one
                executor.submit(new Object(), () -> {
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
     * a newer selection (task B, a different key) is submitted from the "EDT". A must
     * never be able to cancel B, since A only ever continues its own work as a plain
     * inline call (never a nested {@code submit}); once A notices it was interrupted, it
     * must stop without recording a result, and B must still run to completion and be the
     * last result.
     */
    @Test
    void newerSubmissionWinsOverStillRunningOlderTask() throws InterruptedException {
        List<String> recorded = new CopyOnWriteArrayList<>();
        CountDownLatch aBlocked = new CountDownLatch(1);
        CountDownLatch releaseA = new CountDownLatch(1);
        CountDownLatch bDone = new CountDownLatch(1);

        try (LatestTaskExecutor executor = new LatestTaskExecutor()) {
            executor.submit(new Object(), () -> {
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
            executor.submit(new Object(), () -> {
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
            executor.submit(new Object(), () -> {
                aBlocked.countDown();
                awaitUninterruptibly(releaseA);
                // Deliberately ignores its own interrupted flag: a plain inline call
                // that is never itself a nested submit(), so it has no way to reach
                // (let alone cancel) B's already-submitted task.
                recorded.add("A");
            });
            assertTrue(aBlocked.await(5, TimeUnit.SECONDS), "task A should have started");

            executor.submit(new Object(), () -> {
                recorded.add("B");
                bDone.countDown();
            });

            releaseA.countDown();
            assertTrue(bDone.await(5, TimeUnit.SECONDS), "B must run to completion");
        }

        assertEquals(List.of("A", "B"), recorded, "both run, strictly in submission order; B was never cancelled");
    }

    /**
     * (a) A resubmission under the same key as the task that is currently pending or
     * running is coalesced away: the running task is left undisturbed and finishes
     * normally.
     */
    @Test
    void resubmittingTheSameKeyWhileRunningIsCoalesced() throws InterruptedException {
        List<String> recorded = new CopyOnWriteArrayList<>();
        CountDownLatch aBlocked = new CountDownLatch(1);
        CountDownLatch releaseA = new CountDownLatch(1);
        Object key = new Object();

        try (LatestTaskExecutor executor = new LatestTaskExecutor()) {
            executor.submit(key, () -> {
                aBlocked.countDown();
                awaitUninterruptibly(releaseA);
                if (!Thread.currentThread().isInterrupted()) {
                    recorded.add("first");
                }
            });
            assertTrue(aBlocked.await(5, TimeUnit.SECONDS), "the first task should have started");

            // same key, while the first task is still running: must be a no-op
            executor.submit(key, () -> recorded.add("second"));

            releaseA.countDown();
            awaitCondition(() -> !recorded.isEmpty());
        }

        assertEquals(List.of("first"), recorded, "the running task for the same key must not be cancelled");
    }

    /**
     * (b) A resubmission under a different key while a task is running cancels it and
     * runs the new one, same as before keys existed.
     */
    @Test
    void submittingADifferentKeyWhileRunningCancelsTheCurrentOne() throws InterruptedException {
        List<String> recorded = new CopyOnWriteArrayList<>();
        CountDownLatch aBlocked = new CountDownLatch(1);
        CountDownLatch bDone = new CountDownLatch(1);

        try (LatestTaskExecutor executor = new LatestTaskExecutor()) {
            executor.submit("A", () -> {
                aBlocked.countDown();
                blockUntilInterrupted();
                recorded.add("A-interrupted");
            });
            assertTrue(aBlocked.await(5, TimeUnit.SECONDS), "A should have started");

            executor.submit("B", () -> {
                recorded.add("B");
                bDone.countDown();
            });

            assertTrue(bDone.await(5, TimeUnit.SECONDS), "B must run after cancelling A");
        }

        assertEquals(List.of("A-interrupted", "B"), recorded, "A must notice the interrupt before B runs");
    }

    /**
     * (c) A -> B -> A: A completes, B is submitted and starts running, then A is
     * resubmitted while B is still running. B is cancelled and A runs again, ending on A.
     * Re-rendering a target that already completed earlier is acceptable.
     */
    @Test
    void resubmittingAnEarlierCompletedKeyCancelsANewerRunningOne() throws InterruptedException {
        List<String> recorded = new CopyOnWriteArrayList<>();
        CountDownLatch aFirstDone = new CountDownLatch(1);
        CountDownLatch bBlocked = new CountDownLatch(1);
        CountDownLatch releaseB = new CountDownLatch(1);
        CountDownLatch aSecondDone = new CountDownLatch(1);

        try (LatestTaskExecutor executor = new LatestTaskExecutor()) {
            executor.submit("A", () -> {
                recorded.add("A1");
                aFirstDone.countDown();
            });
            assertTrue(aFirstDone.await(5, TimeUnit.SECONDS), "A should have completed");

            executor.submit("B", () -> {
                bBlocked.countDown();
                awaitUninterruptibly(releaseB);
                if (Thread.currentThread().isInterrupted()) {
                    recorded.add("B-interrupted");
                }
            });
            // note: releaseB is only ever counted down below, after A is resubmitted,
            // so B is genuinely still running (not finished) when A pre-empts it
            assertTrue(bBlocked.await(5, TimeUnit.SECONDS), "B should have started");

            // A again, while B (a different key) is still running: must cancel B
            executor.submit("A", () -> {
                recorded.add("A2");
                aSecondDone.countDown();
            });
            releaseB.countDown();

            assertTrue(aSecondDone.await(5, TimeUnit.SECONDS), "A must run again and finish");
        }

        assertEquals(List.of("A1", "B-interrupted", "A2"), recorded, "ends on A, B was cancelled mid-render");
    }

    /**
     * (d) A help/examples-style submission under a fresh key always wins over whatever
     * is pending/running, and a later request for the render target it interrupted still
     * renders that target (it is not "stuck" because it was cancelled once).
     */
    @Test
    void aFreshKeySubmissionAlwaysWinsAndALaterSameKeyRequestStillRenders() throws InterruptedException {
        List<String> recorded = new CopyOnWriteArrayList<>();
        CountDownLatch aBlocked = new CountDownLatch(1);
        CountDownLatch helpDone = new CountDownLatch(1);
        CountDownLatch aSecondDone = new CountDownLatch(1);

        try (LatestTaskExecutor executor = new LatestTaskExecutor()) {
            executor.submit("A", () -> {
                aBlocked.countDown();
                blockUntilInterrupted();
                recorded.add("A-interrupted");
            });
            assertTrue(aBlocked.await(5, TimeUnit.SECONDS), "A should have started");

            // help: a fresh key every time, so it is never coalesced with A
            executor.submit(new Object(), () -> {
                recorded.add("help");
                helpDone.countDown();
            });
            assertTrue(helpDone.await(5, TimeUnit.SECONDS), "help must run, cancelling A");

            // a later request for A (a genuinely new submission, since the previous one
            // was cancelled and never completed) must still render
            executor.submit("A", () -> {
                recorded.add("A-again");
                aSecondDone.countDown();
            });
            assertTrue(aSecondDone.await(5, TimeUnit.SECONDS), "A must render again, not be stuck");
        }

        assertEquals(List.of("A-interrupted", "help", "A-again"), recorded);
    }

    /**
     * An uncaught {@code RuntimeException} from a task must not leave the executor
     * thinking that key is still pending/running forever: the task's {@code Future} is
     * done regardless of how it finished, so the very next submission (even for the same
     * key) proceeds normally rather than being wrongly coalesced away.
     */
    @Test
    void anUncaughtExceptionDoesNotLeaveTheKeyStuck() throws InterruptedException {
        CountDownLatch failed = new CountDownLatch(1);
        CountDownLatch secondRan = new CountDownLatch(1);
        Object key = new Object();

        try (LatestTaskExecutor executor = new LatestTaskExecutor()) {
            executor.submit(key, () -> {
                failed.countDown();
                throw new RuntimeException("boom");
            });
            assertTrue(failed.await(5, TimeUnit.SECONDS), "the failing task should have run");

            // Retry submitting under the same key until it actually runs: right after
            // failed.countDown() fires, the failing task's Future may not be marked done
            // yet (it still has to return from the try/catch wrapper), during which a
            // same-key submission would still be correctly coalesced, not stuck.
            long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
            while (secondRan.getCount() > 0 && System.nanoTime() < deadline) {
                executor.submit(key, secondRan::countDown);
                if (secondRan.await(20, TimeUnit.MILLISECONDS)) {
                    break;
                }
            }
            assertTrue(secondRan.await(0, TimeUnit.MILLISECONDS), "a later submission for the same key must still run");
        }
    }

    @Test
    void closeStopsExecutor() throws InterruptedException {
        LatestTaskExecutor executor = new LatestTaskExecutor();
        CountDownLatch started = new CountDownLatch(1);
        CountDownLatch interrupted = new CountDownLatch(1);

        executor.submit(new Object(), () -> {
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
        assertDoesNotThrow(() -> executor.submit(new Object(), () -> ran.set(true)));
        assertFalse(ran.get(), "a task submitted after close() must never run");
    }

    /** Blocks (on a latch nobody ever counts down) until interrupted, then returns. */
    private static void blockUntilInterrupted() {
        try {
            new CountDownLatch(1).await();
        } catch (InterruptedException e) {
            // expected: this is how the task notices it was cancelled
        }
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

    /** Bounded busy-poll for a condition with no dedicated notification hook. */
    private static void awaitCondition(BooleanSupplier condition) throws InterruptedException {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
        while (!condition.getAsBoolean()) {
            if (System.nanoTime() > deadline) {
                fail("condition was never true within the 5s timeout");
            }
            Thread.sleep(5);
        }
    }
}
