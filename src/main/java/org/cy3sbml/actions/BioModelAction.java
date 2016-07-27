package org.cy3sbml.actions;

import java.awt.event.ActionEvent;

import javax.swing.ImageIcon;

import org.cy3sbml.gui.GUIConstants;
import org.cytoscape.application.swing.AbstractCyAction;

import org.cy3sbml.ServiceAdapter;
import org.cy3sbml.biomodel.BioModelDialog;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** 
 * Open the BioModel GUI for importing BioModels via search terms. 
 */
public class BioModelAction extends AbstractCyAction{
	private static final Logger logger = LoggerFactory.getLogger(BioModelAction.class);
	private static final long serialVersionUID = 1L;
	
	private ServiceAdapter adapter;

	/** Constructor. */
	public BioModelAction(ServiceAdapter adapter){
		super("BioModelAction");
		this.adapter = adapter;
		
		ImageIcon icon = new ImageIcon(getClass().getResource(GUIConstants.IMAGE_BIOMODELS_LOGO));
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
		logger.debug("actionPerformed()");
	    BioModelDialog bioModelsDialog = BioModelDialog.getInstance(adapter);
	    bioModelsDialog.setVisible(true);   
	}
}
