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
public class HelpAction extends AbstractCyAction {
    private static final Logger logger = LoggerFactory.getLogger(HelpAction.class);
    private static final long serialVersionUID = 1L;

    /**
     * Constructor.
     */
    public HelpAction() {
        super(HelpAction.class.getSimpleName());


        ImageIcon icon = new ImageIcon(getClass().getResource(GUIConstants.ICON_HELP));
        this.putValue(LARGE_ICON_KEY, icon);

        this.putValue(SHORT_DESCRIPTION, GUIConstants.DESCRIPTION_HELP);
        this.putValue(LONG_DESCRIPTION, "Display help information for quick access to main functions.");
        setToolbarGravity(GUIConstants.GRAVITY_HELP);

        this.inToolBar = true;
        this.inMenuBar = false;
        this.insertSeparatorBefore = true;
    }

    @Override
    public void actionPerformed(ActionEvent event) {
        logger.debug("actionPerformed()");
        WebViewPanel vwPanel = WebViewPanel.getInstance();
        vwPanel.activate();
        vwPanel.setHelp();
    }
}

