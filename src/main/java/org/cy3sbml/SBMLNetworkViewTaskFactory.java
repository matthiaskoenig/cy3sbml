package org.cy3sbml;

import java.io.InputStream;
import java.util.Properties;

import org.cytoscape.io.CyFileFilter;
import org.cytoscape.io.read.AbstractInputStreamTaskFactory;
import org.cytoscape.model.CyNetworkFactory;
import org.cytoscape.property.CyProperty;
import org.cytoscape.view.layout.CyLayoutAlgorithmManager;
import org.cytoscape.view.model.CyNetworkViewFactory;
import org.cytoscape.view.vizmap.VisualMappingManager;
import org.cytoscape.work.SynchronousTaskManager;
import org.cytoscape.work.TaskIterator;
import org.cytoscape.work.TaskManager;

public class SBMLNetworkViewTaskFactory extends AbstractInputStreamTaskFactory {

	private final ServiceAdapter adapter;
	
	public SBMLNetworkViewTaskFactory(CyFileFilter filter, ServiceAdapter adapter) {
		super(filter);
		this.adapter = adapter;
	}
	
	public TaskIterator createTaskIterator(InputStream stream, String inputName) {
		
		// gets the SBMLNetworkViewReader Task
		return new TaskIterator(new SBMLNetworkViewReader(stream, adapter));
	}

}
