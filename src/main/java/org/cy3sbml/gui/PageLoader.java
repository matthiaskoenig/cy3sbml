package org.cy3sbml.gui;

import java.util.Objects;
import java.util.function.Consumer;

/**
 * Delivers the pages of the cy3sbml panel (rendered HTML text and static resources such
 * as the help page) to the browser.
 * <p>
 * Every request goes to the browser directly on the calling thread, under one lock, so
 * the browser receives the pages in the order they were requested. {@link Browser} hands
 * each page on to the JavaFX thread with {@code Platform.runLater}, which keeps that
 * order, so the page requested last is the page shown.
 * <p>
 * The browser is created later on the JavaFX thread. Until it is attached, the latest
 * request is held and shown on {@link #attach}; earlier ones would only be replaced.
 */
final class PageLoader {

    /** The browser side of the panel. */
    interface Target {
        void loadText(String text);

        void loadPageFromResource(String resource);
    }

    private Target target; // guarded by this
    private Consumer<Target> pending; // guarded by this

    /**
     * Attaches the browser and shows the latest request made before, if any.
     */
    synchronized void attach(Target browser) {
        target = Objects.requireNonNull(browser);
        if (pending != null) {
            Consumer<Target> request = pending;
            pending = null;
            request.accept(target);
        }
    }

    void loadText(String text) {
        load(browser -> browser.loadText(text));
    }

    void loadPageFromResource(String resource) {
        load(browser -> browser.loadPageFromResource(resource));
    }

    private synchronized void load(Consumer<Target> request) {
        if (target == null) {
            pending = request;
        } else {
            request.accept(target);
        }
    }
}
