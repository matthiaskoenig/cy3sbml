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
    void parsesTermFromCurie() {
        var term = new OlsClient(fixture("/ols/go_0042752.json"))
                .term("GO:0042752")
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
        assertTrue(new OlsClient(erroring).term("GO:0042752").isEmpty());
    }

    @Test
    void returnsEmptyForNonOntologyIdentifier() {
        assertTrue(new OlsClient(fixture("/ols/go_0042752.json")).term("P10415").isEmpty());
    }

    @Test
    void returnsEmptyWhenTermIsMissingLabel() {
        String json = """
                {"_embedded": {"terms": [
                    {"iri": "http://purl.obolibrary.org/obo/GO_0042752", "ontology_name": "go"}
                ]}}
                """;
        assertTrue(new OlsClient(fixtureFromJson(json)).term("GO:0042752").isEmpty());
    }

    @Test
    void cachesAStructurallyIncompleteResponseAsNotFound() {
        String json = """
                {"_embedded": {"terms": [
                    {"iri": "http://purl.obolibrary.org/obo/GO_0042752", "ontology_name": "go"}
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

        assertTrue(client.term("GO:0042752").isEmpty());
        assertTrue(client.term("GO:0042752").isEmpty());

        // GO is already upper-case, so no retry with an upper-cased prefix happens either
        assertEquals(1, calls.get());
    }

    @Test
    void defaultsOntologyNameToPrefixWhenMissing() {
        String json = """
                {"_embedded": {"terms": [
                    {"iri": "http://purl.obolibrary.org/obo/GO_0042752", "label": "regulation of circadian rhythm"}
                ]}}
                """;
        var term = new OlsClient(fixtureFromJson(json)).term("GO:0042752").orElseThrow();
        assertEquals("go", term.ontologyName());
    }

    @Test
    void keepsTheCaseOfTheOboPrefix() {
        List<URI> requested = new ArrayList<>();
        HttpJson http = new HttpJson(null, new ObjectMapper()) {
            @Override
            public FetchResult<JsonNode> fetch(URI uri) {
                requested.add(uri);
                return FetchResult.notFound();
            }
        };
        new OlsClient(http).term("NCBITaxon_7787");
        // the case preserving lookup comes first
        assertEquals(
                URI.create("https://www.ebi.ac.uk/ols4/api/ontologies/ncbitaxon/terms?obo_id=NCBITaxon%3A7787"),
                requested.get(0));
    }

    /** Answers with the GO fixture for the given URI only, records all requested URIs. */
    private static HttpJson fixtureFor(URI answered, List<URI> requested) {
        return new HttpJson(null, new ObjectMapper()) {
            @Override
            public FetchResult<JsonNode> fetch(URI uri) {
                requested.add(uri);
                if (!uri.equals(answered)) {
                    return FetchResult.notFound();
                }
                return fixture("/ols/go_0042752.json").fetch(uri);
            }
        };
    }

    @Test
    void retriesLowercasePrefixUpperCased() {
        List<URI> requested = new ArrayList<>();
        URI upper = URI.create("https://www.ebi.ac.uk/ols4/api/ontologies/go/terms?obo_id=GO%3A0042752");
        var term = new OlsClient(fixtureFor(upper, requested)).term("go:0042752");
        assertTrue(term.isPresent());
        assertEquals(
                List.of(URI.create("https://www.ebi.ac.uk/ols4/api/ontologies/go/terms?obo_id=go%3A0042752"), upper),
                requested);
    }

    @Test
    void doesNotRetryWhenThePrefixIsUpperCase() {
        List<URI> requested = new ArrayList<>();
        var term = new OlsClient(fixtureFor(URI.create("https://example.org"), requested)).term("GO:0042752");
        assertTrue(term.isEmpty());
        assertEquals(1, requested.size());
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

        assertTrue(client.term("GO:0042752").isEmpty());
        assertTrue(client.term("GO:0042752").isEmpty());
        assertEquals(1, requested.size());

        clock.advance(Duration.ofMinutes(11));
        assertTrue(client.term("GO:0042752").isEmpty());
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
        assertTrue(client.term("GO:0042752").isEmpty());
        assertTrue(client.term("GO:0042752").isEmpty());
        assertEquals(2, calls.get());
    }

    @Test
    @Tag("network")
    void liveLookup() {
        var client = new OlsClient(HttpJson.createDefault());
        assertEquals(
                "regulation of circadian rhythm",
                client.term("GO:0042752").orElseThrow().label());
    }
}
