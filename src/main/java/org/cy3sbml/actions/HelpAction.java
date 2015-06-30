package org.cy3sbml.actions;

import java.awt.event.ActionEvent;
import java.net.MalformedURLException;
import java.net.URL;

import javax.swing.ImageIcon;

import org.cy3sbml.gui.ResultsPanel;
import org.cytoscape.application.swing.AbstractCyAction;
import org.cytoscape.application.swing.CySwingApplication;
import org.cytoscape.util.swing.OpenBrowser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HelpAction extends AbstractCyAction{
	private static final Logger logger = LoggerFactory.getLogger(HelpAction.class);
	private static final long serialVersionUID = 1L;
	public static final String HELP_URL = "https://github.com/matthiaskoenig/cy3sbml";
	
	private OpenBrowser openBrowser;
	
	public HelpAction(CySwingApplication cySwingApplication, OpenBrowser openBrowser){
		super("HelpAction");
		this.openBrowser = openBrowser;
		
		ImageIcon icon = new ImageIcon(getClass().getResource("/images/help.png"));
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
		
		// open browser help
		/*
		URL url;
		try {
			url = new URL(HELP_URL);
			openBrowser.openURL(url.toString());
		} catch (MalformedURLException e) {
			e.printStackTrace();
		}
		*/
	}
}

