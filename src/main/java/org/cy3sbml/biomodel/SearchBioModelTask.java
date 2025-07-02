package org.cy3sbml.biomodel;

import java.util.LinkedList;
import java.util.List;

import org.cytoscape.work.ObservableTask;
import org.cytoscape.work.TaskMonitor;


public class SearchBioModelTask implements ObservableTask{
	private SearchContent searchContent;
	private BioModelWSInterface bmInterface;
	private List<String> searchResultIds;

	public SearchBioModelTask(SearchContent searchContent, BioModelWSInterface bmInterface) {
		this.searchContent = searchContent;
		this.bmInterface = bmInterface;
	}
	
	public void run(final TaskMonitor taskMonitor) throws Exception {
		String mode = searchContent.getSearchMode();
		List<String> resultIds = new LinkedList<String>();
		List<String> ids = null;
		List<String> ids2 = null;
		
		
		taskMonitor.setProgress(0.0);
		taskMonitor.setTitle("Searching by Name ...");
		if (searchContent.hasNames()){
			for (String name: searchContent.getNames()){
				ids = bmInterface.getBioModelIdsByName(name);
				SearchBioModel.addIdsToResultIds(ids, resultIds, mode);
			}
		}
		taskMonitor.setProgress(0.2);
		taskMonitor.setTitle("Searching by Person ...");

		taskMonitor.setProgress(0.4);
		taskMonitor.setTitle("Searching by Publication ...");

		taskMonitor.setProgress(0.6);
		taskMonitor.setTitle("Searching by ChEBI ...");

		taskMonitor.setProgress(0.8);
		taskMonitor.setTitle("Searching by UniProt ...");

		taskMonitor.setProgress(1.0);
		searchResultIds = resultIds;
	}

	/** Here the results are available when task has finished. */
	public List<String> getIds(){
		return searchResultIds;
	}

		
	@Override
	public void cancel() {
		// TODO Auto-generated method stub
	}

	@SuppressWarnings("unchecked")
	@Override
	public <R> R getResults(Class<? extends R> type) {
		return (R) searchResultIds;
	}
}
