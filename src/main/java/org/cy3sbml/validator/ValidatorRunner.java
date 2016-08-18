package org.cy3sbml.validator;

import java.util.List;

import org.cytoscape.work.FinishStatus;
import org.cytoscape.work.ObservableTask;
import org.cytoscape.work.TaskIterator;
import org.cytoscape.work.TaskObserver;

import org.sbml.jsbml.SBMLDocument;
import org.sbml.jsbml.SBMLError;

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

	/** Constructor */
	public ValidatorRunner(ServiceAdapter adapter) {
		this.adapter = adapter;

		logger.info("ValidatorRunner created");
	}
	
	public void runValidation(SBMLDocument document){

		// run validation task
		ValidatorTaskFactory validationTaskFactory = new ValidatorTaskFactory(document);
		TaskIterator iterator = validationTaskFactory.createTaskIterator();
		logger.info("run validation");
		adapter.taskManager.execute(iterator, this);
	}
	
	@Override
	public void taskFinished(ObservableTask task) {
		logger.info("taskFinished in ValidatorDialog");
		
		// execute task with task observer to be able to get results back
		Validator validator = task.getResults(Validator.class);
		this.validator = validator;
		setValidationInformation();
	}

	@Override
	public void allFinished(FinishStatus finishStatus) {
	}

	private void setValidationInformation(){
		if (validator.getErrorMap() != null) {
			List<SBMLError> eList = validator.getErrorList();
			String html = validator.createHTML(eList);
			ValidationPanel.getInstance().setText(html);
		} else {
			ValidationPanel.getInstance().setText("SBML validation failed.");
		}
	}
}
