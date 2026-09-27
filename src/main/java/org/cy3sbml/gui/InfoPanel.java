package org.cy3sbml.gui;

import java.util.Set;

public interface InfoPanel {
    /**
     * Set text.
     */
    void setText(String text);

    /**
     * Display SBase information
     */
    void showSBaseInfo(Object obj);

    /**
     * Display information for set of nodes.
     */
    void showSBaseInfo(Set<Object> objSet);
}
