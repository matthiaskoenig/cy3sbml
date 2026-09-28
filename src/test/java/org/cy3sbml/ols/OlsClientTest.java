package org.cy3sbml.ols;

import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.cy3sbml.cache.MutableClock;
import org.cy3sbml.util.FetchResult;
import org.cy3sbml.util.HttpJson;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

class OlsClientTest {
    private static final String GO_PAGE = "https://www.ebi.ac.uk/ols4/ontologies/go/terms?obo_id=GO:0042752";

    /** Answers FOUND from the given resource, or NOT_FOUND if the resource does not exist. */
    private static HttpJson fixture(String resource) {
        return new HttpJson(null, new ObjectMapper()) {
            @Override
            public FetchResult<JsonNode> fetch(URI uri) {
                try (var in = OlsClientTest.class.getResourceAsStream(resource)) {
                    return in == null ? FetchResult.notFound() : FetchResult.found(new ObjectMapper().readTree(in));
                } catch (IOException e) {
                    return FetchResult.error();
                }
            }
        };
    }

    private static HttpJson fixtureFromJson(String json) {
        return new HttpJson(null, new ObjectMapper()) {
            @Override
            public FetchResult<JsonNode> fetch(URI uri) {
                try {
                    return FetchResult.found(new ObjectMapper().readTree(json));
                } catch (IOException e) {
                    return FetchResult.error();
                }
            }
        };
    }

    @Test
    void parsesTermFromPage() {
        var term = new OlsClient(fixture("/ols/go_0042752.json"))
                .termForPage(GO_PAGE)
                .orElseThrow();
        assertEquals("regulation of circadian rhythm", term.label());
        assertEquals("go", term.ontologyName());
        assertEquals("http://purl.obolibrary.org/obo/GO_0042752", term.iri());
        assertFalse(term.descriptions().isEmpty());
    }

    @Test
    void returnsEmptyOnHttpError() {
        HttpJson erroring = new HttpJson(null, new ObjectMapper()) {
            @Override
            public FetchResult<JsonNode> fetch(URI uri) {
                return FetchResult.error();
            }
        };
        assertTrue(new OlsClient(erroring).termForPage(GO_PAGE).isEmpty());
    }

    @Test
    void returnsEmptyForNonOlsPage() {
        assertTrue(new OlsClient(fixture("/ols/go_0042752.json"))
                .termForPage("https://www.uniprot.org/uniprot/P10415")
                .isEmpty());
    }

    @Test
    void returnsEmptyWhenTermIsMissingLabel() {
        String json = """
                {"_embedded": {"terms": [
                    {"iri": "http://purl.obolibrary.org/obo/GO_0042752", "obo_id": "GO:0042752", "ontology_name": "go"}
                ]}}
                """;
        assertTrue(new OlsClient(fixtureFromJson(json)).termForPage(GO_PAGE).isEmpty());
    }

    @Test
    void cachesAStructurallyIncompleteResponseAsNotFound() {
        String json = """
                {"_embedded": {"terms": [
                    {"iri": "http://purl.obolibrary.org/obo/GO_0042752", "obo_id": "GO:0042752", "ontology_name": "go"}
                ]}}
                """;
        AtomicInteger calls = new AtomicInteger();
        HttpJson http = new HttpJson(null, new ObjectMapper()) {
            @Override
            public FetchResult<JsonNode> fetch(URI uri) {
                calls.incrementAndGet();
                return fixtureFromJson(json).fetch(uri);
            }
        };
        var client = new OlsClient(http);

        assertTrue(client.termForPage(GO_PAGE).isEmpty());
        assertTrue(client.termForPage(GO_PAGE).isEmpty());
        assertEquals(1, calls.get());
    }

    @Test
    void defaultsOntologyNameToPageOntologyWhenMissing() {
        String json = """
                {"_embedded": {"terms": [
                    {"iri": "http://purl.obolibrary.org/obo/GO_0042752", "obo_id": "GO:0042752",
                     "label": "regulation of circadian rhythm"}
                ]}}
                """;
        var term = new OlsClient(fixtureFromJson(json)).termForPage(GO_PAGE).orElseThrow();
        assertEquals("go", term.ontologyName());
    }

