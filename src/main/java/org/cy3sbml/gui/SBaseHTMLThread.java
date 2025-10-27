package org.cy3sbml.gui;


import java.io.IOException;
import java.util.Collection;

import lombok.Getter;

/**
 * Creates SBase HTML information in separate thread.
 * Provides some helper functions to preload information for given SBMLDocuments.
 */
public class SBaseHTMLThread extends Thread {
    private Collection<Object> objSet;
    private InfoPanel panel;

    @Getter
    private String info;

    /**
     * Constructor.
     */
    public SBaseHTMLThread(Collection<Object> objSet, InfoPanel panel) {
        this.objSet = objSet;
        this.panel = panel;
        this.info = null;
    }

    /**
     * Creates information for all objects within a single thread.
     */
    public void run() {

        for (Object obj : objSet) {
            SBaseHTMLFactory infoFac = new SBaseHTMLFactory(obj);

            try {
                infoFac.createInfo();
            } catch (IOException e) {

            }

            String html = infoFac.getHtml();
            if (info == null) {
                info = html;
            } else {
                info += html;
            }
        }
        // Display if a panel is provided
        if (panel != null) {
            panel.setText(this);
        }
    }
}