package org.cy3sbml.chebi;

import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
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

    /** Counts HTTP calls, to assert that {@code html()} caches instead of re-fetching. */
    private static class CountingFixture extends HttpJson {
        final AtomicInteger getCalls = new AtomicInteger();
        final AtomicInteger textCalls = new AtomicInteger();

        CountingFixture() {
            super(null, new ObjectMapper());
        }

        @Override
        public Optional<JsonNode> get(URI uri) {
            getCalls.incrementAndGet();
            try (var in = ChebiAccessTest.class.getResourceAsStream("/chebi/15422.json")) {
                return in == null ? Optional.empty() : Optional.of(new ObjectMapper().readTree(in));
            } catch (java.io.IOException e) {
                return Optional.empty();
            }
        }

        @Override
        public Optional<String> getText(URI uri) {
            textCalls.incrementAndGet();
            return Optional.of("<svg>structure</svg>");
        }
    }

    /**
     * Fails both the compound and structure lookups while {@link #failing} is
     * true, and counts calls, to assert that a failed lookup is retried
     * rather than cached.
     */
    private static class FlakyFixture extends HttpJson {
        final AtomicInteger getCalls = new AtomicInteger();
        final AtomicInteger textCalls = new AtomicInteger();
        volatile boolean failing = true;

        FlakyFixture() {
            super(null, new ObjectMapper());
        }

        @Override
        public Optional<JsonNode> get(URI uri) {
            getCalls.incrementAndGet();
            if (failing) {
                return Optional.empty();
            }
            try (var in = ChebiAccessTest.class.getResourceAsStream("/chebi/15422.json")) {
                return in == null ? Optional.empty() : Optional.of(new ObjectMapper().readTree(in));
            } catch (java.io.IOException e) {
                return Optional.empty();
            }
        }

        @Override
        public Optional<String> getText(URI uri) {
            textCalls.incrementAndGet();
            return failing ? Optional.empty() : Optional.of("<svg>structure</svg>");
        }
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
    void htmlUsesCache() {
        CountingFixture counting = new CountingFixture();
        ChebiAccess access = new ChebiAccess(counting);

        String first = access.html("CHEBI:15422");
        assertTrue(first.contains("Formula"));
        assertTrue(first.contains("C10H16N5O13P3"));
        assertTrue(first.contains("<svg>structure</svg>"));

        String second = access.html("CHEBI:15422");
        assertEquals(first, second);

        assertEquals(1, counting.getCalls.get());
        assertEquals(1, counting.textCalls.get());
    }

    @Test
    void htmlRetriesAfterFailure() {
        FlakyFixture flaky = new FlakyFixture();
        ChebiAccess access = new ChebiAccess(flaky);

        String degraded = access.html("CHEBI:15422");
        assertEquals("", degraded);
        assertEquals(1, flaky.getCalls.get());
        assertEquals(1, flaky.textCalls.get());

        flaky.failing = false;

        String full = access.html("CHEBI:15422");
        assertTrue(full.contains("Formula"));
        assertTrue(full.contains("C10H16N5O13P3"));
        assertTrue(full.contains("<svg>structure</svg>"));
        assertEquals(2, flaky.getCalls.get());
        assertEquals(2, flaky.textCalls.get());
    }

    @Test
    @Tag("network")
    void liveLookup() {
        var access = new ChebiAccess(HttpJson.createDefault());
        assertEquals("ATP", access.compound("CHEBI:15422").orElseThrow().name());
    }
}
