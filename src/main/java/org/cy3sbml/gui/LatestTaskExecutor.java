package org.cy3sbml.gui;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * Runs render tasks one at a time on a single daemon thread, keeping only the most
 * recently submitted task alive.
 * <p>
 * Submitting a new task cancels (interrupts) whatever task is currently pending or
 * running, so a slow, superseded render (e.g. one still waiting on a web service call)
 * never posts stale information after a newer selection has already been requested.
 */
public final class LatestTaskExecutor implements AutoCloseable {
    private static final String THREAD_NAME = "cy3sbml-info-renderer";

    private final ExecutorService executor;
    private Future<?> currentTask;

    public LatestTaskExecutor() {
        this.executor = Executors.newSingleThreadExecutor(runnable -> {
            Thread thread = new Thread(runnable, THREAD_NAME);
            thread.setDaemon(true);
            return thread;
        });
    }

    /**
     * Cancels the pending/running previous task, then submits {@code task} to run next.
     */
    public synchronized void submit(Runnable task) {
        if (currentTask != null) {
            currentTask.cancel(true);
        }
        currentTask = executor.submit(task);
    }

    /**
     * Stops the current task and shuts down the executor thread; no further tasks
     * submitted after this run.
     */
    @Override
    public void close() {
        executor.shutdownNow();
    }
}
