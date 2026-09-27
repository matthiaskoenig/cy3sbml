package org.cy3sbml.cache;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import org.cy3sbml.util.FetchResult;
import org.junit.jupiter.api.Test;

class MemoryCacheTest {
    @Test
    void loadsOnceAndCachesFoundValues() {
        var cache = new MemoryCache<String, String>(10);
        var calls = new AtomicInteger();
        assertEquals(Optional.of("a"), cache.get("k", k -> {
            calls.incrementAndGet();
            return FetchResult.found("a");
        }));
        assertEquals(Optional.of("a"), cache.get("k", k -> {
            calls.incrementAndGet();
            return FetchResult.found("b");
        }));
        assertEquals(1, calls.get());
    }

    @Test
    void doesNotCacheErrors() {
        var cache = new MemoryCache<String, String>(10);
        assertTrue(cache.get("k", k -> FetchResult.<String>error()).isEmpty());
        assertEquals(Optional.of("x"), cache.get("k", k -> FetchResult.found("x")));
    }

    @Test
    void evictsLeastRecentlyUsed() {
        var cache = new MemoryCache<Integer, Integer>(2);
        cache.get(1, FetchResult::found);
        cache.get(2, FetchResult::found);
        cache.get(1, FetchResult::found);
        cache.get(3, FetchResult::found);
        assertEquals(2, cache.size());
        assertEquals(Optional.of(-2), cache.get(2, k -> FetchResult.found(-2)));
    }

    @Test
    void cachesNotFoundForTtlThenRetries() {
        var clock = new MutableClock(Instant.parse("2024-01-01T00:00:00Z"));
        var cache = new MemoryCache<String, String>(10, Duration.ofMinutes(10), clock);
        var calls = new AtomicInteger();

        assertTrue(cache.get("k", k -> {
                    calls.incrementAndGet();
                    return FetchResult.<String>notFound();
                })
                .isEmpty());
        assertEquals(1, calls.get());

        // still within the TTL: the loader is not called again
        clock.advance(Duration.ofMinutes(9));
        assertTrue(cache.get("k", k -> {
                    calls.incrementAndGet();
                    return FetchResult.found("late");
                })
                .isEmpty());
        assertEquals(1, calls.get());

        // TTL elapsed: retried, and now found
        clock.advance(Duration.ofMinutes(2));
        assertEquals(Optional.of("late"), cache.get("k", k -> {
            calls.incrementAndGet();
            return FetchResult.found("late");
        }));
        assertEquals(2, calls.get());
    }

    @Test
    void doesNotCacheErrorsAcrossCalls() {
        var cache = new MemoryCache<String, String>(10);
        var calls = new AtomicInteger();
        for (int i = 0; i < 3; i++) {
            assertTrue(cache.get("k", k -> {
                        calls.incrementAndGet();
                        return FetchResult.<String>error();
                    })
                    .isEmpty());
        }
        assertEquals(3, calls.get());
    }
}
