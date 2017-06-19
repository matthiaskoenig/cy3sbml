package org.cy3sbml.actions;

import java.awt.event.ActionEvent;
import javax.swing.ImageIcon;

import org.cy3sbml.gui.GUIConstants;
import org.cy3sbml.gui.WebViewPanel;
import org.cytoscape.application.swing.AbstractCyAction;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Loads the example HTML page.
 */
public class ExamplesAction extends AbstractCyAction{
	private static final Logger logger = LoggerFactory.getLogger(ExamplesAction.class);
	private static final long serialVersionUID = 1L;
	
	/** Constructor. */
	public ExamplesAction(){
		super(ExamplesAction.class.getSimpleName());
		
		ImageIcon icon = new ImageIcon(getClass().getResource(GUIConstants.ICON_EXAMPLES));
		putValue(LARGE_ICON_KEY, icon);
		
		this.putValue(SHORT_DESCRIPTION, GUIConstants.DESCRIPTION_EXAMPLES);
		setToolbarGravity(GUIConstants.GRAVITY_EXAMPLES);

		this.inToolBar = true;
		this.inMenuBar = false;
	}
	
	@Override
	public void actionPerformed(ActionEvent event) {
		logger.debug("actionPerformed()");
		WebViewPanel vwPanel = WebViewPanel.getInstance();
		vwPanel.activate();
		vwPanel.setExamples();
	}
}

