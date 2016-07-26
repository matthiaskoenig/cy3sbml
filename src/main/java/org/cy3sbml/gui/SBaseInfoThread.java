package org.cy3sbml.gui;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import javax.xml.stream.XMLStreamException;

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
    			try {
					infoFac.createInfo();
				} catch (XMLStreamException e) {
					logger.error("Creating info for object failed");
					e.printStackTrace();
				}
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
     * Creates SBase information and
	 * stores in cache.
	 */
	public static void preloadAnnotationsForSBMLDocument(SBMLDocument document){
		Model model = document.getModel();
		logger.debug("Preload Miriam for <compartments>");
		preloadAnnotationForListOf(model.getListOfCompartments());
		logger.debug("Preload Miriam for <species>");
		preloadAnnotationForListOf(model.getListOfSpecies());
		logger.debug("Preload Miriam for <reactions>");
		preloadAnnotationForListOf(model.getListOfReactions());
		
		QualModelPlugin qModel = (QualModelPlugin) model.getExtension(QualConstants.namespaceURI);
		if (qModel != null){
			logger.debug("Preload Miriam for <qualitativeSpecies>");
			preloadAnnotationForListOf(qModel.getListOfQualitativeSpecies());
			logger.debug("Preload Miriam for <transitions>");
			preloadAnnotationForListOf(qModel.getListOfTransitions());
		}
	}
	
	private static void preloadAnnotationForListOf(@SuppressWarnings("rawtypes") ListOf list){
		Set<Object> nsbSet = new HashSet<>();
		for (Object nsb: list){
			nsbSet.add(nsb);
		}
		SBaseInfoThread thread = new SBaseInfoThread(nsbSet, null);
		thread.start();
	}
}