package org.cy3sbml.actions;

import java.awt.event.ActionEvent;
import java.util.Map;

import javax.swing.ImageIcon;

import javafx.application.Platform;

import org.cytoscape.application.events.SetCurrentNetworkEvent;
import org.cytoscape.application.events.SetCurrentNetworkListener;
import org.cytoscape.application.swing.AbstractCyAction;
import org.cytoscape.model.CyNetwork;

import org.cytoscape.work.TaskManager;
import org.sbml.jsbml.SBMLDocument;

import org.cy3sbml.SBMLManager;
import org.cy3sbml.ServiceAdapter;
import org.cy3sbml.gui.GUIConstants;
import org.cy3sbml.validator.ValidationFrame;
import org.cy3sbml.validator.ValidationTaskObserver;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Validate current SBML file
 * and display validation HTML report.
 */
public class ValidationAction extends AbstractCyAction implements SetCurrentNetworkListener {
    private static final Logger logger = LoggerFactory.getLogger(ValidationAction.class);
    private static final long serialVersionUID = 1L;

    private TaskManager taskManager;
    private ValidationEnableTaskFactory validationEnableTaskFactory;

    /**
     * Constructor.
     */
    public ValidationAction(Map<String, String> configProps, ServiceAdapter adapter, ValidationEnableTaskFactory validationEnableTaskFactory) {
        // super(ValidationAction.class.getSimpleName());
        super(configProps, adapter.cyApplicationManager, adapter.cyNetworkViewManager, validationEnableTaskFactory);
        taskManager = adapter.taskManager;
        this.validationEnableTaskFactory = validationEnableTaskFactory;

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
        runValidation(taskManager);
    }

    /**
     * Run the validation action.
     * This displays the validation dialog and performs the validation.
     *
     * @param taskManager
     */
    public static void runValidation(TaskManager taskManager) {
        SBMLDocument document = SBMLManager.getInstance().getCurrentSBMLDocument();

        /*
        JFrame parentFrame = adapter.cySwingApplication.getJFrame();
        if (document == null) {
            JOptionPane.showMessageDialog(parentFrame,
                    "<html>SBML must to loaded before validation.<br />" +
                            "Load network from file or URL, or import network from BioModels.</html>");
        } else {
        */

        if (document != null){
            // Open JavaFX Dialog
            ValidationFrame dialog = ValidationFrame.getInstance(null);
            Platform.runLater(new Runnable() {
                @Override
                public void run() {
                    dialog.setVisible(true);
                    dialog.toFront();
                }
            });

            // Validator action
            ValidationTaskObserver taskObserver = new ValidationTaskObserver(taskManager);
            taskObserver.runValidation(document);
        }
    }

    @Override
    public void handleEvent(SetCurrentNetworkEvent event) {
        CyNetwork network = event.getNetwork();
        if (network == null){
            validationEnableTaskFactory.setReady(false);
        } else {
            SBMLDocument doc = SBMLManager.getInstance().getSBMLDocument(network);
            if (doc == null){
                validationEnableTaskFactory.setReady(false);
            } else {
                validationEnableTaskFactory.setReady(true);
            }
        }
        updateEnableState();
    }
}
