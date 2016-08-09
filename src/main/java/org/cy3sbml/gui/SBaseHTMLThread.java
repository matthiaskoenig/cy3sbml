package org.cy3sbml.gui;


import java.util.Collection;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Creates SBase HTML information in separate thread.
 * Provides some helper functions to preload information for given SBMLDocuments.
 */
public class SBaseHTMLThread extends Thread{
	private static final Logger logger = LoggerFactory.getLogger(SBaseHTMLThread.class);
	private Collection<Object> objSet;
	private SBMLPanel panel;
    private String info;

    /** Constructor. */
    public SBaseHTMLThread(Collection<Object> objSet, SBMLPanel panel) {
        this.objSet = objSet;
        this.panel = panel;
        this.info = null;
    }

    /**
     * Creates information for all objects within a single thread.
     */
    public void run() {
        for (Object obj : objSet){
            SBaseHTMLFactory infoFac = new SBaseHTMLFactory(obj);
            infoFac.createInfo();
            String html = infoFac.getHtml();
            if (info == null) {
                info = html;
            } else {
                info +=html;
            }
        }
        // Display if a panel is provided
    	if (panel != null){
			panel.setText(this);
		}
    }

    /**
     * Get the created information.
     */
    public String getInfo() {
        return info;
    }

}