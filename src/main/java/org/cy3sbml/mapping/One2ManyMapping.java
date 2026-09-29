package org.cy3sbml.mapping;

import java.io.Serializable;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * One to many mapping class.
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

    public One2ManyMapping() {
        map = new HashMap<>();
    }

    public synchronized boolean containsKey(T1 key) {
        return map.containsKey(key);
    }

    public synchronized Set<T1> keySet() {
        return new HashSet<>(map.keySet());
    }

    public synchronized boolean put(T1 key, T2 newValue) {
        return map.computeIfAbsent(key, k -> new HashSet<>()).add(newValue);
    }

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

    public synchronized Set<T2> getValues(T1 key) {
        HashSet<T2> values = map.get(key);
        return values == null ? new HashSet<>() : new HashSet<>(values);
    }

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

    public synchronized One2ManyMapping<T2, T1> createReverseMapping() {
        One2ManyMapping<T2, T1> reverseMapping = new One2ManyMapping<>();
        for (Map.Entry<T1, HashSet<T2>> entry : map.entrySet()) {
            for (T2 value : entry.getValue()) {
                reverseMapping.put(value, entry.getKey());
            }
        }
        return reverseMapping;
    }

    @Override
    public synchronized String toString() {
        String info = "*** OneToManyMapping ***\n";
        for (Map.Entry<T1, HashSet<T2>> entry : map.entrySet()) {
            info += String.format(
                    "%s -> %s\n", entry.getKey().toString(), entry.getValue().toString());
        }
        info += "************************";
        return info;
    }
}
