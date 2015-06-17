package org.cy3sbml.actions;

import java.awt.event.ActionEvent;

import org.cytoscape.application.swing.AbstractCyAction;
import org.cytoscape.application.swing.CySwingApplication;
import org.cy3sbml.gui.ResultsPanel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ResultsPanelAction extends AbstractCyAction {
	private static final Logger logger = LoggerFactory.getLogger(ResultsPanelAction.class);
	private static final long serialVersionUID = 1L;
	
	public ResultsPanelAction(CySwingApplication desktopApp){
		// Add menu item -- Apps->cy3sbml
		super("cy3sbml");
		setPreferredMenu("Apps");
	}
	
	public void actionPerformed(ActionEvent e) {
		logger.debug("actionPerformed");
		ResultsPanel panel = ResultsPanel.getInstance();
		if (panel.isActive()){
			panel.deactivate();
		} else {
			panel.activate();
		}
	}
}
