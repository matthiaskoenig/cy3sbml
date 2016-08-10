package org.cy3sbml.actions;

import java.awt.event.ActionEvent;
import javax.swing.ImageIcon;

import org.cy3sbml.gui.GUIConstants;
import org.cy3sbml.gui.WebViewPanel;
import org.cytoscape.application.swing.AbstractCyAction;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Set help information in ResultsPanel.
 */
public class HelpAction extends AbstractCyAction{
	private static final Logger logger = LoggerFactory.getLogger(HelpAction.class);
	private static final long serialVersionUID = 1L;
	
	/** Constructor. */
	public HelpAction(){
		super(HelpAction.class.getSimpleName());
		
		ImageIcon icon = new ImageIcon(getClass().getResource(GUIConstants.ICON_HELP));
		putValue(LARGE_ICON_KEY, icon);
		
		this.putValue(SHORT_DESCRIPTION, GUIConstants.DESCRIPTION_HELP);
		setToolbarGravity(GUIConstants.GRAVITY_HELP);
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
		WebViewPanel vwPanel = WebViewPanel.getInstance();
		vwPanel.activate();
		vwPanel.setHelp();
	}
}

