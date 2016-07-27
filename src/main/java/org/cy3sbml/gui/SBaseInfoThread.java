package org.cy3sbml.gui;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.sbml.jsbml.ListOf;
import org.sbml.jsbml.Model;
import org.sbml.jsbml.SBMLDocument;
import org.sbml.jsbml.ext.qual.QualConstants;
import org.sbml.jsbml.ext.qual.QualModelPlugin;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Generates information for web resources in separate thread.
 */
public class SBaseInfoThread extends Thread{
	private static final Logger logger = LoggerFactory.getLogger(SBaseInfoThread.class);
	
	Collection<Object> objSet;
	SBMLPanel panel;
	public String info;
	   
    public SBaseInfoThread(Collection<Object> objSet, SBMLPanel panel) {
        this.objSet = objSet;
        this.panel = panel;
        info = "";
    }

    public void run() {
    	if (panel != null){
    		// Info creating mode
    		for (Object obj : objSet){	
    			SBaseInfoFactory infoFac = new SBaseInfoFactory(obj);
				infoFac.createInfo();
    			info += infoFac.getInfo();
    			panel.setText(this);
    		}
    	} else {
    		// Cache filling mode
    		for (Object obj : objSet){
    			SBaseInfoFactory infoFac = new SBaseInfoFactory(obj);
    			infoFac.cacheMiriamInformation();
    		}
    	}
    }
        
	/**
     * Creates SBase information and stores in cache.
	 */
	public static void preloadInfosForSBMLDocument(SBMLDocument document){
		Model model = document.getModel();
		logger.debug("Preload <compartments>");
		preloadInfosForListOf(model.getListOfCompartments());
		logger.debug("Preload <species>");
		preloadInfosForListOf(model.getListOfSpecies());
		logger.debug("Preload <reactions>");
		preloadInfosForListOf(model.getListOfReactions());
		
		QualModelPlugin qModel = (QualModelPlugin) model.getExtension(QualConstants.namespaceURI);
		if (qModel != null){
			logger.debug("Preload <qualitativeSpecies>");
			preloadInfosForListOf(qModel.getListOfQualitativeSpecies());
			logger.debug("Preload <transitions>");
			preloadInfosForListOf(qModel.getListOfTransitions());
		}
	}

	/**
	 * Preload all the information.
     */
	private static void preloadInfosForListOf(@SuppressWarnings("rawtypes") ListOf list){
		Set<Object> nsbSet = new HashSet<>();
		for (Object nsb: list){
			nsbSet.add(nsb);
		}
		SBaseInfoThread thread = new SBaseInfoThread(nsbSet, null);
		thread.start();
	}
}