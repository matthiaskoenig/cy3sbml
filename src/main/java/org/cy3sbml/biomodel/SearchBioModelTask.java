package org.cy3sbml.biomodel;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.cytoscape.work.ObservableTask;
import org.cytoscape.work.TaskMonitor;

public class SearchBioModelTask implements ObservableTask {
    private final SearchContent searchContent;
    private final BiomodelsQuery biomodelsQuery;
    private List<String> searchResultIds;

    public SearchBioModelTask(SearchContent searchContent, BiomodelsQuery biomodelsQuery) {
        this.searchContent = searchContent;
        this.biomodelsQuery = biomodelsQuery;
    }

    @Override
    public void run(final TaskMonitor taskMonitor) throws Exception {
        List<String> resultIds = new ArrayList<String>();

        taskMonitor.setProgress(0.0);
        taskMonitor.setTitle("Searching BioModels ...");
        if (searchContent.hasNames()) {
            // the search terms are combined with the search mode (AND, OR) in the query
            String query = String.join(" " + searchContent.getSearchMode() + " ", searchContent.getNames());

            BiomodelsQueryResult searchQueryResult = biomodelsQuery.performSearchQuery(query);
            if (!searchQueryResult.success()) {
                throw new IOException("The BioModels search failed for: " + query);
            }
            resultIds.addAll(searchQueryResult.getBiomodelIdsFromSearch());
        }
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
