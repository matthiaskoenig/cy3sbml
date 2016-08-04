package org.cy3sbml.actions;

import java.awt.event.ActionEvent;
import javax.swing.ImageIcon;

import org.cy3sbml.gui.GUIConstants;
import org.cy3sbml.gui.WebViewPanel;
import org.cytoscape.application.swing.AbstractCyAction;
import org.cytoscape.application.swing.CySwingApplication;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Activate or deactivate the cy3sbml panel.
 * This allows to hide the panel and remove the overhead of
 * information generation and update.
 */
public class ChangeStateAction extends AbstractCyAction{
	private static final Logger logger = LoggerFactory.getLogger(ChangeStateAction.class);
	private static final long serialVersionUID = 1L;
	
	/** Constructor. */
	public ChangeStateAction(){
		super("ChangeStateAction");
		
		ImageIcon icon = new ImageIcon(getClass().getResource(GUIConstants.IMAGE_CHANGESTATE));
		putValue(LARGE_ICON_KEY, icon);
		
		this.putValue(SHORT_DESCRIPTION, "Hide|show panel");
		setToolbarGravity((float) 90.0);
		insertSeparatorBefore = true;
        insertSeparatorAfter = true;
	}

	@Override
	public boolean insertSeparatorBefore() { return true; }
    @Override
    public boolean insertSeparatorAfter() { return true; }
	@Override
	public boolean isInToolBar() {
		return true;
	}
	@Override
	public boolean isInMenuBar() {
		return false;
	}
		
	@Override
	public void actionPerformed(ActionEvent event) {
		logger.debug("actionPerformed");
		WebViewPanel vwPanel = WebViewPanel.getInstance();
		vwPanel.changeState();
	}
}
