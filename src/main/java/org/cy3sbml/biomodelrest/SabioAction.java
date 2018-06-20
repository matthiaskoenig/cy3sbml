package org.cy3sbml.biomodelrest;

import org.cy3sabiork.gui.WebViewSwing;
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
public class SabioAction extends AbstractCyAction{
	private static final long serialVersionUID = 1L;
	private static final Logger logger = LoggerFactory.getLogger(SabioAction.class);
	private CySwingApplication cySwingApplication;
	private OpenBrowser openBrowser;
	private BiomodelsSBMLReader sbmlReader;
	
	/** 
	 * Constructor. 
	 * Requires functionality for open links in external browser and
	 * for reading SBML into networks.
	 */
	public SabioAction(CySwingApplication cySwingApplication, OpenBrowser openBrowser, BiomodelsSBMLReader sbmlReader){
		super("SabioRKAction");
		this.cySwingApplication = cySwingApplication;
		this.openBrowser = openBrowser;
		this.sbmlReader = sbmlReader;
		
		ImageIcon icon = new ImageIcon(getClass().getResource("/gui/images/icon-cy3sabiork.png"));
		putValue(LARGE_ICON_KEY, icon);
		
		this.putValue(SHORT_DESCRIPTION, "SABIO-RK web services");
		setToolbarGravity((float) 80.0);
	}
	
	public boolean insertSeparatorBefore(){
		return true;
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
		logger.debug("SabioAction performed.");
		JFrame frame = this.cySwingApplication.getJFrame();
		
		// Open JavaFX GUI
		WebViewSwing.launch(frame, openBrowser, sbmlReader);
		
		// Open old JPanel based Dialog
		// SabioDialog.launch(frame, sbmlReader);



	}
	
}