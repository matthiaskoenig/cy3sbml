package org.cy3sbml.biomodel;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;
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
     * Runs a BioModels search. The result is unsuccessful if the search failed.
     */
    public BiomodelsQueryResult performSearchQuery(String query) {
        // only the first result page is read (#402)
        URI uri = base.resolve("search?query=" + URLEncoder.encode(query, StandardCharsets.UTF_8) + "&format=json");
        return new BiomodelsQueryResult(http.fetchText(uri).value().orElse(null));
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

    private static String encode(String pathSegment) {
        return URLEncoder.encode(pathSegment, StandardCharsets.UTF_8);
    }
}
