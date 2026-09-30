package org.cy3sbml.biomodel;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The BioModels search of the dialog: the models found by a search or parsed from text,
 * and their details.
 * <p>
 * {@link #search} and {@link #getDetails} access the BioModels web service and must not
 * run on the Swing event dispatch thread. They return immutable results, which the dialog
 * renders with {@link BiomodelsHtml} on the event dispatch thread.
 */
public class SearchBioModel {
    private static final Logger logger = LoggerFactory.getLogger(SearchBioModel.class);

    /** Number of model details requested at the same time. */
    static final int DETAILS_BATCH_SIZE = 10;

    private final BiomodelsQuery biomodelsQuery;

    /**
     * The models of a search or of parsed ids.
     *
     * @param searchContent the search terms and mode
     * @param matches the number of models matching the search, at least the number of model ids
     * @param modelIds the model ids, in the order of the search response
     * @param summaries the information of the search response by model id, empty for parsed ids
     */
    public record Result(
            SearchContent searchContent, int matches, List<String> modelIds, Map<String, BiomodelSummary> summaries) {
        public Result {
            modelIds = List.copyOf(modelIds);
            summaries = Map.copyOf(summaries);
            matches = Math.max(matches, modelIds.size());
        }

        /** Returns true if the models were parsed from text, not searched. */
        public boolean isParsed() {
            return SearchContent.PARSED_IDS.equals(searchContent.getSearchMode());
        }
    }

    /** Creates the search with the given BioModels queries. */
    public SearchBioModel(BiomodelsQuery biomodelsQuery) {
        this.biomodelsQuery = biomodelsQuery;
    }

    /**
     * Searches BioModels with the search terms of the given content, combined with its
     * search mode (AND, OR).
     *
     * @throws IOException if the search failed, e.g. because BioModels could not be reached
     */
    public Result search(SearchContent content) throws IOException {
        if (!content.hasNames()) {
            return new Result(content, 0, List.of(), Map.of());
        }
        String query = String.join(" " + content.getSearchMode() + " ", content.getNames());
        BiomodelsSearchResult searchResult = biomodelsQuery.search(query);
        Map<String, BiomodelSummary> summaries = new LinkedHashMap<>();
        for (BiomodelSummary summary : searchResult.models()) {
            summaries.put(summary.id(), summary);
        }
        logger.info("BioModels search '{}' found {} models, read {}", query, searchResult.matches(), summaries.size());
        return new Result(content, searchResult.matches(), List.copyOf(summaries.keySet()), summaries);
    }

    /**
     * Returns the result for the given model ids, e.g. parsed from text. Does not access
     * the web service.
     */
    public static Result fromIds(Collection<String> modelIds) {
        return new Result(
                new SearchContent(Map.of(SearchContent.CONTENT_MODE, SearchContent.PARSED_IDS)),
                modelIds.size(),
                List.copyOf(modelIds),
                Map.of());
    }

    /**
     * Returns the details of the given models by id, in the order of the ids. Models whose
     * lookup fails are left out.
     */
    public Map<String, Biomodel> getDetails(Collection<String> modelIds) {
        return getDetails(modelIds, biomodelsQuery::performBiomodelQuery);
    }

    /**
     * Looks up the details of a model.
     */
    @FunctionalInterface
    interface BiomodelLookup {
        CompletableFuture<Biomodel> query(String biomodelId);
    }

    static Map<String, Biomodel> getDetails(Collection<String> modelIds, BiomodelLookup lookup) {
        List<String> ids = new ArrayList<>(modelIds);
        Map<String, Biomodel> biomodels = new LinkedHashMap<>();
        // request the details in batches, so selecting many models does not flood BioModels
        for (int start = 0; start < ids.size(); start += DETAILS_BATCH_SIZE) {
            if (Thread.currentThread().isInterrupted()) {
                break;
            }
            Map<String, CompletableFuture<Biomodel>> futures = new LinkedHashMap<>();
            for (String id : ids.subList(start, Math.min(start + DETAILS_BATCH_SIZE, ids.size()))) {
                futures.put(id, lookup.query(id));
            }
            for (Map.Entry<String, CompletableFuture<Biomodel>> entry : futures.entrySet()) {
                try {
                    biomodels.put(entry.getKey(), entry.getValue().join());
                } catch (CompletionException | CancellationException e) {
                    logger.warn("Could not query BioModel {}, skipping it: {}", entry.getKey(), e.getMessage());
                }
            }
        }
        return biomodels;
    }
}
