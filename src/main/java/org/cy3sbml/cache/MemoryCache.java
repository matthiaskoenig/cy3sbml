package org.cy3sbml.cache;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

/** Thread-safe, size-bounded least-recently-used cache for web service lookups. */
public final class MemoryCache<K, V> {
    private final Map<K, V> map;

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
        synchronized (map) {
            V value = map.get(key);
            if (value != null) {
                return Optional.of(value);
            }
        }
        Optional<V> loaded = loader.apply(key);
        loaded.ifPresent(v -> {
            synchronized (map) {
                map.put(key, v);
            }
        });
        return loaded;
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
