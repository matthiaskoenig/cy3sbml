package org.cy3sbml.biomodel;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The BioModels search of the dialog: the model ids found by a search or parsed from
 * text, and their information.
 * <p>
 * {@link #search} and {@link #getInformation} access the BioModels web service and must
 * not run on the Swing event dispatch thread. They return immutable results, which the
 * dialog renders with {@link #getHTMLInformation} on the event dispatch thread.
 */
public class SearchBioModel {
    private static final Logger logger = LoggerFactory.getLogger(SearchBioModel.class);

    private final BiomodelsQuery biomodelsQuery;

    /**
     * The result of a search: the search content, the model ids, and the information
     * of the models whose lookup succeeded (by id, in the order of the ids).
     */
    public record Result(SearchContent searchContent, List<String> modelIds, Map<String, Biomodel> biomodels) {
        public Result {
            modelIds = List.copyOf(modelIds);
            biomodels = Collections.unmodifiableMap(new LinkedHashMap<>(biomodels));
        }
    }

    public SearchBioModel(BiomodelsQuery biomodelsQuery) {
        this.biomodelsQuery = biomodelsQuery;
    }

    /**
     * Searches BioModels with the search terms of the given content, combined with its
     * search mode (AND, OR), and gets the information of the found models.
     *
     * @throws IOException if the search failed, e.g. because BioModels could not be reached
     */
    public Result search(SearchContent content) throws IOException {
        List<String> modelIds = new ArrayList<>();
        if (content.hasNames()) {
            String query = String.join(" " + content.getSearchMode() + " ", content.getNames());
            BiomodelsQueryResult result = biomodelsQuery.performSearchQuery(query);
            if (!result.success()) {
                throw new IOException("The BioModels search failed for: " + query);
            }
            modelIds.addAll(result.getBiomodelIdsFromSearch());
        }
        logger.info("BioModels search '{}' found: {}", content.namesToString(" "), modelIds);
        return getInformation(content, modelIds);
    }

    /**
     * Gets the information of the given model ids, e.g. parsed from text.
     */
    public Result getInformation(Collection<String> modelIds) {
        return getInformation(
                new SearchContent(Map.of(SearchContent.CONTENT_MODE, SearchContent.PARSED_IDS)), modelIds);
    }

    private Result getInformation(SearchContent content, Collection<String> modelIds) {
        return new Result(
                content, List.copyOf(modelIds), BiomodelsQueryResult.getBiomodelsFromIds(modelIds, biomodelsQuery));
    }

    /**
     * Returns the HTML information of the given search result, highlighting the
     * selected models. Does not access the web service.
     */
    public static String getHTMLInformation(Result result, List<String> selectedModelIds) {
        String info = String.format(
                "<h2>%d BioModels found for </h2><hr>", result.modelIds().size());
        info += result.searchContent().toHTML();
        info += "<hr>";
        info += BioModelInterfaceTools.getHTMLInformationForSimpleModels(
                result.biomodels(), result.modelIds(), selectedModelIds);
        return BioModelDialogText.getString(info);
    }
}
