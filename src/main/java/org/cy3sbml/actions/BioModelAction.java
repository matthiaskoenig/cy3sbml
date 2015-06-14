package org.cy3sbml.actions;

import java.awt.event.ActionEvent;

import javax.swing.ImageIcon;

import org.cy3sbml.ConnectionProxy;
import org.cy3sbml.biomodel.BioModelGUIDialog;
import org.cytoscape.application.swing.AbstractCyAction;
import org.cytoscape.application.swing.CySwingApplication;
import org.cytoscape.util.swing.OpenBrowser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BioModelAction extends AbstractCyAction{
	private static final Logger logger = LoggerFactory.getLogger(BioModelAction.class);
	private static final long serialVersionUID = 1L;
	CySwingApplication cySwingApplication;
	OpenBrowser openBrowser;
	ConnectionProxy connectionProxy;
	
	public BioModelAction(CySwingApplication cySwingApplication, OpenBrowser openBrowser, ConnectionProxy connectionProxy){
		super("BioModelAction");
		this.cySwingApplication = cySwingApplication;
		this.openBrowser = openBrowser;
		this.connectionProxy = connectionProxy;
		
		ImageIcon icon = new ImageIcon(getClass().getResource("/images/biomodel.png"));
		putValue(LARGE_ICON_KEY, icon);
		
		this.putValue(SHORT_DESCRIPTION, "cy3sbml BioModel Import");
		setToolbarGravity((float) 100.0);
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
	
	@Override
	public void actionPerformed(ActionEvent event) {
		logger.info("actionPerformed()");
		// Open the BioModels Dialog
	    BioModelGUIDialog bioModelsDialog = BioModelGUIDialog.getInstance(cySwingApplication.getJFrame(),
	    																  openBrowser, connectionProxy);
	    bioModelsDialog.setVisible(true);   
	}
}
