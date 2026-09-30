package org.cy3sbml.gui;

import java.util.Set;

/** The panel that shows the SBML information, see {@link WebViewPanel}. */
public interface InfoPanel {
    /**
     * Shows the HTML text.
     */
    void setText(String text);

    /**
     * Shows the information of the SBase.
     */
    void showSBaseInfo(Object obj);

    /**
     * Shows the information of the SBases.
     */
    void showSBaseInfo(Set<Object> objSet);
}
