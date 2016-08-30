package org.cy3sbml.actions;

import org.cytoscape.work.TaskFactory;
import org.cytoscape.work.TaskIterator;

/**
 * Use a TaskFactory to set the ready state.
 */
public class ValidationEnableTaskFactory implements TaskFactory {

    private boolean ready;

    public ValidationEnableTaskFactory(){
        ready = false;
    }

    public void setReady(boolean ready){
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
