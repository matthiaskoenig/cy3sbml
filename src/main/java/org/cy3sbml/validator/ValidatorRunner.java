package org.cy3sbml.validator;


import org.cytoscape.work.FinishStatus;
import org.cytoscape.work.ObservableTask;
import org.cytoscape.work.TaskIterator;
import org.cytoscape.work.TaskObserver;

import org.sbml.jsbml.SBMLDocument;

import org.cy3sbml.ServiceAdapter;
import org.cy3sbml.gui.ValidationPanel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Runs the Validation Task.
 */
public class ValidatorRunner implements TaskObserver {
    private static final Logger logger = LoggerFactory.getLogger(ValidatorRunner.class);

    private ServiceAdapter adapter;
    private Validator validator;

    /**
     * Constructor.
     */
    public ValidatorRunner(ServiceAdapter adapter) {
        this.adapter = adapter;
        logger.debug("ValidatorRunner created");
    }

    /**
     * Run validation task for given SBMLDocument.
     *
     * @param document SBMLDocument to validate
     */
    public void runValidation(SBMLDocument document) {
        ValidatorTaskFactory validationTaskFactory = new ValidatorTaskFactory(document);
        TaskIterator iterator = validationTaskFactory.createTaskIterator();
        logger.info("run validation");
        adapter.taskManager.execute(iterator, this);
    }

    @Override
    public void taskFinished(ObservableTask task) {
        logger.info("taskFinished in ValidatorRunner");

        // execute task with task observer to be able to get results back
        Validator validator = task.getResults(Validator.class);
        this.validator = validator;
        setValidationInformation();
    }

    @Override
    public void allFinished(FinishStatus finishStatus) {
    }

    /**
     * Display validation results in validation panel.
     */
    private void setValidationInformation() {
        String html = validator.createHtml();
        ValidationPanel.getInstance().setText(html);
    }
}
