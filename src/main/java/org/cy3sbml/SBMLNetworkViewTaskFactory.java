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

	private final CyNetworkFactory networkFactory;
	private final CyNetworkViewFactory viewFactory;
	private final CyProperty<Properties> cy3sbmlProperties;
	private final VisualMappingManager visualMappingManager;
	private final CyLayoutAlgorithmManager cyLayoutAlgorithmManager;
	private final SynchronousTaskManager taskManager;
	
	public SBMLNetworkViewTaskFactory(CyFileFilter filter, CyNetworkFactory networkFactory, CyNetworkViewFactory viewFactory, 
									  CyProperty<Properties> cy3sbmlProperties,
									  VisualMappingManager visualMappingManager, CyLayoutAlgorithmManager cyLayoutAlgorithmManager, SynchronousTaskManager taskManager) {
		super(filter);
		this.networkFactory = networkFactory;
		this.viewFactory = viewFactory;
		this.cy3sbmlProperties = cy3sbmlProperties;
		this.visualMappingManager = visualMappingManager;
		this.cyLayoutAlgorithmManager = cyLayoutAlgorithmManager;
		this.taskManager = taskManager;
	}
	
	public TaskIterator createTaskIterator(InputStream stream, String inputName) {
		
		// gets the SBMLNetworkViewReader Task
		return new TaskIterator(new SBMLNetworkViewReader(stream, networkFactory, viewFactory, cy3sbmlProperties, visualMappingManager, cyLayoutAlgorithmManager, taskManager));
	}

}
