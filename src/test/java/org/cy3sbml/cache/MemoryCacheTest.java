package org.cy3sbml.cache;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
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

    @Test
    void concurrentRequestsForSameKeyLoadOnce() throws Exception {
        var cache = new MemoryCache<String, String>(10);
        var calls = new AtomicInteger();
        int threads = 16;
        var start = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        try {
            List<Future<Optional<String>>> results = new ArrayList<>();
            for (int i = 0; i < threads; i++) {
                results.add(pool.submit(() -> {
                    start.await();
                    return cache.get("k", k -> {
                        calls.incrementAndGet();
                        sleep(200);
                        return Optional.of("v");
                    });
                }));
            }
            start.countDown();
            for (Future<Optional<String>> result : results) {
                assertEquals(Optional.of("v"), result.get(10, TimeUnit.SECONDS));
            }
        } finally {
            pool.shutdownNow();
        }
        assertEquals(1, calls.get());
    }

    @Test
    void differentKeysLoadInParallel() throws Exception {
        var cache = new MemoryCache<String, String>(10);
        // Each loader waits until both loaders run: this only completes when
        // loads of different keys are not serialized behind a global lock.
        var bothLoading = new CountDownLatch(2);
        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            Future<Optional<String>> a = pool.submit(() -> cache.get("a", k -> awaitBoth(bothLoading, k)));
            Future<Optional<String>> b = pool.submit(() -> cache.get("b", k -> awaitBoth(bothLoading, k)));
            assertEquals(Optional.of("a"), a.get(10, TimeUnit.SECONDS));
            assertEquals(Optional.of("b"), b.get(10, TimeUnit.SECONDS));
        } finally {
            pool.shutdownNow();
        }
    }

    @Test
    void loaderExceptionReachesCallerAndIsNotCached() {
        var cache = new MemoryCache<String, String>(10);
        assertThrows(
                IllegalStateException.class,
                () -> cache.get("k", k -> {
                    throw new IllegalStateException("boom");
                }));
        assertEquals(Optional.of("x"), cache.get("k", k -> Optional.of("x")));
    }

    private static Optional<String> awaitBoth(CountDownLatch latch, String key) {
        latch.countDown();
        try {
            assertTrue(latch.await(5, TimeUnit.SECONDS), "loads of different keys were serialized");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);
        }
        return Optional.of(key);
    }

    private static void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
