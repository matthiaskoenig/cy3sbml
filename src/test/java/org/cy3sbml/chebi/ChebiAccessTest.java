package org.cy3sbml.chebi;

import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicInteger;
import org.cy3sbml.cache.MutableClock;
import org.cy3sbml.util.FetchResult;
import org.cy3sbml.util.HttpJson;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

class ChebiAccessTest {
    private static HttpJson fixture(String resource) {
        return new HttpJson(null, new ObjectMapper()) {
            @Override
            public FetchResult<JsonNode> fetch(URI uri) {
                try (var in = ChebiAccessTest.class.getResourceAsStream(resource)) {
                    return in == null ? FetchResult.notFound() : FetchResult.found(new ObjectMapper().readTree(in));
                } catch (IOException e) {
                    return FetchResult.error();
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
        public FetchResult<JsonNode> fetch(URI uri) {
            getCalls.incrementAndGet();
            try (var in = ChebiAccessTest.class.getResourceAsStream("/chebi/15422.json")) {
                return in == null ? FetchResult.notFound() : FetchResult.found(new ObjectMapper().readTree(in));
            } catch (IOException e) {
                return FetchResult.error();
            }
        }

        @Override
        public FetchResult<String> fetchText(URI uri) {
            textCalls.incrementAndGet();
            return FetchResult.found("<svg>structure</svg>");
        }
    }

    /**
     * Fails both the compound and structure lookups (as a transport error)
     * while {@link #failing} is true, and counts calls, to assert that a
     * failed lookup is retried rather than cached.
     */
    private static class FlakyFixture extends HttpJson {
        final AtomicInteger getCalls = new AtomicInteger();
        final AtomicInteger textCalls = new AtomicInteger();
        volatile boolean failing = true;

        FlakyFixture() {
            super(null, new ObjectMapper());
        }

        @Override
        public FetchResult<JsonNode> fetch(URI uri) {
            getCalls.incrementAndGet();
            if (failing) {
                return FetchResult.error();
            }
            try (var in = ChebiAccessTest.class.getResourceAsStream("/chebi/15422.json")) {
                return in == null ? FetchResult.notFound() : FetchResult.found(new ObjectMapper().readTree(in));
            } catch (IOException e) {
                return FetchResult.error();
            }
        }

        @Override
        public FetchResult<String> fetchText(URI uri) {
            textCalls.incrementAndGet();
            return failing ? FetchResult.error() : FetchResult.found("<svg>structure</svg>");
        }
    }

    @Test
    void parsesCompound() {
        var compound = new ChebiAccess(fixture("/chebi/15422.json"))
                .compound("CHEBI:15422")
                .orElseThrow();
        assertEquals("ATP", compound.name());
        assertEquals("C10H16N5O13P3", compound.formula());
    }

    @Test
    void returnsEmptyOnHttpError() {
        HttpJson erroring = new HttpJson(null, new ObjectMapper()) {
            @Override
            public FetchResult<JsonNode> fetch(URI uri) {
                return FetchResult.error();
            }
        };
        assertTrue(new ChebiAccess(erroring).compound("CHEBI:15422").isEmpty());
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
    void cachesAStructurallyIncompleteResponseAsNotFound() {
        String json = "{\"chemical_data\": {}}"; // missing "name"
        var calls = new AtomicInteger();
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
        var access = new ChebiAccess(http);

        assertTrue(access.compound("CHEBI:15422").isEmpty());
        assertTrue(access.compound("CHEBI:15422").isEmpty());

        assertEquals(1, calls.get());
    }

    @Test
    void doesNotRetryNotFoundCompoundWithinTtl() {
        var calls = new AtomicInteger();
        HttpJson http = new HttpJson(null, new ObjectMapper()) {
            @Override
            public FetchResult<JsonNode> fetch(URI uri) {
                calls.incrementAndGet();
                return FetchResult.notFound();
            }
        };
        var clock = new MutableClock(Instant.parse("2024-01-01T00:00:00Z"));
        var access = new ChebiAccess(http, clock);

        assertTrue(access.compound("CHEBI:99999").isEmpty());
        assertTrue(access.compound("CHEBI:99999").isEmpty());
        assertEquals(1, calls.get());

        clock.advance(Duration.ofMinutes(11));
        assertTrue(access.compound("CHEBI:99999").isEmpty());
        assertEquals(2, calls.get());
    }

    @Test
    @Tag("network")
    void liveLookup() {
        var access = new ChebiAccess(HttpJson.createDefault());
        assertEquals("ATP", access.compound("CHEBI:15422").orElseThrow().name());
    }
}
