package org.cy3sbml.actions;

import org.cytoscape.work.TaskFactory;
import org.cytoscape.work.TaskIterator;

/**
 * The enable state of an action, as a task factory without tasks: the action is enabled
 * while the factory is ready (see {@code AbstractCyAction} with an enable task factory).
 */
public class SBMLEnableTaskFactory implements TaskFactory {

    // set on the thread of the Cytoscape events, read on the Swing event dispatch thread
    private volatile boolean ready;

    /** Sets whether the action is enabled. */
    public void setReady(boolean ready) {
        this.ready = ready;
    }

    @Override
    public TaskIterator createTaskIterator() {
        return null;
    }

    @Override
    public boolean isReady() {
        return ready;
    }
}
