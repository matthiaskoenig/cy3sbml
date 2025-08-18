package org.cy3sbml.biomodel;

import org.cytoscape.work.TaskFactory;
import org.cytoscape.work.TaskIterator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class SearchBioModelTaskFactory implements TaskFactory {
	private static final Logger logger = LoggerFactory.getLogger(SearchBioModelTaskFactory.class);

	private SearchContent searchContent;
	
	public SearchBioModelTaskFactory(SearchContent searchContent) {
		logger.info("SearchBioModelTaskFactory created");
		this.searchContent = searchContent;
    }
	
	@Override
	public TaskIterator createTaskIterator() {
		
		SearchBioModelTask searchTask = new SearchBioModelTask(searchContent);
		return new TaskIterator(searchTask);		
	}

	@Override
	public boolean isReady() {
		
		// How to get data out of the task (Observable?)
		return true;
	}
}
