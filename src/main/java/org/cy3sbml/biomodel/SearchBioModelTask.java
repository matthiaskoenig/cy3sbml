package org.cy3sbml.biomodel;

import java.util.LinkedList;
import java.util.List;

import org.cytoscape.work.Task;
import org.cytoscape.work.TaskMonitor;


public class SearchBioModelTask implements Task{
	private SearchContent searchContent;
	private BioModelWSInterface bmInterface;
	private List<String> searchResultIds;

	public SearchBioModelTask(SearchContent searchContent, BioModelWSInterface bmInterface) {
		this.searchContent = searchContent;
		this.bmInterface = bmInterface;
	}


	public void halt() {
	}

	public String getTitle() {
		return "Search Biomodels";
	}

	public List<String> getIds(){
		return searchResultIds;
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
		if (searchContent.hasPersons()){
			for (String person : searchContent.getPersons()){
				ids = bmInterface.getBioModelIdsByPerson(person);
				SearchBioModel.addIdsToResultIds(ids, resultIds, mode);
			}
		}
		taskMonitor.setProgress(0.4);
		taskMonitor.setTitle("Searching by Publication ...");
		if (searchContent.hasPublications()){
			for (String publication: searchContent.getPublications()){
				ids = bmInterface.getBioModelIdsByPublication(publication);
				SearchBioModel.addIdsToResultIds(ids, resultIds, mode);
			}
		}
		taskMonitor.setProgress(0.6);
		taskMonitor.setTitle("Searching by ChEBI ...");
		if (searchContent.hasChebis()){
			for (String chebi: searchContent.getChebis()){
				ids = bmInterface.getBioModelIdsByChebi(chebi);
				ids2 = bmInterface.getBioModelIdsByChebiId(chebi);
				ids.addAll(ids2);
				SearchBioModel.addIdsToResultIds(ids, resultIds, mode);
			}	
		}
		taskMonitor.setProgress(0.8);
		taskMonitor.setTitle("Searching by UniProt ...");
		if (searchContent.hasUniprots()){
			for (String uniprot: searchContent.getUniprots()){
				ids = bmInterface.getBioModelIdsByUniprot(uniprot);
				ids2 = bmInterface.getBioModelIdsByUniprotId(uniprot);
				ids.addAll(ids2);
				SearchBioModel.addIdsToResultIds(ids, resultIds, mode);
			}	
		}	
		taskMonitor.setProgress(1.0);
		searchResultIds = resultIds;
	}


	@Override
	public void cancel() {
		// TODO Auto-generated method stub
		
	}
}
