package org.cy3sbml.gui;

import java.util.Set;


public interface SBMLPanel {
    /** Set text. */
    void setText(String text);

    /**
     * Update Text in the navigation panel.
     * Only updates information if the current thread is the last requested thread
     * for updating text.
     */
    void setText(SBaseHTMLThread infoThread);


    /**
     * Display SBase information
     */
    void showSBaseInfo(Object obj);

    /**
     * Display information for set of nodes.
     */
    void showSBaseInfo(Set<Object> objSet);

}
