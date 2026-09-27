package org.cy3sbml.cache;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
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

    @Test
    void concurrentRequestsForSameKeyLoadOnce() throws Exception {
        assertConcurrentRequestsShareOneLoad(FetchResult.found("v"), Optional.of("v"));
    }

    @Test
    void concurrentRequestsForSameMissingKeyLoadOnce() throws Exception {
        assertConcurrentRequestsShareOneLoad(FetchResult.notFound(), Optional.empty());
    }

    private static void assertConcurrentRequestsShareOneLoad(FetchResult<String> result, Optional<String> expected)
            throws Exception {
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
                        return result;
                    });
                }));
            }
            start.countDown();
            for (Future<Optional<String>> future : results) {
                assertEquals(expected, future.get(10, TimeUnit.SECONDS));
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
        assertEquals(Optional.of("x"), cache.get("k", k -> FetchResult.found("x")));
    }

    private static FetchResult<String> awaitBoth(CountDownLatch latch, String key) {
        latch.countDown();
        try {
            assertTrue(latch.await(5, TimeUnit.SECONDS), "loads of different keys were serialized");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);
        }
        return FetchResult.found(key);
    }

    private static void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
