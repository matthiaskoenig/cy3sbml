package org.cy3sbml.actions;

import java.awt.event.ActionEvent;

import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JOptionPane;

import org.cy3sbml.SBMLManager;
import org.cy3sbml.ServiceAdapter;
import org.cy3sbml.gui.ValidationDialog;
import org.cytoscape.application.swing.AbstractCyAction;
import org.sbml.jsbml.SBMLDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/** Open the BioModel GUI for importing BioModels via search terms. */
public class ValidationAction extends AbstractCyAction{
	private static final Logger logger = LoggerFactory.getLogger(ValidationAction.class);
	private static final long serialVersionUID = 1L;
	
	private ServiceAdapter adapter;
	
	public ValidationAction(ServiceAdapter adapter){
		super("BioModelAction");
		logger.debug("BioModelAction created");
		this.adapter = adapter;
		
		ImageIcon icon = new ImageIcon(getClass().getResource("/images/validation.png"));
		putValue(LARGE_ICON_KEY, icon);
		
		this.putValue(SHORT_DESCRIPTION, "SBML validation");
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
	
    public void openValidationPanel(){
    	SBMLDocument doc = SBMLManager.getInstance().getCurrentSBMLDocument();
    	JFrame parentFrame = adapter.cySwingApplication.getJFrame();
    	if (doc == null){
    		JOptionPane.showMessageDialog(parentFrame,
					"<html>SBML network has to be loaded before validation.<br>" +
					"Import network from BioModel or load network from file or URL first.");
    	}
    	else{
    		ValidationDialog validationDialog = new ValidationDialog(parentFrame, doc);
    		validationDialog.setVisible(true);
    	}
    }
	
	@Override
	public void actionPerformed(ActionEvent event) {
		logger.debug("actionPerformed()");
		openValidationPanel();  
	}
}
