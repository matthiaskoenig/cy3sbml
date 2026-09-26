package org.cy3sbml.uniprot;

import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.util.Optional;
import org.cy3sbml.util.HttpJson;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

class UniprotAccessTest {
    private static HttpJson fixture(String resource) {
        return new HttpJson(null, new ObjectMapper()) {
            @Override
            public Optional<com.fasterxml.jackson.databind.JsonNode> get(URI uri) {
                try (var in = UniprotAccessTest.class.getResourceAsStream(resource)) {
                    return in == null ? Optional.empty() : Optional.of(new ObjectMapper().readTree(in));
                } catch (java.io.IOException e) {
                    return Optional.empty();
                }
            }
        };
    }

    @Test
    void parsesEntry() {
        var entry = new UniprotAccess(fixture("/uniprot/P10415.json")).entry("P10415").orElseThrow();
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

    @Test
    void returnsEmptyOnHttpError() {
        assertTrue(new UniprotAccess(fixture("/uniprot/missing.json")).entry("P10415").isEmpty());
    }

    @Test
    @Tag("network")
    void liveLookup() {
        var access = new UniprotAccess(HttpJson.createDefault());
        assertEquals("BCL2_HUMAN", access.entry("P10415").orElseThrow().uniProtId());
    }
}
