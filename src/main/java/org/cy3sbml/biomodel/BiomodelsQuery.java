package org.cy3sbml.biomodel;

import com.fasterxml.jackson.databind.JsonNode;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import org.cy3sbml.util.FetchResult;
import org.cy3sbml.util.FetchStatus;
import org.cy3sbml.util.HttpJson;

/**
 * Queries of the BioModels REST API (https://www.biomodels.org/docs/).
 * <p>
 * All requests go through {@link HttpJson}, which follows redirects (BioModels moved from
 * www.ebi.ac.uk/biomodels to www.biomodels.org and redirects the old addresses), uses
 * HTTP/1.1 and timeouts, and honors the Cytoscape proxy settings.
 */
public class BiomodelsQuery {
    /** Base URL of the BioModels REST API. */
    public static final URI BIOMODELS_URL = URI.create("https://www.biomodels.org/");

    /** Number of models requested per page of search results. */
    static final int PAGE_SIZE = 100;

    /** Maximum number of models read for a search. */
    public static final int MAX_RESULTS = 1000;

    private final HttpJson http;
    private final URI base;

    /**
     * Creates the queries against the BioModels REST API at the given base URL,
     * which must end with a slash.
     */
    public BiomodelsQuery(HttpJson http, URI base) {
        this.http = http;
        this.base = base;
    }

    /**
     * Creates the queries against the public BioModels REST API.
     */
    public static BiomodelsQuery createDefault() {
        return new BiomodelsQuery(HttpJson.createDefault(), BIOMODELS_URL);
    }

    /**
     * Runs a BioModels search and reads the matching models, page by page, up to
     * {@link #MAX_RESULTS} models.
     *
     * @throws IOException if the search failed, e.g. because BioModels could not be reached
     */
    public BiomodelsSearchResult search(String query) throws IOException {
        return search(query, MAX_RESULTS);
    }

    BiomodelsSearchResult search(String query, int maxResults) throws IOException {
        Map<String, BiomodelSummary> models = new LinkedHashMap<>();
        int matches = 0;
        int offset = 0;
        while (models.size() < maxResults) {
            int numResults = Math.min(PAGE_SIZE, maxResults - models.size());
            URI uri = base.resolve("search?query=" + encode(query) + "&offset=" + offset + "&numResults=" + numResults
                    + "&format=json");
            FetchResult<JsonNode> result = http.fetch(uri);
            JsonNode page = result.value().orElse(null);
            if (page == null || !page.path("models").isArray()) {
                throw new IOException("The BioModels search failed for '" + query + "': " + result.status());
            }
            matches = page.path("matches").asInt(0);
            JsonNode pageModels = page.get("models");
            int read = models.size();
            for (JsonNode model : pageModels) {
                BiomodelSummary summary = BiomodelSummary.fromJson(model);
                // a model can move to the next page if the index changes while paging
                if (summary != null && models.size() < maxResults) {
                    models.putIfAbsent(summary.id(), summary);
                }
            }
            offset += pageModels.size();
            // stop at the end, and if a page brings no new model (a service ignoring the offset)
            if (models.size() == read || offset >= matches) {
                break;
            }
        }
        return new BiomodelsSearchResult(matches, List.copyOf(models.values()));
    }

    /**
     * Gets the information for the given BioModel. The future fails if the
     * information could not be retrieved or parsed.
     */
    public CompletableFuture<Biomodel> performBiomodelQuery(String biomodelId) {
        URI uri = base.resolve(encode(biomodelId) + "?format=json");
        return http.fetchAsync(uri).thenApply(result -> {
            if (result.status() != FetchStatus.FOUND) {
                throw new IllegalStateException("Could not retrieve BioModel " + biomodelId + ": " + result.status());
            }
            return Biomodel.fromJson(result.value().orElseThrow());
        });
    }

    /**
     * Downloads the SBML file of the given BioModel into the given file.
     *
     * @throws IOException if the download failed, e.g. for an unknown id
     */
    public void downloadSBML(String biomodelId, Path file) throws IOException {
        String id = encode(biomodelId);
        http.download(base.resolve("model/download/" + id + "?filename=" + id + "_url.xml"), file);
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
