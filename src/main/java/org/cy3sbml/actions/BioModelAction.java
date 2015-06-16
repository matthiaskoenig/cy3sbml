package org.cy3sbml.actions;

import java.awt.event.ActionEvent;

import javax.swing.ImageIcon;

import org.cy3sbml.ServiceAdapter;
import org.cy3sbml.biomodel.BioModelDialog;
import org.cytoscape.application.swing.AbstractCyAction;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Open the BioModel GUI for importing BioModels via search terms. */
public class BioModelAction extends AbstractCyAction{
	private static final Logger logger = LoggerFactory.getLogger(BioModelAction.class);
	private static final long serialVersionUID = 1L;
	
	private ServiceAdapter adapter;
	
	public BioModelAction(ServiceAdapter adapter){
		super("BioModelAction");
		logger.info("BioModelAction created");
		this.adapter = adapter;
		
		//ImageIcon icon = new ImageIcon(getClass().getResource("/images/biomodel.png"));
		ImageIcon icon = new ImageIcon(getClass().getResource("/images/biomodels_logo.png"));
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
	    BioModelDialog bioModelsDialog = BioModelDialog.getInstance(adapter);
	    bioModelsDialog.setVisible(true);   
	}
}
