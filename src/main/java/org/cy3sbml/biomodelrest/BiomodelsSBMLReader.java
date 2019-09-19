package org.cy3sbml.biomodelrest;

import org.cytoscape.task.read.LoadNetworkFileTaskFactory;
import org.cytoscape.work.TaskIterator;
import org.cytoscape.work.TaskManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;

/**
 * Helper class to provide SBML reader functionality.
 *
 * TODO: this functionality must be provided by cy3sbml, i.e.
 * 		a helper class which allows other apps to easily read SBML.
 *
 * 	FIXME: This currently only handles SBML models, but should also work with OMEX files.
 * 	This can be implemented based on the combine archive readers.
 */
public class BiomodelsSBMLReader {
	private static final Logger logger = LoggerFactory.getLogger(BiomodelsSBMLReader.class);
	
	private LoadNetworkFileTaskFactory factory;
	@SuppressWarnings("rawtypes")
	private TaskManager taskManager;
	
	/* Helper class to read SBML graphs. */
	@SuppressWarnings("rawtypes")
	public BiomodelsSBMLReader(LoadNetworkFileTaskFactory factory, TaskManager taskManager){
		this.factory = factory;
		this.taskManager = taskManager;
	}
	
	/** Create Cytoscape graphs from SBML string. */
	public void loadNetworkFromSBML(String sbml){
		logger.debug("Load SBML for biomodel");
		try{
			// write with encoding
			File tmpFile = File.createTempFile("test", ".xml");
			Writer out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(tmpFile), "UTF-8"));
	    	try {
	    	    out.write(sbml);
	    	} finally {
	    	    out.close();
	    	}
			
    	    // execute task
    		TaskIterator taskIterator = factory.createTaskIterator(tmpFile);
    		taskManager.execute(taskIterator);
    		
    	}catch(IOException e){
    	    e.printStackTrace();
    	}
	}
}
