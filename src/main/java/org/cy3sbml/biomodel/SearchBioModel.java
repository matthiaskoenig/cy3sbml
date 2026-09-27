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

    private final BiomodelsQuery biomodelsQuery;
    private SearchContent searchContent;

    /**
     * Accessed from the search task thread (via {@link #taskFinished}) and from the
     * dialog's panel update thread ({@link #getModelIds}, {@link #getModelId}, {@link #getSize}).
     * Volatile so a search result published by the task thread is visible to readers, and
     * always assigned an unmodifiable copy so that readers never observe a partially built list.
     */
    private volatile List<String> modelIds;

    /** Set by the search task thread, read by the dialog after the synchronous search. */
    private volatile boolean searchFailed;

    public SearchBioModel(ServiceAdapter adapter, BiomodelsQuery biomodelsQuery) {
        this.biomodelsQuery = biomodelsQuery;
        dialogTaskManager = adapter.dialogTaskManager;
        synchronousTaskManager = adapter.synchronousTaskManager;

        resetSearch();
    }

    private void resetSearch() {
        searchContent = null;
        searchFailed = false;
        modelIds = Collections.emptyList();
    }

    /**
     * Returns the current snapshot of model ids. Safe to iterate: a concurrent search
     * result replaces the field with a new list rather than mutating this one.
     */
    public List<String> getModelIds() {
        return modelIds;
    }

    /**
     * Returns the model id at the given index in the current snapshot.
     * <p>
     * Do not call this in a loop together with {@link #getSize()}: a concurrent search
     * result may replace the underlying list between the two calls, so the index could be
     * out of bounds for the list {@link #getSize()} was read from. Call
     * {@link #getModelIds()} once instead and iterate over the returned snapshot.
     */
    public String getModelId(int index) {
        return modelIds.get(index);
    }

    /**
     * Returns the number of model ids in the current snapshot.
     * <p>
     * Do not call this in a loop together with {@link #getModelId(int)}; see there.
     */
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
        SearchBioModelTaskFactory searchBioModelTaskFactory = new SearchBioModelTaskFactory(content, biomodelsQuery);
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
    public void allFinished(FinishStatus finishStatus) {
        searchFailed = finishStatus.getType() == FinishStatus.Type.FAILED;
    }

    /** Returns true if the last search failed, e.g. because BioModels could not be reached. */
    public boolean searchFailed() {
        return searchFailed;
    }

    public String getHTMLInformation(final List<String> selectedModelIds) throws IOException, InterruptedException {
        String info = getHTMLHeaderForModelSearch();

        info += BioModelInterfaceTools.getHTMLInformationForSimpleModels(biomodelsQuery, modelIds, selectedModelIds);
        return BioModelDialogText.getString(info);
    }

    private String getHTMLHeaderForModelSearch() {
        String info = String.format("<h2>%d BioModels found for </h2>" + "<hr>", getSize());
        info += searchContent.toHTML();
        info += "<hr>";
        return info;
    }
}
