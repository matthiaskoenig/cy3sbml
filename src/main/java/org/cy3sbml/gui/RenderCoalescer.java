package org.cy3sbml.gui;

/**
 * Coalesces consecutive render requests for the same target into one.
 * <p>
 * Loading a model fires several Cytoscape events in a row (network added,
 * current network set, network view added, ...), each of which asks
 * {@link WebViewPanel} to update. Several of these events end up resolving
 * to the very same thing to display (e.g. the same {@code SBMLDocument},
 * shared by a model's subnetworks, while none of them has a node selected
 * yet). Comparing by reference against the last accepted target lets
 * {@link PanelUpdater} skip re-rendering (and re-running the OLS/UniProt/ChEBI
 * lookups) for a repeat of the same target, so one load renders once.
 */
public final class RenderCoalescer {
    private static final Object NONE = new Object();

    private Object lastTarget = NONE;

    /**
     * Returns true (and records {@code target} as the new last target) if it
     * differs, by reference, from the last accepted target. Returns false,
     * leaving the last target unchanged, for a repeat of the same reference.
     */
    public synchronized boolean accept(Object target) {
        if (target == lastTarget) {
            return false;
        }
        lastTarget = target;
        return true;
    }

    /** Forgets the last accepted target, so the next {@link #accept} always succeeds. */
    public synchronized void reset() {
        lastTarget = NONE;
    }
}
