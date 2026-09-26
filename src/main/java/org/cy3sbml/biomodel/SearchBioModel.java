package org.cy3sbml.biomodel;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import org.cy3sbml.ServiceAdapter;
import org.cytoscape.work.FinishStatus;
import org.cytoscape.work.ObservableTask;
import org.cytoscape.work.SynchronousTaskManager;
import org.cytoscape.work.TaskIterator;
import org.cytoscape.work.TaskObserver;
import org.cytoscape.work.swing.DialogTaskManager;
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

    private SearchContent searchContent;

    /**
     * Accessed from the search task thread (via {@link #taskFinished}) and from the
     * dialog's panel update thread ({@link #getModelIds}, {@link #getModelId}, {@link #getSize}).
     * Volatile so a search result published by the task thread is visible to readers, and
     * always assigned an unmodifiable copy so that readers never observe a partially built list.
     */
    private volatile List<String> modelIds;

    public SearchBioModel(ServiceAdapter adapter) {
        dialogTaskManager = adapter.dialogTaskManager;
        synchronousTaskManager = adapter.synchronousTaskManager;

        resetSearch();
    }

    private void resetSearch() {
        searchContent = null;
        modelIds = Collections.emptyList();
    }

    public List<String> getModelIds() {
        return modelIds;
    }

    public String getModelId(int index) {
        return modelIds.get(index);
    }

    public int getSize() {
        return modelIds.size();
    }

    public void searchBioModels(SearchContent sContent) {
        resetSearch();
        searchContent = sContent;
        // The task searches the biomodel ids and sets modelIds
        // when finished
        searchModelIdsForSearchContent(searchContent);
    }

    public void getBioModelsByParsedIds(Set<String> parsedIds) throws IOException, InterruptedException {
        resetSearch();
        HashMap<String, String> map = new HashMap<String, String>();
        map.put(SearchContent.CONTENT_MODE, SearchContent.PARSED_IDS);
        // set search content
        searchContent = new SearchContent(map);

        List<String> ids = Collections.unmodifiableList(new ArrayList<>(parsedIds));
        modelIds = ids;
        logger.info("modelIds:" + ids);
        for (String id : ids) {
            logger.info(id);
        }
    }

    private void searchModelIdsForSearchContent(SearchContent content) {
        // Run the biomodel task with a taskManger

        // Necessary to init the tasks with different contents
        SearchBioModelTaskFactory searchBioModelTaskFactory = new SearchBioModelTaskFactory(content);
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
        modelIds = Collections.unmodifiableList(new ArrayList<>(ids));
        logger.info("modelIds:");
        for (String id : ids) {
            logger.info(id);
        }
    }

    @Override
    public void allFinished(FinishStatus finishStatus) {}

    public static void addIdsToResultIds(final List<String> ids, List<String> resultIds, final String mode) {
        // OR -> combine all results
        if (mode.equals(SearchContent.CONNECT_OR)) {
            resultIds.addAll(ids);
        }
        // AND -> only the combination results of all search terms
        if (mode.equals(SearchContent.CONNECT_AND)) {
            if (resultIds.size() > 0) {
                resultIds.retainAll(ids);
            } else {
                resultIds.addAll(ids);
            }
        }
    }

    public String getHTMLInformation(final List<String> selectedModelIds) throws IOException, InterruptedException {
        String info = getHTMLHeaderForModelSearch();

        info += BioModelInterfaceTools.getHTMLInformationForSimpleModels(modelIds, selectedModelIds);
        return BioModelDialogText.getString(info);
    }

    private String getHTMLHeaderForModelSearch() {
        String info = String.format("<h2>%d BioModels found for </h2>" + "<hr>", getSize());
        info += searchContent.toHTML();
        info += "<hr>";
        return info;
    }
}
