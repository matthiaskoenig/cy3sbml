package org.cy3sbml.gui;

import java.util.Set;

public interface InfoPanel {
    /**
     * Set text.
     */
    void setText(String text);

    /**
     * Display SBase information.
     *
     * @return true if the information was actually shown; false if the render was
     *     cancelled (interrupted by a newer, superseding request) partway through.
     */
    boolean showSBaseInfo(Object obj);

    /**
     * Display information for a set of nodes.
     *
     * @return true if the information was actually shown; false if the render was
     *     cancelled (interrupted by a newer, superseding request) partway through.
     */
    boolean showSBaseInfo(Set<Object> objSet);
}
