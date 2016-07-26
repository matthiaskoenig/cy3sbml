package org.cy3sbml.actions;

import java.awt.event.ActionEvent;
import javax.swing.ImageIcon;

import org.cy3sbml.gui.GUIConstants;
import org.cy3sbml.gui.WebViewPanel;
import org.cytoscape.application.swing.AbstractCyAction;
import org.cytoscape.application.swing.CySwingApplication;
import org.cy3sbml.gui.ResultsPanel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Set help information in ResultsPanel.
 */
public class HelpAction extends AbstractCyAction{
	private static final Logger logger = LoggerFactory.getLogger(HelpAction.class);
	private static final long serialVersionUID = 1L;
	public static final String HELP_URL = "https://github.com/matthiaskoenig/cy3sbml";
	
	/** Constructor. */
	public HelpAction(CySwingApplication cySwingApplication){
		super("HelpAction");
		
		ImageIcon icon = new ImageIcon(getClass().getResource(GUIConstants.IMAGE_HELP));
		putValue(LARGE_ICON_KEY, icon);
		
		this.putValue(SHORT_DESCRIPTION, "cy3sbml help");
		setToolbarGravity((float) 120.0);
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
		panel.getTextPane().setHelp();

		WebViewPanel vwPanel = WebViewPanel.getInstance();
		vwPanel.activate();
		vwPanel.setHelp();
	}
}

