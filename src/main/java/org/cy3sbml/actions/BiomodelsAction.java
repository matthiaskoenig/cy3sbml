package org.cy3sbml.actions;

import java.awt.event.ActionEvent;

import javax.swing.ImageIcon;

import org.cy3sbml.gui.GUIConstants;
import org.cytoscape.application.swing.AbstractCyAction;

import org.cy3sbml.ServiceAdapter;
import org.cy3sbml.biomodel.BiomodelsDialog;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** 
 * Open the BioModel GUI for importing BioModels via search terms. 
 */
public class BiomodelsAction extends AbstractCyAction{
	private static final Logger logger = LoggerFactory.getLogger(BiomodelsAction.class);
	private static final long serialVersionUID = 1L;
	
	private ServiceAdapter adapter;

	/** Constructor. */
	public BiomodelsAction(ServiceAdapter adapter){
		super(BiomodelsAction.class.getSimpleName());
		this.adapter = adapter;
		
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
	
	@Override
	public void actionPerformed(ActionEvent event) {
		logger.debug("actionPerformed()");
	    BiomodelsDialog bioModelsDialog = BiomodelsDialog.getInstance(adapter);
	    bioModelsDialog.setVisible(true);   
	}
}
