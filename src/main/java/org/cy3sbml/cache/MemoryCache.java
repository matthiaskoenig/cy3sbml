package org.cy3sbml.cache;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

/**
 * Thread-safe, size-bounded least-recently-used cache for web service lookups.
 * <p>
 * Each key is loaded at most once at a time: concurrent requests for a key that
 * is being loaded wait for that load and share its result. Loads of different
 * keys run in parallel; no lock is held while the loader runs.
 */
public final class MemoryCache<K, V> {
    private final Map<K, V> map;
    private final ConcurrentHashMap<K, CompletableFuture<Optional<V>>> inFlight = new ConcurrentHashMap<>();

    public MemoryCache(int maxEntries) {
        this.map = new LinkedHashMap<>(16, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
                return size() > maxEntries;
            }
        };
    }

    /** Returns the cached value or loads it; empty results are not cached. */
    public Optional<V> get(K key, Function<K, Optional<V>> loader) {
        Optional<V> cached = lookup(key);
        if (cached.isPresent()) {
            return cached;
        }
        CompletableFuture<Optional<V>> load = new CompletableFuture<>();
        CompletableFuture<Optional<V>> running = inFlight.putIfAbsent(key, load);
        if (running != null) {
            return await(running);
        }
        try {
            // a load that finished between the lookup and putIfAbsent has cached its value
            Optional<V> loaded = lookup(key);
            if (loaded.isEmpty()) {
                loaded = loader.apply(key);
                loaded.ifPresent(v -> {
                    synchronized (map) {
                        map.put(key, v);
                    }
                });
            }
            load.complete(loaded);
            return loaded;
        } catch (RuntimeException | Error e) {
            load.completeExceptionally(e);
            throw e;
        } finally {
            inFlight.remove(key, load);
        }
    }

    private Optional<V> lookup(K key) {
        synchronized (map) {
            return Optional.ofNullable(map.get(key));
        }
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
        synchronized (map) {
            return map.size();
        }
    }

    public void clear() {
        synchronized (map) {
            map.clear();
        }
    }
}
