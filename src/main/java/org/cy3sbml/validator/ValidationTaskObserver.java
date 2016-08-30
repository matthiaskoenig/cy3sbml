package org.cy3sbml.validator;

import org.cytoscape.work.TaskManager;
import org.cytoscape.work.TaskIterator;
import org.cytoscape.work.TaskObserver;
import org.cytoscape.work.ObservableTask;
import org.cytoscape.work.FinishStatus;

import org.sbml.jsbml.SBMLDocument;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Runs the Validator Task.
 */
public class ValidationTaskObserver implements TaskObserver {
    private static final Logger logger = LoggerFactory.getLogger(ValidationTaskObserver.class);

    private TaskManager taskManager;

    /**
     * Constructor.
     */
    public ValidationTaskObserver(TaskManager taskManager) {
        logger.debug("ValidationTaskObserver created");
        this.taskManager = taskManager;
    }

    /**
     * Run validation task for given SBMLDocument.
     *
     * @param document SBMLDocument to validate
     */
    public void runValidation(SBMLDocument document) {
        logger.debug("run validation");
        ValidationTaskFactory validationTaskFactory = new ValidationTaskFactory(document);
        TaskIterator iterator = validationTaskFactory.createTaskIterator();
        taskManager.execute(iterator, this);
    }

    @Override
    public void taskFinished(ObservableTask task) {
        logger.debug("taskFinished in ValidationTaskObserver");

        // Task was executed with an observer so we can get the results back
        // when the validation is finished.
        Validator validator = task.getResults(Validator.class);
        // Display information
        String html = validator.createHtml();
        ValidationFrame validationFrame = ValidationFrame.getInstance();
        if (validationFrame != null){
            validationFrame.setText(html);
        } else {
            // for testing support without GUI
            logger.warn("No ValidationFrame instance.");
        }
    }

    @Override
    public void allFinished(FinishStatus finishStatus) {
    }
}
