package org.cy3sbml.gui;

/**
 * Coalesces consecutive render requests for the same target into one.
 * <p>
 * Loading a model fires several Cytoscape events in a row (network added,
 * current network set, network view added, ...), each of which asks
 * {@link WebViewPanel} to update. Several of these events end up resolving
 * to the very same thing to display (e.g. the same {@code SBMLDocument},
 * shared by a model's subnetworks, while none of them has a node selected
 * yet). Comparing by reference lets the submitter ({@code WebViewPanel.
 * updateInformation}) skip submitting a render at all for a repeat of the
 * same target, so one load renders once.
 * <p>
 * Two references are tracked, both compared by identity, and deliberately kept
 * separate: {@link #markPending} records the target of the render about to be
 * submitted, before it is submitted; {@link #markCompleted} records the target of a
 * render that actually finished (its content was shown), which only the render itself
 * may call, and only once it knows it was not cancelled partway through. A submitter
 * must treat a target as redundant, via {@link #isRedundant}, if it matches either: the
 * pending one, because a render for it is already in flight and must not be duplicated
 * (submitting a second one would cancel the first, per {@code LatestTaskExecutor}, and
 * if the second is then coalesced away too - because {@code isRedundant} would otherwise
 * see the same target already "accepted" - nothing ever gets rendered); or the completed
 * one, because it is already showing. Keeping "pending" and "completed" apart, rather
 * than accepting a target as soon as its render starts, is what makes a cancelled render
 * leave the last completed target unchanged: a later request for the same target it
 * failed to show is not wrongly treated as "already done".
 */
public final class RenderCoalescer {
    private static final Object NONE = new Object();

    private Object pendingTarget = NONE;
    private Object completedTarget = NONE;

    /**
     * Returns true if a render for this target is already pending or running, or is
     * already showing (the last one that actually completed) - either way, a new render
     * for it must not be submitted.
     */
    @SuppressWarnings("ReferenceEquality") // identity, not value equality, is the intended comparison here
    public synchronized boolean isRedundant(Object target) {
        return target == pendingTarget || target == completedTarget;
    }

    /** Records the target of the render about to be submitted. */
    public synchronized void markPending(Object target) {
        pendingTarget = target;
    }

    /**
     * Records the target of a render that actually finished (its content was shown).
     * Must be called only once the render is known not to have been cancelled partway
     * through; a cancelled render must not call this, so the target it failed to show
     * stays eligible for a later request.
     */
    public synchronized void markCompleted(Object target) {
        completedTarget = target;
    }

    /** Forgets both the pending and the completed target. */
    public synchronized void reset() {
        pendingTarget = NONE;
        completedTarget = NONE;
    }

    /**
     * Runs {@code loadAction} after first forgetting both the pending and the completed
     * target, since {@code loadAction} is about to load something into the browser that
     * is not a tracked render (a help/examples page, an error page, ...): neither the
     * pending nor the completed target still describes what is now shown or in flight.
     * <p>
     * This is the single place that invalidation happens, so that a caller loading
     * untracked content into the browser never has to remember to do it itself.
     */
    public synchronized void resetThenRun(Runnable loadAction) {
        reset();
        loadAction.run();
    }
}
