package org.cy3sbml.actions;

import java.awt.event.ActionEvent;

import javax.swing.ImageIcon;

import org.cytoscape.application.swing.AbstractCyAction;
import org.cytoscape.application.swing.CySwingApplication;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ImportAction extends AbstractCyAction{
	private static final Logger logger = LoggerFactory.getLogger(ImportAction.class);
	private static final long serialVersionUID = 1L;

	public ImportAction(CySwingApplication cySwingApplication){
		super("ImportAction");
		ImageIcon icon = new ImageIcon(getClass().getResource("/images/import.png"));
		putValue(LARGE_ICON_KEY, icon);
		
		this.putValue(SHORT_DESCRIPTION, "Import SBML");
		setToolbarGravity((float) 0.5);
	}
		
	public boolean isInToolBar() {
		return true;
	}
	
	@Override
	public void actionPerformed(ActionEvent e) {
		logger.info("actionPerformed()");
		// TODO: implement 
	}
}

