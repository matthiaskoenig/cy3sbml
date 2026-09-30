package org.cy3sbml.mapping;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * A mapping of keys to sets of values, e.g. of the cyIds of SBases to the SUIDs of their
 * nodes (one SBase can have several nodes).
 * <p>
 * Reached from both the Cytoscape event/EDT thread (writers, via SBMLManager and
 * CofactorManager) and the WebViewPanel's background panel-update thread (readers), so
 * every method is {@code synchronized} on the instance and the collection-returning
 * methods hand out defensive copies rather than live views, so a caller can iterate the
 * result without holding the lock and without racing a concurrent writer.
 * <p>
 * The synchronization is added purely on the methods; the serialized field ({@code map})
 * is unchanged so session files serialized by an older version can still be deserialized.
 */
public class One2ManyMapping<T1, T2> implements Serializable {
    private static final long serialVersionUID = 1L;
    private HashMap<T1, HashSet<T2>> map;

    /** Creates an empty mapping. */
    public One2ManyMapping() {
        map = new HashMap<>();
    }

    /** Whether the key has values. */
    public synchronized boolean containsKey(T1 key) {
        return map.containsKey(key);
    }

    /** The keys; a copy. */
    public synchronized Set<T1> keySet() {
        return new HashSet<>(map.keySet());
    }

    /**
     * Adds a value to the key.
     *
     * @return true if the key did not have the value yet
     */
    public synchronized boolean put(T1 key, T2 newValue) {
        return map.computeIfAbsent(key, k -> new HashSet<>()).add(newValue);
    }

    /** Removes the key with all its values. */
    public synchronized void remove(T1 key) {
        map.remove(key);
    }

    /** Removes one value of the key, and the key if it has no values left. */
    public synchronized void removeValue(T1 key, T2 value) {
        HashSet<T2> values = map.get(key);
        if (values != null && values.remove(value) && values.isEmpty()) {
            map.remove(key);
        }
    }

    /** The values of the key, empty if it has none; a copy. */
    public synchronized Set<T2> getValues(T1 key) {
        HashSet<T2> values = map.get(key);
        return values == null ? new HashSet<>() : new HashSet<>(values);
    }

    /** The values of all the keys; a copy. */
    public synchronized Set<T2> getValues(Collection<T1> keys) {
        Set<T2> values = new HashSet<>();
        for (T1 key : keys) {
            HashSet<T2> keyValues = map.get(key);
            if (keyValues != null) {
                values.addAll(keyValues);
            }
        }
        return values;
    }

    /** A new mapping of every value to the keys that have it. */
    public synchronized One2ManyMapping<T2, T1> createReverseMapping() {
        One2ManyMapping<T2, T1> reverseMapping = new One2ManyMapping<>();
        for (Map.Entry<T1, HashSet<T2>> entry : map.entrySet()) {
            for (T2 value : entry.getValue()) {
                reverseMapping.put(value, entry.getKey());
            }
        }
        return reverseMapping;
    }

    /** Serializes the mapping under its lock, a consistent snapshot while it is changed. */
    private synchronized void writeObject(ObjectOutputStream out) throws IOException {
        out.defaultWriteObject();
    }

    /** The keys with their values, one per line. */
    @Override
    public synchronized String toString() {
        StringBuilder info = new StringBuilder("*** OneToManyMapping ***\n");
        for (Map.Entry<T1, HashSet<T2>> entry : map.entrySet()) {
            info.append(entry.getKey()).append(" -> ").append(entry.getValue()).append('\n');
        }
        return info.append("************************").toString();
    }
}
