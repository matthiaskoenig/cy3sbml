package org.cy3sbml.biomodel;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.Test;

class BiomodelsQueryResultTest {

    private static Biomodel biomodel(String id) {
        return new Biomodel(id, "MODEL" + id, "", id, "", "");
    }

    @Test
    void getBiomodelsFromIdsSkipsFailedLookups() throws Exception {
        Map<String, Biomodel> biomodels = BiomodelsQueryResult.getBiomodelsFromIds(
                List.of("BIOMD1", "BIOMD2", "BIOMD3"),
                id -> id.equals("BIOMD2")
                        ? CompletableFuture.failedFuture(new IllegalStateException("HTTP error for " + id))
                        : CompletableFuture.completedFuture(biomodel(id)));

        assertEquals(List.of("BIOMD1", "BIOMD3"), List.copyOf(biomodels.keySet()));
        assertEquals(biomodel("BIOMD3"), biomodels.get("BIOMD3"));
    }
}
