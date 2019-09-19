package org.cy3sbml.biomodelrest;

import org.cy3sbml.biomodelrest.gui.WebViewSwing;
import org.cy3sbml.gui.GUIConstants;
import org.cytoscape.application.swing.AbstractCyAction;
import org.cytoscape.application.swing.CySwingApplication;
import org.cytoscape.util.swing.OpenBrowser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.event.ActionEvent;

/**
 * Test access to the cy3sbml instance information.
 */
public class BiomodelsRestAction extends AbstractCyAction{
	private static final long serialVersionUID = 1L;
	private static final Logger logger = LoggerFactory.getLogger(BiomodelsRestAction.class);
	private CySwingApplication cySwingApplication;
	private OpenBrowser openBrowser;
	private BiomodelsSBMLReader sbmlReader;
	
	/** 
	 * Constructor. 
	 * Requires functionality for open links in external browser and
	 * for reading SBML into networks.
	 */
	public BiomodelsRestAction(CySwingApplication cySwingApplication, OpenBrowser openBrowser, BiomodelsSBMLReader sbmlReader){
		super("BiomodelsRestAction");
		this.cySwingApplication = cySwingApplication;
		this.openBrowser = openBrowser;
		this.sbmlReader = sbmlReader;
		
		ImageIcon icon = new ImageIcon(getClass().getResource(GUIConstants.ICON_BIOMODELS));
		putValue(LARGE_ICON_KEY, icon);
		
		this.putValue(SHORT_DESCRIPTION, GUIConstants.DESCRIPTION_BIOMODELS);
		setToolbarGravity(GUIConstants.GRAVITY_BIOMODELS);
	}

	public boolean isInToolBar() {
		return true;
	}
	public boolean isInMenuBar() {
		return false;
	}
	
	public OpenBrowser getOpenBrowser(){
		return this.openBrowser;
	}
	
	@Override
	public void actionPerformed(ActionEvent event) {
		logger.debug("BiomodelsRestAction performed.");
		JFrame frame = this.cySwingApplication.getJFrame();
		
		// Open JavaFX GUI
		WebViewSwing.launch(frame, openBrowser, sbmlReader);
	}
	
}