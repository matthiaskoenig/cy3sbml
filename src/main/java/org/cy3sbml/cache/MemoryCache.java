package org.cy3sbml.cache;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import org.cy3sbml.util.FetchResult;

/**
 * Thread-safe, size-bounded least-recently-used cache for web service lookups.
 * <p>
 * A found value is cached until evicted. A "not found" result (e.g. an HTTP
 * 404) is cached for a short TTL, so a missing term is not re-requested on
 * every render. A transport or parse error is never cached, so a transient
 * outage (e.g. while offline) recovers on the next lookup.
 * <p>
 * Each key is loaded at most once at a time: concurrent requests for a key that
 * is being loaded wait for that load and share its result. Loads of different
 * keys run in parallel; no lock is held while the loader runs.
 */
public final class MemoryCache<K, V> {
    /** Default TTL for a cached "not found" result. */
    public static final Duration DEFAULT_NOT_FOUND_TTL = Duration.ofMinutes(10);

    private final Map<K, V> values;
    private final Map<K, Instant> notFoundUntil;
    private final Duration notFoundTtl;
    private final Clock clock;
    private final ConcurrentHashMap<K, CompletableFuture<Optional<V>>> inFlight = new ConcurrentHashMap<>();

    public MemoryCache(int maxEntries) {
        this(maxEntries, DEFAULT_NOT_FOUND_TTL, Clock.systemUTC());
    }

    /** For tests: an injectable TTL and clock for the "not found" entries. */
    public MemoryCache(int maxEntries, Duration notFoundTtl, Clock clock) {
        this.notFoundTtl = notFoundTtl;
        this.clock = clock;
        this.values = boundedMap(maxEntries);
        this.notFoundUntil = boundedMap(maxEntries);
    }

    private static <K, V> Map<K, V> boundedMap(int maxEntries) {
        return new LinkedHashMap<>(16, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
                return size() > maxEntries;
            }
        };
    }

    /**
     * Returns the cached value or loads it with the given loader. A found
     * value is cached until evicted; a "not found" result is cached for a
     * short TTL; an error is not cached, so it is retried on the next call.
     */
    public Optional<V> get(K key, Function<K, FetchResult<V>> loader) {
        Optional<Optional<V>> cached = lookup(key);
        if (cached.isPresent()) {
            return cached.get();
        }
        CompletableFuture<Optional<V>> load = new CompletableFuture<>();
        CompletableFuture<Optional<V>> running = inFlight.putIfAbsent(key, load);
        if (running != null) {
            return await(running);
        }
        try {
            // a load that finished between the lookup and putIfAbsent has cached its result
            Optional<Optional<V>> loaded = lookup(key);
            Optional<V> value = loaded.isPresent() ? loaded.get() : load(key, loader);
            load.complete(value);
            return value;
        } catch (RuntimeException | Error e) {
            load.completeExceptionally(e);
            throw e;
        } finally {
            inFlight.remove(key, load);
        }
    }

    /**
     * The cached result for the key: a found value, an empty value for a
     * not-yet-expired "not found", or empty if nothing is cached.
     */
    private Optional<Optional<V>> lookup(K key) {
        synchronized (values) {
            V value = values.get(key);
            if (value != null) {
                return Optional.of(Optional.of(value));
            }
            Instant until = notFoundUntil.get(key);
            if (until != null) {
                if (clock.instant().isBefore(until)) {
                    return Optional.of(Optional.empty());
                }
                notFoundUntil.remove(key);
            }
            return Optional.empty();
        }
    }

    private Optional<V> load(K key, Function<K, FetchResult<V>> loader) {
        FetchResult<V> result = loader.apply(key);
        synchronized (values) {
            switch (result.status()) {
                case FOUND -> result.value().ifPresent(v -> values.put(key, v));
                case NOT_FOUND -> notFoundUntil.put(key, clock.instant().plus(notFoundTtl));
                case ERROR -> {
                    // not cached: retried on the next lookup
                }
            }
        }
        return result.value();
    }

    private static <T> T await(CompletableFuture<T> future) {
        try {
            return future.join();
        } catch (CompletionException e) {
            Throwable cause = e.getCause();
            if (cause instanceof RuntimeException runtime) {
                throw runtime;
            }
            if (cause instanceof Error error) {
                throw error;
            }
            throw e;
        }
    }

    public int size() {
        synchronized (values) {
            return values.size();
        }
    }

    public void clear() {
        synchronized (values) {
            values.clear();
            notFoundUntil.clear();
        }
    }
}
