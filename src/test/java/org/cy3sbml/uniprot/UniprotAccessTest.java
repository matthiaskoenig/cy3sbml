package org.cy3sbml.uniprot;

import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.cy3sbml.cache.MutableClock;
import org.cy3sbml.util.FetchResult;
import org.cy3sbml.util.HttpJson;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

class UniprotAccessTest {
    private static HttpJson fixture(String resource) {
        return new HttpJson(null, new ObjectMapper()) {
            @Override
            public FetchResult<JsonNode> fetch(URI uri) {
                try (var in = UniprotAccessTest.class.getResourceAsStream(resource)) {
                    return in == null ? FetchResult.notFound() : FetchResult.found(new ObjectMapper().readTree(in));
                } catch (IOException e) {
                    return FetchResult.error();
                }
            }
        };
    }

    @Test
    void parsesEntry() {
        var entry = new UniprotAccess(fixture("/uniprot/P10415.json"))
                .entry("P10415")
                .orElseThrow();
        assertEquals("BCL2_HUMAN", entry.uniProtId());
        assertEquals("Apoptosis regulator Bcl-2", entry.fullName());
        assertEquals("Homo sapiens", entry.scientificName());
        assertTrue(entry.geneNames().contains("BCL2"));
    }

    @Test
    void html() {
        String html = new UniprotAccess(fixture("/uniprot/P10415.json")).html("P10415");
        assertTrue(html.contains("BCL2_HUMAN"));
        assertTrue(html.contains("Homo sapiens"));
    }

    /**
     * P04406 (GAPDH / G3P_HUMAN) has an EC number, an alternative name, a
     * CATALYTIC ACTIVITY comment and a PATHWAY comment, none of which P10415
     * (BCL2, used above) has.
     */
    @Test
    void parsesEntryWithAllFields() {
        var entry = new UniprotAccess(fixture("/uniprot/P04406.json"))
                .entry("P04406")
                .orElseThrow();
        assertEquals("G3P_HUMAN", entry.uniProtId());
        assertEquals("Glyceraldehyde-3-phosphate dehydrogenase", entry.fullName());
        assertEquals(List.of("1.2.1.12"), entry.ecNumbers());
        assertEquals(List.of("Peptidyl-cysteine S-nitrosylase GAPDH"), entry.alternativeNames());
        assertFalse(entry.functionComments().isEmpty());
        assertEquals(2, entry.catalyticActivities().size());
        assertTrue(entry.catalyticActivities().get(0).contains("D-glyceraldehyde 3-phosphate + phosphate + NAD(+)"));
        assertEquals(
                List.of("Carbohydrate degradation; glycolysis; pyruvate from D-glyceraldehyde"
                        + " 3-phosphate: step 1/5"),
                entry.pathways());
    }

    @Test
    void htmlWithAllFields() {
        String html = new UniprotAccess(fixture("/uniprot/P04406.json")).html("P04406");
        assertTrue(html.contains("G3P_HUMAN"));
        assertTrue(html.contains("<b>EC</b>: 1.2.1.12"));
        assertTrue(html.contains("Peptidyl-cysteine S-nitrosylase GAPDH"));
        assertTrue(html.contains("D-glyceraldehyde 3-phosphate + phosphate + NAD(+)"));
        assertTrue(html.contains("Carbohydrate degradation; glycolysis"));
    }

    @Test
    void returnsEmptyOnHttpError() {
        HttpJson erroring = new HttpJson(null, new ObjectMapper()) {
            @Override
            public FetchResult<JsonNode> fetch(URI uri) {
                return FetchResult.error();
            }
        };
        assertTrue(new UniprotAccess(erroring).entry("P10415").isEmpty());
    }

    @Test
    void cachesAStructurallyIncompleteResponseAsNotFound() {
        String json = "{\"someOtherField\": \"x\"}"; // missing primaryAccession/uniProtkbId
        var calls = new java.util.concurrent.atomic.AtomicInteger();
        HttpJson http = new HttpJson(null, new ObjectMapper()) {
            @Override
            public FetchResult<JsonNode> fetch(URI uri) {
                calls.incrementAndGet();
                try {
                    return FetchResult.found(new ObjectMapper().readTree(json));
                } catch (IOException e) {
                    return FetchResult.error();
                }
            }
        };
        var access = new UniprotAccess(http);

        assertTrue(access.entry("P10415").isEmpty());
        assertTrue(access.entry("P10415").isEmpty());

        assertEquals(1, calls.get());
    }

    @Test
    void cachesNotFoundThenRetriesAfterTtl() {
        var calls = new java.util.concurrent.atomic.AtomicInteger();
        HttpJson http = new HttpJson(null, new ObjectMapper()) {
            @Override
            public FetchResult<JsonNode> fetch(URI uri) {
                calls.incrementAndGet();
                return FetchResult.notFound();
            }
        };
        var clock = new MutableClock(Instant.parse("2024-01-01T00:00:00Z"));
        var access = new UniprotAccess(http, clock);

        assertTrue(access.entry("P10415").isEmpty());
        assertTrue(access.entry("P10415").isEmpty());
        assertEquals(1, calls.get());

        clock.advance(Duration.ofMinutes(11));
        assertTrue(access.entry("P10415").isEmpty());
        assertEquals(2, calls.get());
    }

    @Test
    @Tag("network")
    void liveLookup() {
        var access = new UniprotAccess(HttpJson.createDefault());
        assertEquals("BCL2_HUMAN", access.entry("P10415").orElseThrow().uniProtId());
    }
}
