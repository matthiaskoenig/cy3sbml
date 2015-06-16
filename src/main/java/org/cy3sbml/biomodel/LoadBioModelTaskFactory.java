package org.cy3sbml.biomodel;

import org.cy3sbml.ServiceAdapter;
import org.cytoscape.work.TaskFactory;
import org.cytoscape.work.TaskIterator;

public class LoadBioModelTaskFactory implements TaskFactory{

	private ServiceAdapter adapter;
	
	public LoadBioModelTaskFactory(ServiceAdapter adapter) {
		this.adapter = adapter;
	}
	
	
	@Override
	public TaskIterator createTaskIterator() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean isReady() {
		// TODO Auto-generated method stub
		return false;
	}

}
