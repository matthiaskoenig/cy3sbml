package org.cy3sbml.validator;

import org.cy3sbml.ServiceAdapter;
import org.cy3sbml.biomodel.BioModelWSInterface;
import org.cy3sbml.biomodel.SearchBioModelTask;
import org.cy3sbml.biomodel.SearchBioModelTaskFactory;
import org.cy3sbml.biomodel.SearchContent;
import org.cytoscape.work.TaskFactory;
import org.cytoscape.work.TaskIterator;
import org.sbml.jsbml.SBMLDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ValidatorTaskFactory implements TaskFactory{

	private static final Logger logger = LoggerFactory.getLogger(SearchBioModelTaskFactory.class);

	private SBMLDocument document;
	
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
		return false;
	}

}
