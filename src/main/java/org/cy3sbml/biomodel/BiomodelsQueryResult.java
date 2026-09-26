package org.cy3sbml.biomodel;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Result of the given web service query.
 */
public class BiomodelsQueryResult {
    private static final Logger logger = LoggerFactory.getLogger(BiomodelsQueryResult.class);

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
     * Looks up the biomodel for a given id.
     */
    @FunctionalInterface
    interface BiomodelLookup {
        CompletableFuture<Biomodel> query(String biomodelId) throws IOException, InterruptedException;
    }

    /**
     * Returns the biomodels for the given ids, in the order of the ids.
     * Biomodels whose lookup fails are skipped with a warning.
     */
    public static Map<String, Biomodel> getBiomodelsFromIds(Iterable<String> biomodelIds)
            throws IOException, InterruptedException {
        return getBiomodelsFromIds(biomodelIds, BiomodelsQuery::performBiomodelQuery);
    }

    static Map<String, Biomodel> getBiomodelsFromIds(Iterable<String> biomodelIds, BiomodelLookup lookup)
            throws IOException, InterruptedException {
        // start all lookups before waiting for the first one
        Map<String, CompletableFuture<Biomodel>> futures = new LinkedHashMap<>();
        for (String biomodelId : biomodelIds) {
            futures.put(biomodelId, lookup.query(biomodelId));
        }
        Map<String, Biomodel> biomodels = new LinkedHashMap<>();
        for (Map.Entry<String, CompletableFuture<Biomodel>> entry : futures.entrySet()) {
            try {
                biomodels.put(entry.getKey(), entry.getValue().join());
            } catch (CompletionException | CancellationException e) {
                logger.warn("Could not query biomodel {}, skipping it: {}", entry.getKey(), e.getMessage());
            }
        }
        return biomodels;
    }
}
