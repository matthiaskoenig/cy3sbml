package org.cy3sbml.cofactors;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.cy3sbml.SBML;
import org.cy3sbml.SBMLManager;
import org.cy3sbml.util.AttributeUtil;
import org.cytoscape.event.CyEventHelper;
import org.cytoscape.model.CyEdge;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNode;
import org.cytoscape.model.events.NetworkAboutToBeDestroyedEvent;
import org.cytoscape.model.events.NetworkAboutToBeDestroyedListener;
import org.cytoscape.model.subnetwork.CyRootNetwork;
import org.cytoscape.model.subnetwork.CySubNetwork;
import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.view.model.View;
import org.cytoscape.view.presentation.property.BasicVisualLexicon;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Splits cofactor nodes into clones and merges the clones back.
 * <p>
 * A node with N edges in a network is split into N clones with one edge each; the
 * clones copy the table values of the node and are marked by the column
 * {@link SBML#ATTR_COFACTOR_CLONE}. The node and its edges are only removed from the
 * (sub)network, they stay in the root network. A merge removes the clones and their
 * edges from the root network and adds the node and its edges again, so a merge
 * restores the network before the split. Every clone edge knows the original edge it
 * stands for ({@link Network2CofactorMapper}): an edge between two split nodes is an
 * edge between two clones, and merging one of them connects the node to the clone of
 * the other, so adjacent nodes can be split and merged in any order.
 */
public class CofactorManager implements NetworkAboutToBeDestroyedListener {
    private static final Logger logger = LoggerFactory.getLogger(CofactorManager.class);

    /** Distance of a clone from its neighbor in the network view. */
    static final double CLONE_DISTANCE = 70.0;

    /** Angle between the clones at one neighbor; with the distance, the clones do not overlap. */
    static final double CLONE_ANGLE = Math.toRadians(45);

    private final SBMLManager sbmlManager;
    private final CyEventHelper eventHelper;

    // replaced on session restore while the actions read it, see setNetwork2CofactorMapper
    private volatile Network2CofactorMapper mapper;

    public CofactorManager(SBMLManager sbmlManager, CyEventHelper eventHelper) {
        this.sbmlManager = sbmlManager;
        this.eventHelper = eventHelper;
        this.mapper = new Network2CofactorMapper();
    }

    /** The mapper, for the serialization in sessions. */
    public Network2CofactorMapper getNetwork2CofactorMapper() {
        return mapper;
    }

    /** Sets the mapper of a restored session. */
    public void setNetwork2CofactorMapper(Network2CofactorMapper mapper) {
        this.mapper = mapper;
    }

    /** Whether the node is a clone of a split cofactor node in the network. */
    public boolean isClone(CyNetwork network, CyNode node) {
        return mapper.getCofactor(network.getSUID(), node.getSUID()) != null;
    }

    /** Whether the network has split cofactor nodes. */
    public boolean hasClones(CyNetwork network) {
        return !mapper.getCofactors(network.getSUID()).isEmpty();
    }

    /**
     * Whether the node can be split: a node of the network with at least two edges that
     * is neither a clone nor a group node.
     */
    public boolean canSplit(CyNetwork network, CyNode node) {
        if (!network.containsNode(node) || isClone(network, node)) {
            return false;
        }
        if (SBML.NODETYPE_GROUP.equals(AttributeUtil.get(network, node, SBML.NODETYPE_ATTR, String.class))) {
            return false;
        }
        return network.getAdjacentEdgeList(node, CyEdge.Type.ANY).size() >= 2;
    }

    /**
     * Splits the given nodes into clones, one per edge; nodes that cannot be split
     * ({@link #canSplit}) are skipped.
     *
     * @param view the view of the network to place the clones in, or null
     * @return the clones
     */
    public List<CyNode> split(CyNetwork network, CyNetworkView view, Collection<CyNode> nodes) {
        Long networkSUID = network.getSUID();
        List<CyNode> clones = new ArrayList<>();
        Set<Long> removedEdges = new HashSet<>();
        for (CyNode node : new LinkedHashSet<>(nodes)) {
            if (!canSplit(network, node)) {
                logger.debug("Node is not split: {}", node);
                continue;
            }
            double[] position = position(view, node);
            if (position != null) {
                mapper.putPosition(networkSUID, node.getSUID(), position[0], position[1]);
            }
            clones.addAll(splitNode(network, node, removedEdges));
        }
        deleteRows(network, Set.of(), removedEdges);
        if (view != null && !clones.isEmpty()) {
            placeClones(network, view, clones);
        }
        return clones;
    }

    private List<CyNode> splitNode(CyNetwork network, CyNode node, Set<Long> removedEdges) {
        Long networkSUID = network.getSUID();
        List<CyEdge> edges = network.getAdjacentEdgeList(node, CyEdge.Type.ANY);
        List<CyNode> clones = new ArrayList<>();
        List<CyEdge> replacedCloneEdges = new ArrayList<>();
        for (CyEdge edge : new LinkedHashSet<>(edges)) {
            CyNode clone = network.addNode();
            AttributeUtil.copyNodeAttributes(network, node, clone);
            AttributeUtil.set(network, clone, SBML.ATTR_COFACTOR_CLONE, true, Boolean.class);
            sbmlManager.addNodeMapping(network, clone);
            mapper.putClone(networkSUID, node.getSUID(), clone.getSUID());
            clones.add(clone);

            CyNode source = edge.getSource().equals(node) ? clone : edge.getSource();
            CyNode target = edge.getTarget().equals(node) ? clone : edge.getTarget();
            CyEdge cloneEdge = network.addEdge(source, target, edge.isDirected());
            AttributeUtil.copyEdgeAttributes(network, edge, cloneEdge);

            // an edge to a clone of another split node is itself a clone edge
            Long original = mapper.getOriginalEdge(networkSUID, edge.getSUID());
            if (original != null) {
                mapper.removeCloneEdge(networkSUID, edge.getSUID());
                replacedCloneEdges.add(edge);
                removedEdges.add(edge.getSUID());
            }
            mapper.putCloneEdge(networkSUID, cloneEdge.getSUID(), original != null ? original : edge.getSUID());
        }
        // the node and its edges stay in the root network for the merge; the node is
        // deselected, its clones are selected instead
        network.getRow(node).set(CyNetwork.SELECTED, false);
        network.removeEdges(edges);
        network.removeNodes(List.of(node));
        rootNetwork(network).removeEdges(replacedCloneEdges);
        return clones;
    }

    /**
     * Merges the cofactor nodes of the given clones; nodes that are no clones are
     * skipped.
     *
     * @param view the view of the network to place the merged nodes in, or null
     * @return the merged cofactor nodes
     */
    public List<CyNode> merge(CyNetwork network, CyNetworkView view, Collection<CyNode> clones) {
        Set<Long> cofactors = new LinkedHashSet<>();
        for (CyNode clone : clones) {
            Long cofactor = mapper.getCofactor(network.getSUID(), clone.getSUID());
            if (cofactor != null) {
                cofactors.add(cofactor);
            }
        }
        return mergeCofactors(network, view, cofactors);
    }

    /**
     * Merges all split cofactor nodes of the network.
     *
     * @param view the view of the network to place the merged nodes in, or null
     * @return the merged cofactor nodes
     */
    public List<CyNode> mergeAll(CyNetwork network, CyNetworkView view) {
        return mergeCofactors(network, view, mapper.getCofactors(network.getSUID()));
    }

    private List<CyNode> mergeCofactors(CyNetwork network, CyNetworkView view, Collection<Long> cofactorSUIDs) {
        List<CyNode> merged = new ArrayList<>();
        Map<CyNode, double[]> positions = new HashMap<>();
        Set<Long> removedNodes = new HashSet<>();
        Set<Long> removedEdges = new HashSet<>();
        for (Long cofactorSUID : cofactorSUIDs) {
            CyNode cofactor = rootNetwork(network).getNode(cofactorSUID);
            if (cofactor == null) {
                logger.warn("Split cofactor node {} no longer exists, its clones stay", cofactorSUID);
                mapper.removeCofactor(network.getSUID(), cofactorSUID);
                continue;
            }
            double[] position = mapper.getPosition(network.getSUID(), cofactorSUID);
            if (position == null) {
                position = centroid(view, clones(network, cofactorSUID));
            }
            if (position != null) {
                positions.put(cofactor, position);
            }
            mergeNode(network, cofactor, removedNodes, removedEdges);
            merged.add(cofactor);
        }
        deleteRows(network, removedNodes, removedEdges);
        if (view != null && !positions.isEmpty()) {
            eventHelper.flushPayloadEvents();
            for (Map.Entry<CyNode, double[]> entry : positions.entrySet()) {
                setPosition(view, entry.getKey(), entry.getValue());
            }
        }
        return merged;
    }

    private void mergeNode(CyNetwork network, CyNode cofactor, Set<Long> removedNodes, Set<Long> removedEdges) {
        Long networkSUID = network.getSUID();
        CySubNetwork subnetwork = (CySubNetwork) network;
        List<CyNode> clones = clones(network, cofactor.getSUID());

        subnetwork.addNode(cofactor);
        Set<CyEdge> cloneEdges = new LinkedHashSet<>();
        for (CyNode clone : clones) {
            cloneEdges.addAll(network.getAdjacentEdgeList(clone, CyEdge.Type.ANY));
        }
        Set<CyNode> cloneSet = new HashSet<>(clones);
        for (CyEdge edge : cloneEdges) {
            removedEdges.add(edge.getSUID());
            // the other end of the edge after the merge
            CyNode source = cloneSet.contains(edge.getSource()) ? cofactor : edge.getSource();
            CyNode target = cloneSet.contains(edge.getTarget()) ? cofactor : edge.getTarget();
            CyNode other = cloneSet.contains(edge.getSource()) ? target : source;

            Long originalSUID = mapper.getOriginalEdge(networkSUID, edge.getSUID());
            CyEdge original = originalSUID != null
                    ? rootNetwork(network).getEdge(originalSUID)
                    : findOriginalEdge(network, cofactor, other);
            mapper.removeCloneEdge(networkSUID, edge.getSUID());
            if (original != null && connects(original, cofactor, other)) {
                subnetwork.addEdge(original);
            } else {
                // the other end is the clone of another split node
                CyEdge cloneEdge = network.addEdge(source, target, edge.isDirected());
                AttributeUtil.copyEdgeAttributes(network, edge, cloneEdge);
                if (original != null) {
                    mapper.putCloneEdge(networkSUID, cloneEdge.getSUID(), original.getSUID());
                }
            }
        }
        for (CyNode clone : clones) {
            sbmlManager.removeNodeMapping(network, clone);
            removedNodes.add(clone.getSUID());
        }
        // the clones and their edges are removed completely
        network.removeNodes(clones);
        rootNetwork(network).removeNodes(clones);
        mapper.removeCofactor(networkSUID, cofactor.getSUID());
        network.getRow(cofactor).set(CyNetwork.SELECTED, true);
    }

    /**
     * Deletes the rows of the removed clones and clone edges from the tables of the
     * network. The rows of nodes and edges removed from a subnetwork are kept (and Cytoscape
     * sets their selected state when the removal events are sent), so they are deleted
     * after the events.
     */
    private void deleteRows(CyNetwork network, Set<Long> nodeSUIDs, Set<Long> edgeSUIDs) {
        if (nodeSUIDs.isEmpty() && edgeSUIDs.isEmpty()) {
            return;
        }
        eventHelper.flushPayloadEvents();
        for (String table : List.of(CyNetwork.LOCAL_ATTRS, CyNetwork.HIDDEN_ATTRS)) {
            network.getTable(CyNode.class, table).deleteRows(nodeSUIDs);
            network.getTable(CyEdge.class, table).deleteRows(edgeSUIDs);
        }
    }

    /** The clones of the cofactor node in the network. */
    private List<CyNode> clones(CyNetwork network, Long cofactorSUID) {
        return mapper.getClones(network.getSUID(), cofactorSUID).stream()
                .map(network::getNode)
                .filter(Objects::nonNull)
                .toList();
    }

    /**
     * The edge of the cofactor node in the root network to the node (or the cofactor
     * node of the clone) that is not in the network. For the clone edges of sessions of
     * older versions, which did not store the original edges.
     */
    private CyEdge findOriginalEdge(CyNetwork network, CyNode cofactor, CyNode other) {
        Long otherCofactor = mapper.getCofactor(network.getSUID(), other.getSUID());
        CyNode end = otherCofactor != null ? rootNetwork(network).getNode(otherCofactor) : other;
        for (CyEdge edge : rootNetwork(network).getAdjacentEdgeList(cofactor, CyEdge.Type.ANY)) {
            if (!network.containsEdge(edge) && connects(edge, cofactor, end)) {
                return edge;
            }
        }
        return null;
    }

    private static boolean connects(CyEdge edge, CyNode node, CyNode other) {
        return (edge.getSource().equals(node) && edge.getTarget().equals(other))
                || (edge.getSource().equals(other) && edge.getTarget().equals(node));
    }

    private static CyRootNetwork rootNetwork(CyNetwork network) {
        return ((CySubNetwork) network).getRootNetwork();
    }

    // ------------------------------------------------------------
    // Positions
    // ------------------------------------------------------------

    /**
     * Places every clone {@link #CLONE_DISTANCE} away from its neighbor, in the direction of
     * the position of the split node; the clones at one neighbor, also of different split
     * nodes, are spread to at least {@link #CLONE_ANGLE} apart, so they do not overlap.
     */
    private void placeClones(CyNetwork network, CyNetworkView view, List<CyNode> clones) {
        eventHelper.flushPayloadEvents();
        Set<CyNode> newClones = new HashSet<>(clones);
        Map<CyNode, double[]> centers = new HashMap<>();
        Map<CyNode, List<CyNode>> clonesAtNeighbor = new LinkedHashMap<>();
        Map<CyNode, Double> directions = new HashMap<>();
        for (CyNode clone : clones) {
            Long cofactor = mapper.getCofactor(network.getSUID(), clone.getSUID());
            double[] origin = mapper.getPosition(network.getSUID(), cofactor);
            List<CyNode> neighbors = network.getNeighborList(clone, CyEdge.Type.ANY);
            if (origin == null || neighbors.isEmpty()) {
                continue;
            }
            CyNode neighbor = neighbors.get(0);
            // a new clone of another split node is not placed yet, it starts at its node
            double[] center = newClones.contains(neighbor) || neighbor.equals(clone)
                    ? mapper.getPosition(network.getSUID(), mapper.getCofactor(network.getSUID(), neighbor.getSUID()))
                    : position(view, neighbor);
            if (center == null) {
                setPosition(view, clone, origin);
                continue;
            }
            centers.put(neighbor, center);
            clonesAtNeighbor.computeIfAbsent(neighbor, k -> new ArrayList<>()).add(clone);
            directions.put(clone, direction(center, origin));
        }
        for (Map.Entry<CyNode, List<CyNode>> entry : clonesAtNeighbor.entrySet()) {
            double[] center = centers.get(entry.getKey());
            List<CyNode> atNeighbor = entry.getValue();
            double[] angles = new double[atNeighbor.size()];
            for (int k = 0; k < angles.length; k++) {
                angles[k] = directions.get(atNeighbor.get(k));
            }
            double[] spread = spreadAngles(angles, CLONE_ANGLE);
            for (int k = 0; k < spread.length; k++) {
                setPosition(view, atNeighbor.get(k), new double[] {
                    center[0] + CLONE_DISTANCE * Math.cos(spread[k]), center[1] + CLONE_DISTANCE * Math.sin(spread[k])
                });
            }
        }
    }

    /** The direction (angle) from center to origin, 0 if both are the same point. */
    static double direction(double[] center, double[] origin) {
        double dx = origin[0] - center[0];
        double dy = origin[1] - center[1];
        return dx == 0 && dy == 0 ? 0.0 : Math.atan2(dy, dx);
    }

    /**
     * Spreads the angles (radians) of the clones at one neighbor to at least the separation
     * apart, keeping their order around the neighbor and their mean direction; angles that
     * are already far enough apart are kept.
     *
     * @return the spread angles, in the order of the given angles
     */
    static double[] spreadAngles(double[] angles, double separation) {
        int n = angles.length;
        if (n < 2) {
            return angles.clone();
        }
        double sep = Math.min(separation, 2 * Math.PI / n);
        Integer[] order = new Integer[n];
        double[] normalized = new double[n];
        for (int k = 0; k < n; k++) {
            order[k] = k;
            normalized[k] = ((angles[k] % (2 * Math.PI)) + 2 * Math.PI) % (2 * Math.PI);
        }
        Arrays.sort(order, (a, b) -> Double.compare(normalized[a], normalized[b]));
        // start after the largest gap, so the angles increase without a wrap around
        int start = 0;
        double largestGap = -1;
        for (int k = 0; k < n; k++) {
            double next = k + 1 < n ? normalized[order[k + 1]] : normalized[order[0]] + 2 * Math.PI;
            double gap = next - normalized[order[k]];
            if (gap > largestGap) {
                largestGap = gap;
                start = (k + 1) % n;
            }
        }
        double[] unwrapped = new double[n];
        for (int k = 0; k < n; k++) {
            int index = order[(start + k) % n];
            unwrapped[k] = normalized[index] + (start + k >= n ? 2 * Math.PI : 0);
        }
        double[] spread = unwrapped.clone();
        for (int k = 1; k < n; k++) {
            spread[k] = Math.max(spread[k], spread[k - 1] + sep);
        }
        // keep the mean direction of the clones
        double shift = 0;
        for (int k = 0; k < n; k++) {
            shift += (unwrapped[k] - spread[k]) / n;
        }
        double[] result = new double[n];
        for (int k = 0; k < n; k++) {
            result[order[(start + k) % n]] = spread[k] + shift;
        }
        return result;
    }

    private static double[] centroid(CyNetworkView view, List<CyNode> nodes) {
        double x = 0;
        double y = 0;
        int count = 0;
        for (CyNode node : nodes) {
            double[] position = position(view, node);
            if (position != null) {
                x += position[0];
                y += position[1];
                count++;
            }
        }
        return count == 0 ? null : new double[] {x / count, y / count};
    }

    private static double[] position(CyNetworkView view, CyNode node) {
        View<CyNode> nodeView = view == null ? null : view.getNodeView(node);
        if (nodeView == null) {
            return null;
        }
        return new double[] {
            nodeView.getVisualProperty(BasicVisualLexicon.NODE_X_LOCATION),
            nodeView.getVisualProperty(BasicVisualLexicon.NODE_Y_LOCATION)
        };
    }

    private static void setPosition(CyNetworkView view, CyNode node, double[] position) {
        View<CyNode> nodeView = view.getNodeView(node);
        if (nodeView != null) {
            nodeView.setVisualProperty(BasicVisualLexicon.NODE_X_LOCATION, position[0]);
            nodeView.setVisualProperty(BasicVisualLexicon.NODE_Y_LOCATION, position[1]);
        }
    }

    /** Removes the split cofactors of a destroyed network. */
    @Override
    public void handleEvent(NetworkAboutToBeDestroyedEvent event) {
        mapper.removeNetwork(event.getNetwork().getSUID());
    }

    @Override
    public String toString() {
        return mapper.toString();
    }
}
