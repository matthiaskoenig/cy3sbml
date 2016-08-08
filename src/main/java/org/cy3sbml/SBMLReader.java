package org.cy3sbml;

import java.io.IOException;
import java.io.InputStream;

import javax.swing.SwingUtilities;

import org.cytoscape.io.CyFileFilter;
import org.cytoscape.io.read.AbstractInputStreamTaskFactory;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.view.layout.CyLayoutAlgorithm;
import org.cytoscape.view.layout.CyLayoutAlgorithmManager;
import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.view.model.events.NetworkViewAddedEvent;
import org.cytoscape.view.model.events.NetworkViewAddedListener;
import org.cytoscape.view.vizmap.VisualMappingManager;
import org.cytoscape.view.vizmap.VisualStyle;
import org.cytoscape.work.TaskIterator;

import org.cy3sbml.util.IOUtil;
import org.cy3sbml.util.NetworkUtil;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * SBMLReader class
 * 
 * Manages the reading of SBMLDocuments within
 * the SBMLReaderTasks and creates networks and views for the given
 * SBMLDocument.
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
			throw new SBMLReaderError(e.toString());
		}
	}
}
