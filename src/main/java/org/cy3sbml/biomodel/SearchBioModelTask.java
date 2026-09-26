package org.cy3sbml.biomodel;

import java.util.ArrayList;
import java.util.List;
import org.cytoscape.work.ObservableTask;
import org.cytoscape.work.TaskMonitor;

public class SearchBioModelTask implements ObservableTask {
    private SearchContent searchContent;
    private List<String> searchResultIds;

    public SearchBioModelTask(SearchContent searchContent) {
        this.searchContent = searchContent;
    }

    @Override
    public void run(final TaskMonitor taskMonitor) throws Exception {
        String mode = searchContent.getSearchMode();
        List<String> resultIds = new ArrayList<String>();

        taskMonitor.setProgress(0.0);
        taskMonitor.setTitle("Searching by Name ...");
        if (searchContent.hasNames()) {
            List<String> names = searchContent.getNames();
            String fullName = String.join(" ", names);

            BiomodelsQueryResult searchQueryResult = BiomodelsQuery.performSearchQuery(fullName);
            assert searchQueryResult != null;
            List<String> modelIds = searchQueryResult.getBiomodelIdsFromSearch();
            // Has to be done in task

            SearchBioModel.addIdsToResultIds(modelIds, resultIds, mode);
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

    /**
     * Here the results are available when task has finished.
     */
    public List<String> getIds() {
        return searchResultIds;
    }

    @Override
    public void cancel() {
        // the search request cannot be interrupted
    }

    @SuppressWarnings("unchecked")
    @Override
    public <R> R getResults(Class<? extends R> type) {
        return (R) searchResultIds;
    }
}
