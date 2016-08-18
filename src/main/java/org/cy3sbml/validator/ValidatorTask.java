package org.cy3sbml.validator;

import org.cytoscape.work.ObservableTask;
import org.cytoscape.work.TaskMonitor;
import org.sbml.jsbml.SBMLDocument;

/** 
 * Running the online SBML validation. 
 */
public class ValidatorTask implements ObservableTask{
	private SBMLDocument document;
	private Validator validator;
	
	/** Constructor. */
	public ValidatorTask(SBMLDocument document) {
		this.document = document;
	}

	@Override
	public void run(TaskMonitor taskMonitor) throws Exception {
		taskMonitor.setTitle("Online SBML Validation ...");
		taskMonitor.setProgress(0.0);
		try {
			taskMonitor.setProgress(0.4);
			validator = new Validator(document);
			taskMonitor.setStatusMessage("Validating SBML ...");
		} catch (Exception e) {
			e.printStackTrace();
		}	
		taskMonitor.setProgress(1.0);
	}

	@Override
	public void cancel() {}

	@SuppressWarnings("unchecked")
	@Override
	public <R> R getResults(Class<? extends R> type) {
		return (R) validator;
	}	
}