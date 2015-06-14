package org.cy3sbml.biomodel;

import org.cytoscape.task.internal.creation.CloneNetworkTask;
import org.cytoscape.work.TaskFactory;
import org.cytoscape.work.TaskIterator;


public class SearchBioModelTaskFactory implements TaskFactory {

	@Override
	public TaskIterator createTaskIterator() {
		
    	return new TaskIterator(2,new CloneNetworkTask(network, networkMgr, networkViewMgr, vmm, netFactory, 
    			netViewFactory, naming, appMgr, netTableMgr, rootNetMgr, groupMgr, groupFactory, renderingEngineMgr, nullNetworkViewFactory));
		
	}

	@Override
	public boolean isReady() {
		// TODO Auto-generated method stub
		return false;
	}

}
