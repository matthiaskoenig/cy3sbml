package org.cy3sbml.biomodel;

import java.util.List;

/**
 * The result of a BioModels search: the number of matching models and the models read,
 * in the order of the search response.
 *
 * @param matches the number of models matching the search, at least the number of models read
 * @param models the models read, at most {@link BiomodelsQuery#MAX_RESULTS}
 */
public record BiomodelsSearchResult(int matches, List<BiomodelSummary> models) {
    public BiomodelsSearchResult {
        models = List.copyOf(models);
        matches = Math.max(matches, models.size());
    }

    /** Returns true if all matching models were read. */
    public boolean isComplete() {
        return models.size() >= matches;
    }
}