    @Test
    void queriesTheOntologyAndTermOfThePage() {
        List<URI> requested = new ArrayList<>();
        HttpJson http = new HttpJson(null, new ObjectMapper()) {
            @Override
            public FetchResult<JsonNode> fetch(URI uri) {
                requested.add(uri);
                return FetchResult.notFound();
            }
        };
        var client = new OlsClient(http);
        client.termForPage(GO_PAGE);
        client.termForPage("https://www.ebi.ac.uk/ols4/ontologies/ncbitaxon/terms?short_form=NCBITaxon_9606");
        client.termForPage("https://www.ebi.ac.uk/ols4/ontologies/nmrcv/terms?curie=NMR:1000003");
        client.termForPage("https://www.ebi.ac.uk/ols4/ontologies/cheminf/classes?obo_id=CHEMINF:000306");
        client.termForPage("https://www.ebi.ac.uk/ols4/ontologies/ordo/terms/"
                + "http%253A%252F%252Fwww.orpha.net%252FORDO%252FOrphanet_558");
        client.termForPage("https://www.ebi.ac.uk/ols4/ontologies/probonto/terms/"
                + "http%253A%252F%252Fwww.probonto.org%252Fontology%2523PROB_c0000005");
        assertEquals(
                List.of(
                        URI.create("https://www.ebi.ac.uk/ols4/api/ontologies/go/terms?obo_id=GO%3A0042752"),
                        URI.create(
                                "https://www.ebi.ac.uk/ols4/api/ontologies/ncbitaxon/terms?short_form=NCBITaxon_9606"),
                        // the API ignores a curie parameter, so the CURIE is queried as OBO id
                        URI.create("https://www.ebi.ac.uk/ols4/api/ontologies/nmrcv/terms?obo_id=NMR%3A1000003"),
                        URI.create("https://www.ebi.ac.uk/ols4/api/ontologies/cheminf/terms?obo_id=CHEMINF%3A000306"),
                        URI.create("https://www.ebi.ac.uk/ols4/api/ontologies/ordo/terms?iri="
                                + "http%3A%2F%2Fwww.orpha.net%2FORDO%2FOrphanet_558"),
                        URI.create("https://www.ebi.ac.uk/ols4/api/ontologies/probonto/terms?iri="
                                + "http%3A%2F%2Fwww.probonto.org%2Fontology%23PROB_c0000005")),
                requested);
    }

    @Test
    void returnsEmptyWithoutRequestForUnknownPageShapes() {
        List<URI> requested = new ArrayList<>();
        HttpJson http = new HttpJson(null, new ObjectMapper()) {
            @Override
            public FetchResult<JsonNode> fetch(URI uri) {
                requested.add(uri);
                return FetchResult.notFound();
            }
        };
        var client = new OlsClient(http);
        assertTrue(
                client.termForPage("https://www.ebi.ac.uk/ols4/ontologies/go").isEmpty());
        assertTrue(client.termForPage("https://www.ebi.ac.uk/ols4/ontologies/go/terms?iri=x")
                .isEmpty());
        assertTrue(client.termForPage("https://www.ebi.ac.uk/ols4/ontologies/go/terms/%ZZ")
                .isEmpty());
        assertTrue(requested.isEmpty());
    }

    @Test
    void returnsEmptyWhenTheFirstTermIsNotTheQueriedOne() {
        // the API lists all terms of the ontology for a parameter it does not support
        String json = """
                {"_embedded": {"terms": [
                    {"iri": "http://nmrML.org/nmrCV#BFO_0000001", "obo_id": "BFO:0000001", "label": "entity"}
                ]}}
                """;
        assertTrue(new OlsClient(fixtureFromJson(json))
                .termForPage("https://www.ebi.ac.uk/ols4/ontologies/nmrcv/terms?curie=NMR:1000003")
                .isEmpty());
    }

    @Test
    void matchesTheIriOfAnIriPage() {
        String json = """
                {"_embedded": {"terms": [
                    {"iri": "http://www.orpha.net/ORDO/Orphanet_558", "label": "Marfan syndrome",
                     "ontology_name": "ordo"}
                ]}}
                """;
        var term = new OlsClient(fixtureFromJson(json))
                .termForPage("https://www.ebi.ac.uk/ols4/ontologies/ordo/terms/"
                        + "http%253A%252F%252Fwww.orpha.net%252FORDO%252FOrphanet_558")
                .orElseThrow();
        assertEquals("Marfan syndrome", term.label());
    }

    @Test
    void cachesNotFoundThenRetriesAfterTtl() {
        List<URI> requested = new ArrayList<>();
        HttpJson http = new HttpJson(null, new ObjectMapper()) {
            @Override
            public FetchResult<JsonNode> fetch(URI uri) {
                requested.add(uri);
                return FetchResult.notFound();
            }
        };
        var clock = new MutableClock(Instant.parse("2024-01-01T00:00:00Z"));
        var client = new OlsClient(http, clock);

        assertTrue(client.termForPage(GO_PAGE).isEmpty());
        assertTrue(client.termForPage(GO_PAGE).isEmpty());
        assertEquals(1, requested.size());

        clock.advance(Duration.ofMinutes(11));
        assertTrue(client.termForPage(GO_PAGE).isEmpty());
        assertEquals(2, requested.size());
    }

    @Test
    void doesNotCacheTransportErrors() {
        AtomicInteger calls = new AtomicInteger();
        HttpJson http = new HttpJson(null, new ObjectMapper()) {
            @Override
            public FetchResult<JsonNode> fetch(URI uri) {
                calls.incrementAndGet();
                return FetchResult.error();
            }
        };
        var client = new OlsClient(http);
        assertTrue(client.termForPage(GO_PAGE).isEmpty());
        assertTrue(client.termForPage(GO_PAGE).isEmpty());
        assertEquals(2, calls.get());
    }

    @Test
    @Tag("network")
    void liveLookup() {
        var client = new OlsClient(HttpJson.createDefault());
        assertEquals(
                "regulation of circadian rhythm",
                client.termForPage(GO_PAGE).orElseThrow().label());
    }
}
