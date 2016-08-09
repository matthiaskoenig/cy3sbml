package org.cy3sbml;

import java.io.IOException;
import java.io.InputStream;

import org.cytoscape.io.CyFileFilter;
import org.cytoscape.io.read.AbstractInputStreamTaskFactory;
import org.cytoscape.work.TaskIterator;

import org.cy3sbml.util.IOUtil;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * SBMLReader class
 * TaskFactory for the SBMLReaderTask.
 */
public class SBMLReader extends AbstractInputStreamTaskFactory {
    private static final Logger logger = LoggerFactory.getLogger(SBMLReader.class);
	private final ServiceAdapter adapter;


	/** Constructor. */
	public SBMLReader(CyFileFilter filter, ServiceAdapter adapter){
		super(filter);
		this.adapter = adapter;
	}

	@Override
	public TaskIterator createTaskIterator(InputStream is, String inputName) {		
		logger.debug("createTaskIterator: input stream name: " + inputName);

		try {
			return new TaskIterator(
				new SBMLReaderTask(IOUtil.copyInputStream(is), inputName,
                        adapter.cyNetworkFactory,
                        adapter.cyNetworkViewFactory,
						adapter.visualMappingManager,
                        adapter.cyLayoutAlgorithmManager,
                        adapter.cy3sbmlProperties)
			);
		} catch (IOException e) {
		    logger.error("Error in creating TaskIterator for SBMLReader.");
			e.printStackTrace();
            return null;
		}
	}
}
