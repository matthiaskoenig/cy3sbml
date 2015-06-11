package org.cy3sbml.actions;

import java.awt.event.ActionEvent;

import org.cytoscape.application.swing.AbstractCyAction;
import org.cytoscape.application.swing.CySwingApplication;
import org.cy3sbml.gui.ControlPanel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ControlPanelAction extends AbstractCyAction {
	private static final Logger logger = LoggerFactory.getLogger(ControlPanelAction.class);
	private static final long serialVersionUID = 1L;
	
	public ControlPanelAction(CySwingApplication desktopApp){
		// Add menu item -- Apps->cy3sbml
		super("cy3sbml");
		setPreferredMenu("Apps");
	}
	
	public void actionPerformed(ActionEvent e) {
		logger.info("actionPerformed");
		ControlPanel panel = ControlPanel.getInstance();
		if (panel.isActive()){
			panel.deactivate();
		} else {
			panel.activate();
		}
	}
}
