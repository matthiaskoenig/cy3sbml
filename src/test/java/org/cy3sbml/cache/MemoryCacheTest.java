package org.cy3sbml.cache;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class MemoryCacheTest {
    @Test
    void loadsOnceAndCachesPresentValues() {
        var cache = new MemoryCache<String, String>(10);
        var calls = new AtomicInteger();
        assertEquals(Optional.of("a"), cache.get("k", k -> {
            calls.incrementAndGet();
            return Optional.of("a");
        }));
        assertEquals(Optional.of("a"), cache.get("k", k -> {
            calls.incrementAndGet();
            return Optional.of("b");
        }));
        assertEquals(1, calls.get());
    }

    @Test
    void doesNotCacheEmptyResults() {
        var cache = new MemoryCache<String, String>(10);
        assertTrue(cache.get("k", k -> Optional.empty()).isEmpty());
        assertEquals(Optional.of("x"), cache.get("k", k -> Optional.of("x")));
    }

    @Test
    void evictsLeastRecentlyUsed() {
        var cache = new MemoryCache<Integer, Integer>(2);
        cache.get(1, Optional::of);
        cache.get(2, Optional::of);
        cache.get(1, Optional::of);
        cache.get(3, Optional::of);
        assertEquals(2, cache.size());
        assertEquals(Optional.of(-2), cache.get(2, k -> Optional.of(-2)));
    }
}
