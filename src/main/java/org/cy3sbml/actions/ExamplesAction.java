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
public final class ExamplesAction extends AbstractCyAction {
    private static final Logger logger = LoggerFactory.getLogger(ExamplesAction.class);
    private static final long serialVersionUID = 1L;

    private final WebViewPanel webViewPanel;

    /**
     * Constructor.
     */
    public ExamplesAction(WebViewPanel webViewPanel) {
        super(ExamplesAction.class.getSimpleName());
        this.webViewPanel = webViewPanel;

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
        webViewPanel.activate();
        webViewPanel.setExamples();
    }
}
