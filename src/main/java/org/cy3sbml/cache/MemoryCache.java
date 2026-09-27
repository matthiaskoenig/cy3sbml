package org.cy3sbml.cache;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import org.cy3sbml.util.FetchResult;

/**
 * Thread-safe, size-bounded least-recently-used cache for web service lookups.
 * <p>
 * A found value is cached until evicted. A "not found" result (e.g. an HTTP
 * 404) is cached for a short TTL, so a missing term is not re-requested on
 * every render. A transport or parse error is never cached, so a transient
 * outage (e.g. while offline) recovers on the next lookup.
 */
public final class MemoryCache<K, V> {
    /** Default TTL for a cached "not found" result. */
    public static final Duration DEFAULT_NOT_FOUND_TTL = Duration.ofMinutes(10);

    private final Map<K, V> values;
    private final Map<K, Instant> notFoundUntil;
    private final Duration notFoundTtl;
    private final Clock clock;

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
        synchronized (values) {
            V value = values.get(key);
            if (value != null) {
                return Optional.of(value);
            }
            Instant until = notFoundUntil.get(key);
            if (until != null) {
                if (clock.instant().isBefore(until)) {
                    return Optional.empty();
                }
                notFoundUntil.remove(key);
            }
        }

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
