package org.cy3sbml.biomodel;

import org.cy3sbml.ServiceAdapter;
import org.cytoscape.work.TaskFactory;
import org.cytoscape.work.TaskIterator;

public class LoadBioModelTaskFactory implements TaskFactory{

	private String id;
	private ServiceAdapter adapter;
	
	public LoadBioModelTaskFactory(String id, ServiceAdapter adapter) {
		this.id = id;
		this.adapter = adapter;
	}
	
	@Override
	public TaskIterator createTaskIterator() {
		BioModelWSInterface bmInterface = new BioModelWSInterface(adapter.connectionProxy);
		return new TaskIterator(new LoadBioModelTask(id, bmInterface, adapter));
	}

	@Override
	public boolean isReady() {
		// TODO Auto-generated method stub
		return false;
	}

}
