package biomodel;

import java.util.LinkedList;
import java.util.List;

import cytoscape.task.Task;
import cytoscape.task.TaskMonitor;

public class SearchBioModelTask implements Task{
	private TaskMonitor taskMonitor;
	private SearchContent searchContent;
	private BioModelWSInterface bmInterface;
	private List<String> searchResultIds;

	public SearchBioModelTask(SearchContent searchContent, BioModelWSInterface bmInterface) {
		this.searchContent = searchContent;
		this.bmInterface = bmInterface;
	}

	public void setTaskMonitor(TaskMonitor monitor)
			throws IllegalThreadStateException {
		taskMonitor = monitor;
	}

	public void halt() {
	}

	public String getTitle() {
		return "Search Biomodels";
	}

	public List<String> getIds(){
		return searchResultIds;
	}
	
	public void run() {
		String mode = searchContent.getSearchMode();
		List<String> resultIds = new LinkedList<String>();
		List<String> ids = null;
		List<String> ids2 = null;
		
		taskMonitor.setPercentCompleted(-1);
		taskMonitor.setStatus("Searching by Name ...");
		if (searchContent.hasNames()){
			for (String name: searchContent.getNames()){
				ids = bmInterface.getBioModelIdsByName(name);
				SearchBioModel.addIdsToResultIds(ids, resultIds, mode);
			}
		}
		taskMonitor.setPercentCompleted(20);
		taskMonitor.setStatus("Searching by Person ...");
		if (searchContent.hasPersons()){
			for (String person : searchContent.getPersons()){
				ids = bmInterface.getBioModelIdsByPerson(person);
				SearchBioModel.addIdsToResultIds(ids, resultIds, mode);
			}
		}
		taskMonitor.setPercentCompleted(40);
		taskMonitor.setStatus("Searching by Publication ...");
		if (searchContent.hasPublications()){
			for (String publication: searchContent.getPublications()){
				ids = bmInterface.getBioModelIdsByPublication(publication);
				SearchBioModel.addIdsToResultIds(ids, resultIds, mode);
			}
		}
		taskMonitor.setPercentCompleted(60);
		taskMonitor.setStatus("Searching by ChEBI ...");
		if (searchContent.hasChebis()){
			for (String chebi: searchContent.getChebis()){
				ids = bmInterface.getBioModelIdsByChebi(chebi);
				ids2 = bmInterface.getBioModelIdsByChebiId(chebi);
				ids.addAll(ids2);
				SearchBioModel.addIdsToResultIds(ids, resultIds, mode);
			}	
		}
		taskMonitor.setPercentCompleted(80);
		taskMonitor.setStatus("Searching by UniProt ...");
		if (searchContent.hasUniprots()){
			for (String uniprot: searchContent.getUniprots()){
				ids = bmInterface.getBioModelIdsByUniprot(uniprot);
				ids2 = bmInterface.getBioModelIdsByUniprotId(uniprot);
				ids.addAll(ids2);
				SearchBioModel.addIdsToResultIds(ids, resultIds, mode);
			}	
		}	
		taskMonitor.setPercentCompleted(100);
		searchResultIds = resultIds;
	}
}
