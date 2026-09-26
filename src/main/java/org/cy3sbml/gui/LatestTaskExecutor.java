package org.cy3sbml.gui;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Runs render tasks one at a time on a single daemon thread, keeping only the most
 * recently submitted task alive.
 * <p>
 * Submitting a new task cancels (interrupts) whatever task is currently pending or
 * running, so a slow, superseded render (e.g. one still waiting on a web service call)
 * never posts stale information after a newer selection has already been requested.
 * <p>
 * Every task submitted here must run to completion (or bail out early on interruption) as
 * a single, self-contained unit of work: a task must never itself call {@link #submit}
 * for a "nested" continuation of its own work. Since submit cancels whatever task is
 * currently current, a task submitting from inside itself would race with, and could
 * cancel, a different, newer task that a caller (e.g. the EDT) already submitted in the
 * meantime - cancelling the wrong task instead of its own. Callers with a multi-step
 * render (e.g. WebViewPanel's PanelUpdater followed by SBase HTML generation) must run
 * every step as a plain, inline call within the one task they submitted.
 */
public final class LatestTaskExecutor implements AutoCloseable {
    private static final Logger logger = LoggerFactory.getLogger(LatestTaskExecutor.class);
    private static final String THREAD_NAME = "cy3sbml-info-renderer";

    private final ExecutorService executor;
    private Future<?> currentTask;
    private boolean closed = false;

    public LatestTaskExecutor() {
        this.executor = Executors.newSingleThreadExecutor(runnable -> {
            Thread thread = new Thread(runnable, THREAD_NAME);
            thread.setDaemon(true);
            return thread;
        });
    }

    /**
     * Cancels the pending/running previous task, then submits {@code task} to run next.
     * <p>
     * A no-op (with a debug log) once {@link #close()} has been called: {@code close()}
     * runs from {@code CyActivator.shutDown} on OSGi bundle stop, which can race a
     * Cytoscape event still arriving on the EDT, and that caller must not have to handle a
     * rejection.
     */
    public synchronized void submit(Runnable task) {
        if (closed) {
            logger.debug("submit() called after close(); ignoring task");
            return;
        }
        if (currentTask != null) {
            currentTask.cancel(true);
        }
        currentTask = executor.submit(task);
    }

    /**
     * Stops the current task (interrupting it if it is running) and shuts down the
     * executor thread. Further {@link #submit} calls are silently ignored.
     */
    @Override
    public synchronized void close() {
        closed = true;
        executor.shutdownNow();
    }
}
