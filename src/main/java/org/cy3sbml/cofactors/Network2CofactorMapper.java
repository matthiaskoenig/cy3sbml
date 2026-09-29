package org.cy3sbml.cofactors;

import java.io.Serializable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.cy3sbml.mapping.One2ManyMapping;

/**
 * The split cofactor nodes of the networks, per (sub)network SUID.
 * <p>
 * For every split cofactor node the mapper stores its clones, the position of the node
 * before the split, and for every edge of a clone the original edge of the cofactor it
 * stands for. The original nodes and edges stay in the root network, so a merge adds
 * them back.
 * <p>
 * Reached from the Cytoscape event/EDT thread, the task threads and the session
 * serialization, so every method is {@code synchronized} on the instance; collections
 * are returned as copies. The fields {@code cofactor2clone} and {@code clone2cofactor}
 * keep their names and types, so session files written by an older version still
 * deserialize; the fields added later are null in those and created on first use.
 */
public class Network2CofactorMapper implements Serializable {
    private static final long serialVersionUID = 1L;

    private Map<Long, One2ManyMapping<Long, Long>> cofactor2clone;
    private Map<Long, One2ManyMapping<Long, Long>> clone2cofactor;
    // network -> clone edge -> original edge; null in sessions of older versions
    private Map<Long, Map<Long, Long>> cloneEdge2edge;
    // network -> cofactor -> {x, y} before the split; null in sessions of older versions
    private Map<Long, Map<Long, double[]>> positions;

    public Network2CofactorMapper() {
        cofactor2clone = new HashMap<>();
        clone2cofactor = new HashMap<>();
        cloneEdge2edge = new HashMap<>();
        positions = new HashMap<>();
    }

    private Map<Long, Map<Long, Long>> cloneEdges() {
        if (cloneEdge2edge == null) {
            cloneEdge2edge = new HashMap<>();
        }
        return cloneEdge2edge;
    }

    private Map<Long, Map<Long, double[]>> positions() {
        if (positions == null) {
            positions = new HashMap<>();
        }
        return positions;
    }

    /** The networks with split cofactors. */
    public synchronized Set<Long> keySet() {
        return new HashSet<>(cofactor2clone.keySet());
    }

    /** Stores a clone of the cofactor node. */
    public synchronized void putClone(Long networkSUID, Long cofactorSUID, Long cloneSUID) {
        cofactor2clone
                .computeIfAbsent(networkSUID, k -> new One2ManyMapping<>())
                .put(cofactorSUID, cloneSUID);
        clone2cofactor
                .computeIfAbsent(networkSUID, k -> new One2ManyMapping<>())
                .put(cloneSUID, cofactorSUID);
    }

    /** The split cofactor nodes of the network. */
    public synchronized Set<Long> getCofactors(Long networkSUID) {
        One2ManyMapping<Long, Long> mapping = cofactor2clone.get(networkSUID);
        return mapping == null ? new HashSet<>() : mapping.keySet();
    }

    /** The clones of the cofactor node, empty if it is not split. */
    public synchronized Set<Long> getClones(Long networkSUID, Long cofactorSUID) {
        One2ManyMapping<Long, Long> mapping = cofactor2clone.get(networkSUID);
        return mapping == null ? new HashSet<>() : mapping.getValues(cofactorSUID);
    }

    /** The cofactor node of the clone, or null if the node is no clone. */
    public synchronized Long getCofactor(Long networkSUID, Long cloneSUID) {
        One2ManyMapping<Long, Long> mapping = clone2cofactor.get(networkSUID);
        if (mapping == null) {
            return null;
        }
        Set<Long> cofactors = mapping.getValues(cloneSUID);
        return cofactors.isEmpty() ? null : cofactors.iterator().next();
    }

    /** Stores the original edge a clone edge stands for. */
    public synchronized void putCloneEdge(Long networkSUID, Long cloneEdgeSUID, Long edgeSUID) {
        cloneEdges().computeIfAbsent(networkSUID, k -> new HashMap<>()).put(cloneEdgeSUID, edgeSUID);
    }

    /** The original edge of a clone edge, or null if the edge is no clone edge. */
    public synchronized Long getOriginalEdge(Long networkSUID, Long cloneEdgeSUID) {
        Map<Long, Long> edges = cloneEdges().get(networkSUID);
        return edges == null ? null : edges.get(cloneEdgeSUID);
    }

    /** The clone edges of the network with their original edges. */
    public synchronized Map<Long, Long> getCloneEdges(Long networkSUID) {
        Map<Long, Long> edges = cloneEdges().get(networkSUID);
        return edges == null ? new HashMap<>() : new HashMap<>(edges);
    }

    public synchronized void removeCloneEdge(Long networkSUID, Long cloneEdgeSUID) {
        Map<Long, Long> edges = cloneEdges().get(networkSUID);
        if (edges != null) {
            edges.remove(cloneEdgeSUID);
        }
    }

    /** Stores the position of the cofactor node before the split. */
    public synchronized void putPosition(Long networkSUID, Long cofactorSUID, double x, double y) {
        positions().computeIfAbsent(networkSUID, k -> new HashMap<>()).put(cofactorSUID, new double[] {x, y});
    }

    /** The position of the cofactor node before the split, or null if unknown. */
    public synchronized double[] getPosition(Long networkSUID, Long cofactorSUID) {
        Map<Long, double[]> networkPositions = positions().get(networkSUID);
        double[] position = networkPositions == null ? null : networkPositions.get(cofactorSUID);
        return position == null ? null : position.clone();
    }

    /** Removes the cofactor node with its clones and position; its clone edges are removed one by one. */
    public synchronized void removeCofactor(Long networkSUID, Long cofactorSUID) {
        One2ManyMapping<Long, Long> clones = cofactor2clone.get(networkSUID);
        if (clones == null) {
            return;
        }
        for (Long cloneSUID : clones.getValues(cofactorSUID)) {
            clone2cofactor.get(networkSUID).remove(cloneSUID);
        }
        clones.remove(cofactorSUID);
        Map<Long, double[]> networkPositions = positions().get(networkSUID);
        if (networkPositions != null) {
            networkPositions.remove(cofactorSUID);
        }
        if (clones.keySet().isEmpty()) {
            removeNetwork(networkSUID);
        }
    }

    /** Removes all entries of the network, e.g. when it is destroyed. */
    public synchronized void removeNetwork(Long networkSUID) {
        cofactor2clone.remove(networkSUID);
        clone2cofactor.remove(networkSUID);
        cloneEdges().remove(networkSUID);
        positions().remove(networkSUID);
    }

    @Override
    public synchronized String toString() {
        StringBuilder string = new StringBuilder("Network2CofactorMapper\n");
        for (Map.Entry<Long, One2ManyMapping<Long, Long>> entry : cofactor2clone.entrySet()) {
            string.append("[network: ").append(entry.getKey()).append("]\n");
            string.append(entry.getValue()).append('\n');
        }
        return string.toString();
    }
}
