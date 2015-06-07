package org.cy3sbml.actions;

import java.awt.event.ActionEvent;

import javax.swing.ImageIcon;

import org.cytoscape.application.swing.AbstractCyAction;
import org.cytoscape.application.swing.CySwingApplication;

public class ImportAction extends AbstractCyAction{
	private static final long serialVersionUID = 1L;

	public ImportAction(CySwingApplication cySwingApplication){
		super("ImportAction");
		ImageIcon icon = new ImageIcon(getClass().getResource("/images/import.png"));
		this.putValue(LARGE_ICON_KEY, icon);
		// TODO: position in menu bar
		//this.setMenuGravity(100);
		this.putValue(SHORT_DESCRIPTION, "Import SBML");
	}
		
	@Override
	public void actionPerformed(ActionEvent e) {
		System.out.println("ImportAction");
		// TODO: open the file menu 
		

	}
}

