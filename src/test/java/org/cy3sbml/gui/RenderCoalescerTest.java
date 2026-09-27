package org.cy3sbml.gui;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class RenderCoalescerTest {
    @Test
    void notRedundantInitially() {
        var coalescer = new RenderCoalescer();
        assertFalse(coalescer.isRedundant("a"));
    }

    @Test
    void redundantWhilePending() {
        var coalescer = new RenderCoalescer();
        Object target = new Object();
        coalescer.markPending(target);
        assertTrue(coalescer.isRedundant(target));
    }

    @Test
    void redundantOnceCompleted() {
        var coalescer = new RenderCoalescer();
        Object target = new Object();
        coalescer.markCompleted(target);
        assertTrue(coalescer.isRedundant(target));
    }

    @Test
    void notRedundantForADifferentReferenceEvenIfEqual() {
        var coalescer = new RenderCoalescer();
        // two distinct instances that are .equals() (a record's generated equals
        // compares components) but not the same reference
        coalescer.markCompleted(new Value("x"));
        assertFalse(coalescer.isRedundant(new Value("x")));
    }

    @Test
    void markingAnotherTargetPendingDoesNotClearTheEarlierCompletedTarget() {
        var coalescer = new RenderCoalescer();
        Object completed = new Object();
        Object pending = new Object();
        coalescer.markCompleted(completed);
        coalescer.markPending(pending);
        assertTrue(coalescer.isRedundant(completed));
        assertTrue(coalescer.isRedundant(pending));
    }

    @Test
    void resetClearsBothPendingAndCompleted() {
        var coalescer = new RenderCoalescer();
        Object pending = new Object();
        Object completed = new Object();
        coalescer.markPending(pending);
        coalescer.markCompleted(completed);

        coalescer.reset();

        assertFalse(coalescer.isRedundant(pending));
        assertFalse(coalescer.isRedundant(completed));
    }

    @Test
    void resetThenRunResetsBeforeRunningTheAction() {
        var coalescer = new RenderCoalescer();
        Object target = new Object();
        coalescer.markCompleted(target);

        boolean[] wasRedundantDuringAction = new boolean[1];
        coalescer.resetThenRun(() -> wasRedundantDuringAction[0] = coalescer.isRedundant(target));

        assertFalse(wasRedundantDuringAction[0]);
        assertFalse(coalescer.isRedundant(target));
    }

    @Test
    void resetThenRunRunsTheActionEvenWithNothingToReset() {
        var coalescer = new RenderCoalescer();
        boolean[] ran = new boolean[1];

        coalescer.resetThenRun(() -> ran[0] = true);

        assertTrue(ran[0]);
    }

    private record Value(String v) {}
}
