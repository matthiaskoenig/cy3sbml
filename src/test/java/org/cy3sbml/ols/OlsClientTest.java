package org.cy3sbml.ols;

import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.cy3sbml.util.HttpJson;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

class OlsClientTest {
    private static HttpJson fixture(String resource) {
        return new HttpJson(null, new ObjectMapper()) {
            @Override
            public Optional<com.fasterxml.jackson.databind.JsonNode> get(URI uri) {
                try (var in = OlsClientTest.class.getResourceAsStream(resource)) {
                    return in == null ? Optional.empty() : Optional.of(new ObjectMapper().readTree(in));
                } catch (java.io.IOException e) {
                    return Optional.empty();
                }
            }
        };
    }

    private static HttpJson fixtureFromJson(String json) {
        return new HttpJson(null, new ObjectMapper()) {
            @Override
            public Optional<com.fasterxml.jackson.databind.JsonNode> get(URI uri) {
                try {
                    return Optional.of(new ObjectMapper().readTree(json));
                } catch (java.io.IOException e) {
                    return Optional.empty();
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
        assertTrue(
                new OlsClient(fixture("/ols/missing.json")).term("GO:0042752").isEmpty());
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
            public Optional<com.fasterxml.jackson.databind.JsonNode> get(URI uri) {
                requested.add(uri);
                return Optional.empty();
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
            public Optional<com.fasterxml.jackson.databind.JsonNode> get(URI uri) {
                requested.add(uri);
                if (!uri.equals(answered)) {
                    return Optional.empty();
                }
                return fixture("/ols/go_0042752.json").get(uri);
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
    @Tag("network")
    void liveLookup() {
        var client = new OlsClient(HttpJson.createDefault());
        assertEquals(
                "regulation of circadian rhythm",
                client.term("GO:0042752").orElseThrow().label());
    }
}
