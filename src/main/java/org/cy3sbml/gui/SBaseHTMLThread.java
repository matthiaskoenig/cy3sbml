package org.cy3sbml.gui;

import java.io.IOException;
import java.util.Collection;
import org.sbml.jsbml.SBase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Builds the HTML information for a set of SBase objects, via web-service lookups
 * (OLS, UniProt, ChEBI), and posts it to a panel.
 * <p>
 * This is a plain helper, not a {@link Runnable} or a {@link Thread}: it is meant to be
 * run inline, as a single step of whichever thread is already carrying out a render (see
 * {@code WebViewPanel.showSBaseInfo}), never started or submitted to an executor as a
 * second, independent unit of work. Doing so would defeat the "one submit per selection"
 * invariant that {@code LatestTaskExecutor} relies on to cancel a superseded render
 * correctly, letting a stale caller cancel a newer, unrelated one.
 */
public class SBaseHTMLThread {
    private static final Logger logger = LoggerFactory.getLogger(SBaseHTMLThread.class);
    private final Collection<Object> objSet;
    private final InfoPanel panel;
    private final SBaseHTMLFactory htmlFactory;
    private String info;

    /**
     * Constructor.
     */
    public SBaseHTMLThread(Collection<Object> objSet, InfoPanel panel, SBaseHTMLFactory htmlFactory) {
        this.objSet = objSet;
        this.panel = panel;
        this.htmlFactory = htmlFactory;
        this.info = null;
    }

    /**
     * Creates information for all objects within a single thread, and posts it to the
     * panel.
     * <p>
     * Web-service lookups made while building the HTML (OLS, UniProt, ChEBI) restore the
     * thread's interrupt flag on {@code InterruptedException} rather than throwing it, so
     * this checks {@code Thread.currentThread().isInterrupted()} between SBase objects and
     * again before posting to the panel; a cancelled render then stops promptly and never
     * overwrites the HTML of a newer, still-running render.
     *
     * @return true if the HTML was actually posted to the panel; false if the render was
     *     cancelled (interrupted) partway through, so nothing was posted.
     */
    public boolean render() {

        for (Object obj : objSet) {
            if (Thread.currentThread().isInterrupted()) {
                return false;
            }
            String html;
            try {
                html = htmlFactory.createInfo((SBase) obj);
            } catch (IOException e) {
                logger.error("Could not create the information for: " + obj, e);
                continue;
            }

            if (info == null) {
                info = html;
            } else {
                info += html;
            }
        }
        if (Thread.currentThread().isInterrupted()) {
            return false;
        }
        // Display if a panel is provided and there was something to show; an empty
        // object set must not blank out whatever the panel is currently displaying.
        if (panel != null && !objSet.isEmpty()) {
            panel.setText(info);
        }
        return true;
    }

    /**
     * Get the created information.
     */
    public String getInfo() {
        return info;
    }
}
