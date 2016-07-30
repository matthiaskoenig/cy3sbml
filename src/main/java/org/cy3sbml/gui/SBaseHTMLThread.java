package org.cy3sbml.gui;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.sbml.jsbml.CVTerm;
import org.sbml.jsbml.ListOf;
import org.sbml.jsbml.Model;
import org.sbml.jsbml.SBMLDocument;
import org.sbml.jsbml.ext.qual.QualConstants;
import org.sbml.jsbml.ext.qual.QualModelPlugin;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Generates information for web resources in separate thread.
 * Provides some helper functions to preload information for given SBMLDocuments.
 */
public class SBaseHTMLThread extends Thread{
	private static final Logger logger = LoggerFactory.getLogger(SBaseHTMLThread.class);
	
	Collection<Object> objSet;
	SBMLPanel panel;

    private String info;
	   
    public SBaseHTMLThread(Collection<Object> objSet, SBMLPanel panel) {
        this.objSet = objSet;
        this.panel = panel;
        info = null;
    }

    /** Get the created information. */
    public String getInfo() {
        return info;
    }

    /**
     * Creates information for all objects within the single thread.
     */
    public void run() {
    	if (panel != null){
    		// Info creating mode, this is mostly called with a single object in the collection
    		for (Object obj : objSet){	
    			SBaseHTMLFactory infoFac = new SBaseHTMLFactory(obj);
				infoFac.createInfo();
				String html = infoFac.getHtml();
				if (info == null) {
                    info = html;
                } else {
                    info +=html;
                }
    			panel.setText(this);
    		}
    	}

    	else {
    	    logger.info("Cache filling mode NOT implemented");
    		// Cache filling mode. Stores costly information in cache.
    		for (Object obj : objSet){
    			SBaseHTMLFactory infoFac = new SBaseHTMLFactory(obj);
    			// infoFac.cacheInformation();
    		}

			/*
			public void cacheInformation(){
				// cache miriam information
				for (CVTerm term : sbase.getCVTerms()){
					for (String rURI : term.getResources()){
						MiriamResource.getLocationsFromURI(rURI);
					}
				}
			}
			 */

    	}
    }
        
	/**
     * Creates SBase information for given Document and stores in cache.
     * Currently only subset of information is cached.
     * TODO: refactor to cache costly information.
	 */
	@Deprecated
	public static void preload(SBMLDocument document){
		Model model = document.getModel();
		logger.debug("Preload <compartments>");
		preloadListOf(model.getListOfCompartments());
		logger.debug("Preload <species>");
		preloadListOf(model.getListOfSpecies());
		logger.debug("Preload <reactions>");
		preloadListOf(model.getListOfReactions());
		
		QualModelPlugin qModel = (QualModelPlugin) model.getExtension(QualConstants.namespaceURI);
		if (qModel != null){
			logger.debug("Preload <qualitativeSpecies>");
			preloadListOf(qModel.getListOfQualitativeSpecies());
			logger.debug("Preload <transitions>");
			preloadListOf(qModel.getListOfTransitions());
		}
	}

	/**
	 * Preload all the information.
     */
	private static void preloadListOf(@SuppressWarnings("rawtypes") ListOf list){
		Set<Object> nsbSet = new HashSet<>();
		for (Object nsb: list){
			nsbSet.add(nsb);
		}
		SBaseHTMLThread thread = new SBaseHTMLThread(nsbSet, null);
		thread.start();
	}
}