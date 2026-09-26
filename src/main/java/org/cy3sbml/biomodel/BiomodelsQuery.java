package org.cy3sbml.biomodel;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * UniRest based REST queries for biomodels.
 */
public class BiomodelsQuery {
    private static final Logger logger = LoggerFactory.getLogger(BiomodelsQuery.class);
    public static final String BIOMODELS_RESTFUL_URL = "https://www.ebi.ac.uk/biomodels/";
    public static final String BIOMODELS_SEARCH = "search";
    public static final String BIOMODELS_BIOMODEL = "?format=json";
    private static final ObjectMapper MAPPER = new ObjectMapper();

    /**
     * Create URI from query String.
     * <p>
     * Performs necessary replacements and sanitation of query.
     */
    public static URI uriFromQuery(String query) throws URISyntaxException {
        URI uri = new URI(BIOMODELS_RESTFUL_URL + query);
        return uri;
    }

    /**
     * Run a biomodels query.
     */
    public static BiomodelsQueryResult performSearchQuery(String query) throws IOException, InterruptedException {
        // only the first result page is read (#402)
        String url = String.format(
                "%s?query=%s&format=json",
                BIOMODELS_RESTFUL_URL + BIOMODELS_SEARCH, URLEncoder.encode(query, StandardCharsets.UTF_8));
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Accept", "application/json")
                .GET()
                .build();
        HttpResponse<InputStream> response =
                client.send(request, java.net.http.HttpResponse.BodyHandlers.ofInputStream());

        if (response != null) {
            Integer status = response.statusCode();
            String json = null;
            if (status == 200) {
                json = getStringBody(response);
            }
            return new BiomodelsQueryResult(query, status, json);
        }

        return null;
    }

    /**
     * Get information for given biomodel.
     */
    public static CompletableFuture<Biomodel> performBiomodelQuery(String biomodelId)
            throws IOException, InterruptedException {
        String query = biomodelId + "?format=json";
        String url = BIOMODELS_RESTFUL_URL + query;
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Accept", "application/json")
                .GET()
                .build();
        CompletableFuture<Biomodel> future = client.sendAsync(request, HttpResponse.BodyHandlers.ofInputStream())
                .thenApply(response -> {
                    if (response.statusCode() == 200) {
                        try {
                            String json = getStringBody(response); // Reuse your method
                            JsonNode jsonObject = MAPPER.readTree(json);
                            return new Biomodel(jsonObject);
                        } catch (IOException e) {
                            throw new UncheckedIOException("Could not parse biomodel: " + biomodelId, e);
                        }
                    } else {
                        throw new RuntimeException("HTTP error for " + biomodelId + ": " + response.statusCode());
                    }
                });

        return future;
    }

    public static String getBioModelSBMLById(String id) throws IOException, InterruptedException {

        String sbml = "";
        HttpResponse<String> sbmlResponse = getSBMLResponse(BIOMODELS_RESTFUL_URL, "model/download/", id);
        if (sbmlResponse.statusCode() == 200) {
            // The response body contains the SBML XML content
            sbml = sbmlResponse.body();

        } else {
            logger.error("Could not download SBML for {}: status code {}", id, sbmlResponse.statusCode());
        }

        return sbml;
    }

    public static HttpResponse<String> getSBMLResponse(String base, String operation, String id)
            throws IOException, InterruptedException {
        String downloadUrl = base + operation + id + "?filename=" + id + "_url.xml";

        HttpClient client = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
        HttpRequest request =
                HttpRequest.newBuilder().uri(URI.create(downloadUrl)).GET().build();

        return client.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private static String getStringBody(HttpResponse<InputStream> ioResponse) throws IOException {
        try (BufferedReader bufferedReader =
                new BufferedReader(new InputStreamReader(ioResponse.body(), StandardCharsets.UTF_8))) {
            return bufferedReader.lines().collect(Collectors.joining("\n"));
        }
    }
}
