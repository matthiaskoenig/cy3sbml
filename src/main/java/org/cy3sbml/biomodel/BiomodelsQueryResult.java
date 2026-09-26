package org.cy3sbml.biomodel;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

/**
 * Result of the given web service query.
 */
public class BiomodelsQueryResult {

    private final String query;
    private final Integer status;
    private final String json;

    public BiomodelsQueryResult(final String query, Integer status, String json) {
        this.query = query;
        this.status = status;
        this.json = json;
    }

    /**
     * Returns true if the request was successful.
     */
    public boolean success() {
        return (status == 200);
    }

    public String getQuery() {
        return query;
    }

    public Integer getStatus() {
        return status;
    }

    public String getJSON() {
        return json;
    }

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private JsonNode getJSONObject() {
        if (json == null) {
            return null;
        }
        try {
            return MAPPER.readTree(json);
        } catch (IOException e) {
            return null;
        }
    }

    /**
     * Parses the Biomodel information from a search query.
     */
    public List<String> getBiomodelIdsFromSearch() {
        JsonNode jsonObject = getJSONObject();
        List<String> biomodelIds = new ArrayList<>();
        if (jsonObject != null) {

            // get biomodel identifiers, skipping any model without an id
            JsonNode array = jsonObject.path("models");
            for (JsonNode model : array) {
                String biomodelId = model.path("id").asText(null);
                if (biomodelId != null) {
                    biomodelIds.add(biomodelId);
                }
            }
        }
        return biomodelIds;
    }

    /**
     * Returns biomodel information for given biomodel ids
     */
    public static List<Biomodel> getBiomodelsFromIds(Iterable<String> biomodelIds)
            throws IOException, InterruptedException, ExecutionException {

        ArrayList<Biomodel> biomodels;
        List<CompletableFuture<Biomodel>> futures = new ArrayList<>();
        for (String biomodelId : biomodelIds) {
            CompletableFuture<Biomodel> future = BiomodelsQuery.performBiomodelQuery(biomodelId);
            futures.add(future);
        }
        CompletableFuture<Void> allFutures = CompletableFuture.allOf(futures.toArray(new CompletableFuture<?>[0]));
        biomodels = allFutures
                .thenApply(v -> futures.stream()
                        .map(future -> {
                            try {
                                return future.join(); // Get each Biomodel
                            } catch (Exception e) {
                                System.err.println("Skipping failed model: " + e.getMessage());
                                return null; // or handle errors differently
                            }
                        })
                        .filter(Objects::nonNull) // Remove nulls (failed requests)
                        .collect(Collectors.toCollection(ArrayList::new)))
                .get();
        return biomodels;
    }
}
