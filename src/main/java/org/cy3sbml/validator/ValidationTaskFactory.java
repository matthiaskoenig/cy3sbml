package org.cy3sbml.validator;


import org.cytoscape.work.TaskFactory;
import org.cytoscape.work.TaskIterator;

import org.sbml.jsbml.SBMLDocument;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * TaskFactory for validation of SBMLdocuments.
 */
public class ValidationTaskFactory implements TaskFactory {
    private static final Logger logger = LoggerFactory.getLogger(ValidationTaskFactory.class);
    private SBMLDocument document;

    /**
     * Constructor.
     */
    public ValidationTaskFactory(SBMLDocument document) {
        logger.info("ValidatorTaskFacory created");
        this.document = document;
    }

    @Override
    public TaskIterator createTaskIterator() {
        ValidationTask validationTask = new ValidationTask(document);
        return new TaskIterator(validationTask);
    }

    @Override
    public boolean isReady() {
        return true;
    }

}
