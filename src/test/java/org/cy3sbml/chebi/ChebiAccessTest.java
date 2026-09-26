package org.cy3sbml.chebi;

import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.util.Optional;
import org.cy3sbml.util.HttpJson;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

class ChebiAccessTest {
    private static HttpJson fixture(String resource) {
        return new HttpJson(null, new ObjectMapper()) {
            @Override
            public Optional<com.fasterxml.jackson.databind.JsonNode> get(URI uri) {
                try (var in = ChebiAccessTest.class.getResourceAsStream(resource)) {
                    return in == null ? Optional.empty() : Optional.of(new ObjectMapper().readTree(in));
                } catch (java.io.IOException e) {
                    return Optional.empty();
                }
            }
        };
    }

    @Test
    void parsesCompound() {
        var compound = new ChebiAccess(fixture("/chebi/15422.json")).compound("CHEBI:15422").orElseThrow();
        assertEquals("ATP", compound.name());
        assertEquals("C10H16N5O13P3", compound.formula());
    }

    @Test
    void returnsEmptyOnHttpError() {
        assertTrue(new ChebiAccess(fixture("/chebi/missing.json")).compound("CHEBI:15422").isEmpty());
    }

    @Test
    @Tag("network")
    void liveLookup() {
        var access = new ChebiAccess(HttpJson.createDefault());
        assertEquals("ATP", access.compound("CHEBI:15422").orElseThrow().name());
    }
}
