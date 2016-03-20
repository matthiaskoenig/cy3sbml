package org.cy3sbml.validator;

import org.cy3sbml.biomodel.SearchBioModelTaskFactory;
import org.cytoscape.work.TaskFactory;
import org.cytoscape.work.TaskIterator;
import org.sbml.jsbml.SBMLDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Task for validation of SBMLdocuments.
 */
public class ValidatorTaskFactory implements TaskFactory{
	private static final Logger logger = LoggerFactory.getLogger(SearchBioModelTaskFactory.class);
	private SBMLDocument document;
	
	/** Constructor. */
	public ValidatorTaskFactory(SBMLDocument document) {
		logger.info("ValidatorTaskFacory created");
		this.document = document;
	}
	
	@Override
	public TaskIterator createTaskIterator() {
		ValidatorTask validatorTask = new ValidatorTask(document);
		return new TaskIterator(validatorTask);	
	}

	@Override
	public boolean isReady() {
		return true;
	}

}
