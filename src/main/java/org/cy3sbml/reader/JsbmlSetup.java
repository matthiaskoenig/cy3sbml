package org.cy3sbml.reader;

import org.sbml.jsbml.xml.parsers.ParserManager;

/**
 * One-time setup of JSBML before SBML files are read or written on several threads.
 * <p>
 * JSBML creates its {@link ParserManager} singleton lazily and without synchronization, and
 * the instance shares the iterators it fills its parser maps from. Two threads that read or
 * write their first SBML document at the same time (two imports in Cytoscape, or tests run in
 * parallel) can both create an instance, and one of them fails with a
 * {@code NoSuchElementException} from the shared iterator. Creating the singleton once, before
 * anything else uses JSBML, avoids that.
 */
public final class JsbmlSetup {
    private JsbmlSetup() {}

    /**
     * Creates the JSBML parser manager. Call it on one thread before the SBML reader is used.
     */
    public static void initialize() {
        ParserManager.getManager();
    }
}
