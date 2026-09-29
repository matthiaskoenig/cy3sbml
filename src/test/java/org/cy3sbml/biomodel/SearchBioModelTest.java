package org.cy3sbml.biomodel;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class SearchBioModelTest {

    private static Biomodel biomodel(String id) {
        return new Biomodel(id, "MODEL" + id, "", id, "", "");
    }

    @Test
    void getDetailsSkipsFailedLookups() {
        Map<String, Biomodel> biomodels = SearchBioModel.getDetails(
                List.of("BIOMD1", "BIOMD2", "BIOMD3"),
                id -> id.equals("BIOMD2")
                        ? CompletableFuture.failedFuture(new IllegalStateException("HTTP error for " + id))
                        : CompletableFuture.completedFuture(biomodel(id)));

        assertEquals(List.of("BIOMD1", "BIOMD3"), List.copyOf(biomodels.keySet()));
        assertEquals(biomodel("BIOMD3"), biomodels.get("BIOMD3"));
    }

    @Test
    void getDetailsLooksUpABatchOfModelsAtATime() {
        List<String> ids = new ArrayList<>();
        for (int i = 0; i < 25; i++) {
            ids.add("BIOMD" + i);
        }
        AtomicInteger running = new AtomicInteger();
        AtomicInteger maxRunning = new AtomicInteger();
        Map<String, Biomodel> biomodels = SearchBioModel.getDetails(ids, id -> {
            maxRunning.accumulateAndGet(running.incrementAndGet(), Math::max);
            return CompletableFuture.supplyAsync(() -> {
                running.decrementAndGet();
                return biomodel(id);
            });
        });

        assertEquals(ids, List.copyOf(biomodels.keySet()));
        assertTrue(maxRunning.get() <= SearchBioModel.DETAILS_BATCH_SIZE, "at most one batch: " + maxRunning.get());
    }

    @Test
    void fromIdsIsAParsedResult() {
        SearchBioModel.Result result = SearchBioModel.fromIds(List.of("BIOMD0000000070", "BIOMD0000000071"));

        assertTrue(result.isParsed());
        assertEquals(2, result.matches());
        assertEquals(List.of("BIOMD0000000070", "BIOMD0000000071"), result.modelIds());
    }
}
