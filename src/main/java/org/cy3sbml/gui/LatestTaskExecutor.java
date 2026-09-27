package org.cy3sbml.gui;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Runs render tasks one at a time on a single daemon thread, keeping only the most
 * recently submitted task alive, and coalescing a resubmission of the task that is
 * already pending or running.
 * <p>
 * Each task is submitted under a key (compared by reference identity). Submitting a
 * task under the same key as the one currently pending or running is a no-op: that task
 * keeps running undisturbed, so a slow render is not cancelled by a resubmission of the
 * very same thing. Submitting a task under a different key - or when the current one has
 * already finished - cancels (interrupts) whatever is pending or running first, so a
 * slow, superseded render (e.g. one still waiting on a web-service call) never posts
 * stale information after a newer selection has already been requested. A caller for
 * whom "the same as what is already running" never applies (e.g. a help/examples page,
 * which should always replace whatever is being rendered) passes a fresh key (e.g.
 * {@code new Object()}) on every call.
 * <p>
 * Every task submitted here must run to completion (or bail out early on interruption) as
 * a single, self-contained unit of work: a task must never itself call {@link #submit}
 * for a "nested" continuation of its own work. Since submit cancels whatever task is
 * currently current, a task submitting from inside itself would race with, and could
 * cancel, a different, newer task that a caller (e.g. the EDT) already submitted in the
 * meantime - cancelling the wrong task instead of its own. Callers with a multi-step
 * render (e.g. WebViewPanel's PanelUpdater followed by SBase HTML generation) must run
 * every step as a plain, inline call within the one task they submitted.
 * <p>
 * Interruption alone cannot keep a superseded task from publishing: a task may pass its
 * last interrupt check, get superseded, and only then post its result. Every accepted
 * submission therefore gets a new, monotonically increasing generation, and a task
 * publishes its result through {@link #publishIfCurrent}, which runs the publication only
 * while the calling task's generation is still the latest one. The check and the
 * publication happen under the same lock as {@link #submit}, so no newer submission can
 * slip in between them. A publication must therefore be short and non-blocking (e.g.
 * enqueueing onto the JavaFX application thread, whose FIFO order then preserves the
 * order of the accepted publications).
 */
public final class LatestTaskExecutor implements AutoCloseable {
    private static final Logger logger = LoggerFactory.getLogger(LatestTaskExecutor.class);
    private static final String THREAD_NAME = "cy3sbml-info-renderer";

    private final ExecutorService executor;
    private Object currentKey;
    private Future<?> currentTask;
    private long generation = 0;
    private boolean closed = false;
    /** Generation of the task running on the executor thread; unset on any other thread. */
    private final ThreadLocal<Long> runningGeneration = new ThreadLocal<>();

    public LatestTaskExecutor() {
        this.executor = Executors.newSingleThreadExecutor(runnable -> {
            Thread thread = new Thread(runnable, THREAD_NAME);
            thread.setDaemon(true);
            return thread;
        });
    }

    /**
     * Submits {@code task} under {@code key}.
     * <p>
     * If the currently pending/running task's key is the same reference as {@code key},
     * this is a no-op: that task is left running, {@code task} is discarded. Otherwise
     * the current task (if any) is cancelled and {@code task} is submitted as the new
     * current one.
     * <p>
     * A no-op (with a debug log) once {@link #close()} has been called: {@code close()}
     * runs from {@code CyActivator.shutDown} on OSGi bundle stop, which can race a
     * Cytoscape event still arriving on the EDT, and that caller must not have to handle a
     * rejection.
     */
    @SuppressWarnings("ReferenceEquality") // identity, not value equality, is the intended comparison here
    public synchronized void submit(Object key, Runnable task) {
        if (closed) {
            logger.debug("submit() called after close(); ignoring task");
            return;
        }
        if (currentTask != null && !currentTask.isDone() && key == currentKey) {
            logger.debug("Task for {} is already pending/running; not resubmitting", key);
            return;
        }
        if (currentTask != null) {
            currentTask.cancel(true);
        }
        currentKey = key;
        long taskGeneration = ++generation;
        currentTask = executor.submit(() -> {
            runningGeneration.set(taskGeneration);
            try {
                task.run();
            } catch (RuntimeException e) {
                // Logged with its stack trace rather than left to the Future nobody
                // calls get() on: an uncaught exception here must not go unnoticed. The
                // Future is marked done regardless (whether it completes normally or by
                // throwing), so a later submit() is never stuck skipping because of a
                // task that failed.
                logger.error("Uncaught exception from a render task for {}", key, e);
            } finally {
                runningGeneration.remove();
            }
        });
    }

    /**
     * Runs {@code publication} if, and only if, the calling thread is running a task of
     * this executor that is still the current one: no task under a different key (and no
     * later task under an earlier key) has been submitted since, and the executor is not
     * closed. The check and the publication run atomically with respect to
     * {@link #submit}, so a publication that goes through is never followed by one of an
     * older task. Called from any other thread, this does nothing.
     *
     * @param publication short, non-blocking action that posts the task's result
     * @return whether {@code publication} ran
     */
    public synchronized boolean publishIfCurrent(Runnable publication) {
        Long taskGeneration = runningGeneration.get();
        if (closed || taskGeneration == null || taskGeneration != generation) {
            logger.debug("Dropping the publication of a superseded render task");
            return false;
        }
        publication.run();
        return true;
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
