package org.cy3sbml.biomodel;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import uk.ac.ebi.biomodels.ws.SimpleModel;

import org.cytoscape.work.FinishStatus;
import org.cytoscape.work.ObservableTask;
import org.cytoscape.work.SynchronousTaskManager;
import org.cytoscape.work.TaskIterator;
import org.cytoscape.work.TaskObserver;
import org.cytoscape.work.swing.DialogTaskManager;
import org.cy3sbml.ServiceAdapter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Searching BioModels.
 */
public class SearchBioModel implements TaskObserver {
	private static final Logger logger = LoggerFactory.getLogger(SearchBioModel.class);
	
	DialogTaskManager dialogTaskManager;
	@SuppressWarnings("rawtypes")
	SynchronousTaskManager synchronousTaskManager;
	
	private BioModelWSInterface bmInterface;
	private SearchContent searchContent;
	private List<String> modelIds;
	private LinkedHashMap<String, SimpleModel> simpleModels;
	
	
	public SearchBioModel(ServiceAdapter adapter){
		dialogTaskManager = adapter.dialogTaskManager;
		synchronousTaskManager = adapter.synchronousTaskManager;
		bmInterface = new BioModelWSInterface(adapter.connectionProxy);
		
		resetSearch();
	}
	
	private void resetSearch(){
		searchContent = null;
		modelIds = new LinkedList<String>();
		simpleModels = new LinkedHashMap<String, SimpleModel>();
	}
		
	public List<String> getModelIds() {
		return modelIds;
	}

	public String getModelId(int index){
		return modelIds.get(index);
	}
	
	public LinkedHashMap<String, SimpleModel> getSimpleModels(){
		return simpleModels;
	}
	public SimpleModel getSimpleModel(int index){
		return simpleModels.get(index);
	}
	
	public int getSize(){
		return modelIds.size();
	}
	
	public void searchBioModels(SearchContent sContent){
		resetSearch();
		searchContent = sContent;
		// The task searches the biomodel ids and sets modelIds and simpleModels
		// when finished
		searchModelIdsForSearchContent(searchContent);
	}
	
	public void getBioModelsByParsedIds(Set<String> parsedIds){
		resetSearch();
		HashMap<String, String> map = new HashMap<String, String>();
		map.put(SearchContent.CONTENT_MODE, SearchContent.PARSED_IDS);
		// set search content
		searchContent = new SearchContent(map);
		
		modelIds = new LinkedList<String>(parsedIds);
		logger.info("modelIds:" + modelIds.toString());
		for (String id: modelIds){
			logger.info(id);
		}
		// webservice request to get the simpleModels
		simpleModels = getSimpleModelsForSearchResult(modelIds);
	}
	
	private LinkedHashMap<String, SimpleModel> getSimpleModelsForSearchResult(List<String> idsList){
		// convert to array
		String[] ids = new String[idsList.size()];
		for (int k=0; k<idsList.size(); k++){
			ids[k] = idsList.get(k);
		}
		return bmInterface.getSimpleModelsByIds(ids);
	}
	
	private void searchModelIdsForSearchContent(SearchContent content){
		// Run the biomodel task with a taskManger
		
		// Necessary to init the tasks with different contents
		SearchBioModelTaskFactory searchBioModelTaskFactory = new SearchBioModelTaskFactory(content, bmInterface);
		TaskIterator iterator = searchBioModelTaskFactory.createTaskIterator();
	
		// execute the iterator with dialog
		synchronousTaskManager.execute(iterator, this);
	}
	
	@Override
	public void taskFinished(ObservableTask task) {
		logger.info("Task finished");
		// when finished assign the modelIds
		@SuppressWarnings("unchecked")
		List<String> ids = (List<String>) task.getResults(List.class);
		modelIds = ids;
		logger.info("modelIds:");
		for (String id: ids){
			logger.info(id);
		}
		
		simpleModels = getSimpleModelsForSearchResult(modelIds);
		// TODO: somehow notify that this is finished & update the content
		// do synchronous
	}

	@Override
	public void allFinished(FinishStatus finishStatus) {
	}
	
	
	public static void addIdsToResultIds(final List<String> ids, List<String> resultIds, final String mode){
		// OR -> combine all results
		if (mode.equals(SearchContent.CONNECT_OR)){
			resultIds.addAll(ids);
		}
		// AND -> only the combination results of all search terms	
		if (mode.equals(SearchContent.CONNECT_AND)){
			if (resultIds.size() > 0){
				resultIds.retainAll(ids);
			} else {
				resultIds.addAll(ids);
			}
		}
	}
	
	public String getHTMLInformation(final List<String> selectedModelIds){
		String info = getHTMLHeaderForModelSearch();
		info += BioModelWSInterfaceTools.getHTMLInformationForSimpleModels(simpleModels, selectedModelIds);
		return BioModelDialogText.getString(info);
	}
	
	private String getHTMLHeaderForModelSearch(){
		String info = String.format(
				"<h2>%d BioModels found for </h2>" +
				"<hr>", getSize());
		info += searchContent.toHTML();
		info += "<hr>";
		return info;
	}
		
	public String getHTMLInformationForModel(int modelIndex){
		SimpleModel simpleModel = getSimpleModel(modelIndex);
		return BioModelWSInterfaceTools.getHTMLInformationForSimpleModel(simpleModel);
	}

}
