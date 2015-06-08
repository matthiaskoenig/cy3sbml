package org.cy3sbml;


import java.io.InputStream;

import org.cytoscape.io.CyFileFilter;
import org.cytoscape.io.read.AbstractInputStreamTaskFactory;
import org.cytoscape.model.CyNetworkFactory;
import org.cytoscape.task.visualize.ApplyPreferredLayoutTaskFactory;
import org.cytoscape.task.visualize.ApplyVisualStyleTaskFactory;
import org.cytoscape.view.model.CyNetworkViewFactory;
import org.cytoscape.work.TaskIterator;

public class SBMLNetworkViewTaskFactory extends AbstractInputStreamTaskFactory {

	private final CyNetworkFactory networkFactory;
	private final CyNetworkViewFactory viewFactory;
	private final ApplyPreferredLayoutTaskFactory applyPreferredLayout;
	private final ApplyVisualStyleTaskFactory applyVisualStyle;
	
	public SBMLNetworkViewTaskFactory(CyFileFilter filter, CyNetworkFactory networkFactory, CyNetworkViewFactory viewFactory,
									  ApplyPreferredLayoutTaskFactory applyPreferredLayout, ApplyVisualStyleTaskFactory applyVisualStyle) {
		super(filter);
		this.networkFactory = networkFactory;
		this.viewFactory = viewFactory;
		this.applyPreferredLayout = applyPreferredLayout;
		this.applyVisualStyle = applyVisualStyle;
	}
	
	public TaskIterator createTaskIterator(InputStream stream, String inputName) {
		return new TaskIterator(new SBMLNetworkViewReader(stream, networkFactory, viewFactory, applyPreferredLayout, applyVisualStyle));
	}

}
