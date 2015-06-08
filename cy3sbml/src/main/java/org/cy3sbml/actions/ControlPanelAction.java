package org.cy3sbml.actions;

import java.awt.event.ActionEvent;

import org.cytoscape.application.swing.AbstractCyAction;
import org.cytoscape.application.swing.CySwingApplication;
import org.cytoscape.application.swing.CytoPanel;
import org.cytoscape.application.swing.CytoPanelName;
import org.cytoscape.application.swing.CytoPanelState;
import org.cy3sbml.gui.SBMLControlPanel;

public class ControlPanelAction extends AbstractCyAction {

	private static final long serialVersionUID = 1L;
	private CySwingApplication desktopApp;
	private final CytoPanel cytoPanelEast;
	
	private SBMLControlPanel navControlPanel;
	
	public ControlPanelAction(CySwingApplication desktopApp){
	
		// Add a menu item -- Apps->cy3sbml
		super("panel");
		setPreferredMenu("Apps.cy3sbml");
		this.desktopApp = desktopApp;
		
		this.cytoPanelEast = this.desktopApp.getCytoPanel(CytoPanelName.EAST);
		this.navControlPanel = SBMLControlPanel.getInstance(null);
	}
	
	/**
	 *  DOCUMENT ME!
	 *
	 * @param e DOCUMENT ME!
	 */
	public void actionPerformed(ActionEvent e) {
		// If the state of the cytoPanelWest is HIDE, show it
		if (cytoPanelEast.getState() == CytoPanelState.HIDE) {
			cytoPanelEast.setState(CytoPanelState.DOCK);
		}	

		// Select my panel
		int index = cytoPanelEast.indexOfComponent(navControlPanel);
		if (index == -1) {
			return;
		}
		cytoPanelEast.setSelectedIndex(index);
	}

}
