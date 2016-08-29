package org.cy3sbml.actions;

import java.awt.event.ActionEvent;

import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JOptionPane;

import javafx.application.Platform;
import org.cy3sbml.gui.GUIConstants;
import org.cy3sbml.validator.ValidationFrame;
import org.cy3sbml.validator.ValidatorRunner;
import org.cytoscape.application.swing.AbstractCyAction;
import org.sbml.jsbml.SBMLDocument;

import org.cy3sbml.SBMLManager;
import org.cy3sbml.ServiceAdapter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Open the BioModel GUI for importing BioModels via search terms.
 */
public class ValidationAction extends AbstractCyAction {
    private static final Logger logger = LoggerFactory.getLogger(ValidationAction.class);
    private static final long serialVersionUID = 1L;

    private ServiceAdapter adapter;

    /** Constructor. */
    public ValidationAction(ServiceAdapter adapter) {
        super(ValidationAction.class.getSimpleName());
        this.adapter = adapter;

        ImageIcon icon = new ImageIcon(getClass().getResource(GUIConstants.ICON_VALIDATION));
        putValue(LARGE_ICON_KEY, icon);

        this.putValue(SHORT_DESCRIPTION, GUIConstants.DESCRIPTION_VALIDATION);
        setToolbarGravity(GUIConstants.GRAVITY_VALIDATION);
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
        runValidation(adapter);
    }

    /**
     * Run the validation action.
     * This displays the validation dialog and performs the validation.
     * @param adapter
     */
    public static void runValidation(ServiceAdapter adapter) {
        SBMLDocument document = SBMLManager.getInstance().getCurrentSBMLDocument();
        JFrame parentFrame = adapter.cySwingApplication.getJFrame();

        if (document == null) {
            JOptionPane.showMessageDialog(parentFrame,
                    "<html>SBML must to loaded before validation.<br />" +
                            "Load network from file or URL, or import network from BioModels.</html>");
        } else {

            // Open JavaFX Dialog
            ValidationFrame dialog = ValidationFrame.getInstance(adapter);
            Platform.runLater(new Runnable() {
                @Override
                public void run() {
                    dialog.updateInformation();
                    dialog.setVisible(true);
                    dialog.toFront();
                }
            });

            // Validation action
            ValidatorRunner runner = new ValidatorRunner(adapter);
            runner.runValidation(document);
        }
    }
}
