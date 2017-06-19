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
    private SBMLEnableTaskFactory sbmlEnableTaskFactory;

    /**
     * Constructor.
     */
    public ValidationAction(Map<String, String> configProps, ServiceAdapter adapter, SBMLEnableTaskFactory sbmlEnableTaskFactory) {
        super(configProps, adapter.cyApplicationManager, adapter.cyNetworkViewManager, sbmlEnableTaskFactory);
        taskManager = adapter.taskManager;
        this.sbmlEnableTaskFactory = sbmlEnableTaskFactory;

        ImageIcon icon = new ImageIcon(getClass().getResource(GUIConstants.ICON_VALIDATION));
        putValue(LARGE_ICON_KEY, icon);

        this.putValue(SHORT_DESCRIPTION, GUIConstants.DESCRIPTION_VALIDATION);
        setToolbarGravity(GUIConstants.GRAVITY_VALIDATION);

        this.inToolBar = true;
        this.inMenuBar = false;
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

    /**
     * Updates the ready variable.
     * @param event
     */
    @Override
    public void handleEvent(SetCurrentNetworkEvent event) {
        CyNetwork network = event.getNetwork();
        boolean ready = false;
        if (network != null){
            SBMLDocument doc = SBMLManager.getInstance().getSBMLDocument(network);
            if (doc != null) {
                ready = true;
            }
        }
        sbmlEnableTaskFactory.setReady(ready);
        updateEnableState();
    }
}
