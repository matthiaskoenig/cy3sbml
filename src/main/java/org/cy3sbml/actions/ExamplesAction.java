package org.cy3sbml.actions;

import java.awt.event.ActionEvent;
import javax.swing.ImageIcon;

import org.cytoscape.application.swing.AbstractCyAction;
import org.cytoscape.application.swing.CySwingApplication;

import org.cy3sbml.gui.ResultsPanel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ExamplesAction extends AbstractCyAction{
	private static final Logger logger = LoggerFactory.getLogger(ExamplesAction.class);
	private static final long serialVersionUID = 1L;
	
	/** Constructor. */
	public ExamplesAction(CySwingApplication cySwingApplication){
		super("ExamplesAction");
		
		ImageIcon icon = new ImageIcon(getClass().getResource("/images/examples.png"));
		putValue(LARGE_ICON_KEY, icon);
		
		this.putValue(SHORT_DESCRIPTION, "cy3sbml examples");
		setToolbarGravity((float) 110.0);
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
		
		// set information
		ResultsPanel panel = ResultsPanel.getInstance();
		panel.activate();
		panel.getTextPane().setExamples();
	}
}

