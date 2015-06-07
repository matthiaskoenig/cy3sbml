package org.cy3sbml.actions;

import java.awt.event.ActionEvent;
import java.net.MalformedURLException;
import java.net.URL;

import javax.swing.ImageIcon;

import org.cy3sbml.gui.SBMLControlPanel;
import org.cytoscape.application.swing.AbstractCyAction;
import org.cytoscape.application.swing.CySwingApplication;
import org.cytoscape.util.swing.OpenBrowser;

public class HelpAction extends AbstractCyAction{
	private static final long serialVersionUID = 1L;
	private OpenBrowser openBrowser;
	
	public HelpAction(CySwingApplication cySwingApplication, OpenBrowser openBrowser){
		super("HelpAction");
		this.openBrowser = openBrowser;
		
		ImageIcon icon = new ImageIcon(getClass().getResource("/images/help.png"));
		putValue(LARGE_ICON_KEY, icon);
		
		this.putValue(SHORT_DESCRIPTION, "cy3sbml help");
		// TODO: position in menu bar
		setToolbarGravity((float) 0.0);
	}
		
	public boolean isInToolBar() {
		return true;
	}
	public boolean isInMenuBar() {
		return false;
	}
	
	
	@Override
	public void actionPerformed(ActionEvent event) {
		final String HELP_URL = "http://www.google.com";
		System.out.println("HelpAction");
		
		// reset help information
		SBMLControlPanel.getInstance().setHelp();
		
		// open browser help
		URL url;
		try {
			url = new URL(HELP_URL);
			openBrowser.openURL(url.toString());
		} catch (MalformedURLException e) {
			e.printStackTrace();
		}
	}
}

