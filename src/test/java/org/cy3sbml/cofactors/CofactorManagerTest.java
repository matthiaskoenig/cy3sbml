package org.cy3sbml.cofactors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.cy3sbml.SBML;
import org.cy3sbml.SBMLManager;
import org.cy3sbml.mapping.One2ManyMapping;
import org.cy3sbml.reader.SBMLReaderTask;
import org.cy3sbml.util.AttributeUtil;
import org.cytoscape.application.CyApplicationManager;
import org.cytoscape.event.DummyCyEventHelper;
import org.cytoscape.model.CyEdge;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNode;
import org.cytoscape.model.NetworkTestSupport;
import org.cytoscape.model.events.NetworkAboutToBeDestroyedEvent;
import org.cytoscape.model.subnetwork.CyRootNetwork;
import org.cytoscape.model.subnetwork.CySubNetwork;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.sbml.jsbml.SBMLDocument;

/**
 * Tests for CofactorManager: splitting nodes into clones and merging the clones back.
 * <p>
 * The network is a subnetwork of a root network whose base network holds all nodes and
 * edges, like the networks the reader creates: a merge adds the split node and its
 * edges back from the root network.
 * <p>
 * Species atp and h2o take part in the reactions r1, r2 and r3 and are in the
 * compartment c (edges between nodes that can all be split).
 */
class CofactorManagerTest {

    private CyNetwork network;
    private CyRootNetwork root;
    private CyNode atp;
    private CyNode h2o;
    private CyNode c;
    private CyNode r1;
    private CyNode r2;
    private CyNode r3;
    private SBMLManager sbmlManager;
    private CofactorManager manager;

    @BeforeEach
    void setUp() {
        CyNetwork base = new NetworkTestSupport().getNetworkFactory().createNetwork();
        root = ((CySubNetwork) base).getRootNetwork();

        atp = node(base, "atp", SBML.NODETYPE_SPECIES);
        h2o = node(base, "h2o", SBML.NODETYPE_SPECIES);
        c = node(base, "c", SBML.NODETYPE_COMPARTMENT);
        r1 = node(base, "r1", SBML.NODETYPE_REACTION);
        r2 = node(base, "r2", SBML.NODETYPE_REACTION);
        r3 = node(base, "r3", SBML.NODETYPE_REACTION);
        Set<CyEdge> edges = new HashSet<>();
        for (CyNode reaction : List.of(r1, r2, r3)) {
            edges.add(edge(base, atp, reaction, SBML.INTERACTION_REACTION_REACTANT));
            edges.add(edge(base, reaction, h2o, SBML.INTERACTION_REACTION_PRODUCT));
        }
        edges.add(edge(base, atp, c, SBML.INTERACTION_SPECIES_COMPARTMENT));
        edges.add(edge(base, h2o, c, SBML.INTERACTION_SPECIES_COMPARTMENT));

        network = root.addSubNetwork(Set.of(atp, h2o, c, r1, r2, r3), edges);

        sbmlManager = new SBMLManager(mock(CyApplicationManager.class));
        sbmlManager.addSBMLForNetwork(
                new SBMLDocument(3, 1), network, SBMLReaderTask.mappingFromNetwork(network, null));
        manager = new CofactorManager(sbmlManager, new DummyCyEventHelper());
    }

    private static CyNode node(CyNetwork network, String id, String type) {
        CyNode node = network.addNode();
        AttributeUtil.set(network, node, SBML.ATTR_CYID, id, String.class);
        AttributeUtil.set(network, node, SBML.NODETYPE_ATTR, type, String.class);
        return node;
    }

    private static CyEdge edge(CyNetwork network, CyNode source, CyNode target, String interaction) {
        CyEdge edge = network.addEdge(source, target, true);
        AttributeUtil.set(network, edge, SBML.INTERACTION_ATTR, interaction, String.class);
        return edge;
    }

    /** The edges of the network as "source interaction target" of the cyIds. */
    private Set<String> edges() {
        return network.getEdgeList().stream()
                .map(e -> cyId(e.getSource()) + " "
                        + AttributeUtil.get(network, e, SBML.INTERACTION_ATTR, String.class) + " "
                        + cyId(e.getTarget()))
                .collect(Collectors.toSet());
    }

    /** The number of nodes of the network per cyId. */
    private Map<String, Integer> nodes() {
        Map<String, Integer> nodes = new HashMap<>();
        for (CyNode node : network.getNodeList()) {
            nodes.merge(cyId(node), 1, Integer::sum);
        }
        return nodes;
    }

    private String cyId(CyNode node) {
        return AttributeUtil.get(network, node, SBML.ATTR_CYID, String.class);
    }

    @Test
    void splitCreatesOneCloneWithOneEdgePerEdge() {
        Set<String> edges = edges();

        List<CyNode> clones = manager.split(network, null, List.of(atp));

        assertEquals(4, clones.size());
        assertFalse(network.containsNode(atp));
        assertEquals(4, nodes().get("atp"));
        // the edges keep their ends (by cyId) and their table values
        assertEquals(edges, edges());
        for (CyNode clone : clones) {
            assertEquals(1, network.getAdjacentEdgeList(clone, CyEdge.Type.ANY).size());
            assertTrue(manager.isClone(network, clone));
            assertEquals(true, AttributeUtil.get(network, clone, SBML.ATTR_COFACTOR_CLONE, Boolean.class));
            // the sbml type is unchanged, so the style and filters apply to the clones
            assertEquals(SBML.NODETYPE_SPECIES, AttributeUtil.get(network, clone, SBML.NODETYPE_ATTR, String.class));
        }
        // the split node and its edges stay in the root network
        assertTrue(root.containsNode(atp));
        assertEquals(4, root.getAdjacentEdgeList(atp, CyEdge.Type.ANY).size());
    }

    @Test
    void clonesAreMappedToTheSBaseOfTheirNode() {
        List<CyNode> clones = manager.split(network, null, List.of(atp));

        One2ManyMapping<String, Long> mapping = sbmlManager.getMapping(network);
        for (CyNode clone : clones) {
            assertTrue(mapping.getValues("atp").contains(clone.getSUID()));
        }

        manager.mergeAll(network, null);

        assertEquals(Set.of(atp.getSUID()), mapping.getValues("atp"));
    }

    @Test
    void mergeRestoresTheNetwork() {
        Map<String, Integer> nodes = nodes();
        Set<String> edges = edges();
        int rootNodes = root.getNodeCount();
        int rootEdges = root.getEdgeCount();
        List<CyNode> clones = manager.split(network, null, List.of(atp, h2o));

        // two clones of a node in the selection merge it once
        List<CyNode> merged = manager.merge(network, null, List.of(clones.get(0), clones.get(1)));

        assertEquals(List.of(atp), merged);
        manager.merge(network, null, clones.subList(4, 7));
        assertEquals(nodes, nodes());
        assertEquals(edges, edges());
        assertTrue(network.containsNode(atp));
        assertTrue(network.containsNode(h2o));
        // the clones and their edges are removed from the root network
        assertEquals(rootNodes, root.getNodeCount());
        assertEquals(rootEdges, root.getEdgeCount());
        assertFalse(manager.hasClones(network));
        // the merged nodes are selected like the clones were
        assertEquals(true, network.getRow(h2o).get(CyNetwork.SELECTED, Boolean.class));
    }

    /** The merged clones and clone edges leave no rows in the tables of the network. */
    @Test
    void mergeRemovesTheTableRowsOfTheClones() {
        Map<String, Integer> rowCounts = rowCounts();

        manager.split(network, null, List.of(atp));
        manager.split(network, null, List.of(c));
        manager.mergeAll(network, null);

        assertEquals(rowCounts, rowCounts());
    }

    private Map<String, Integer> rowCounts() {
        Map<String, Integer> counts = new HashMap<>();
        for (Class<? extends org.cytoscape.model.CyIdentifiable> type : List.of(CyNode.class, CyEdge.class)) {
            for (String table : List.of(CyNetwork.DEFAULT_ATTRS, CyNetwork.LOCAL_ATTRS, CyNetwork.HIDDEN_ATTRS)) {
                counts.put(
                        type.getSimpleName() + " " + table,
                        network.getTable(type, table).getRowCount());
                counts.put(
                        "root " + type.getSimpleName() + " " + table,
                        root.getTable(type, table).getRowCount());
            }
            counts.put(
                    "shared " + type.getSimpleName(),
                    (type == CyNode.class ? root.getSharedNodeTable() : root.getSharedEdgeTable()).getRowCount());
        }
        return counts;
    }

    @Test
    void adjacentSplitNodesMergeInAnyOrder() {
        Set<String> edges = edges();
        int rootEdges = root.getEdgeCount();

        manager.split(network, null, List.of(atp));
        manager.split(network, null, List.of(c));
        // the edge between atp and c is an edge between two clones
        assertEquals(edges, edges());
        manager.merge(network, null, List.of(cloneOf(atp)));
        assertEquals(edges, edges());
        manager.merge(network, null, List.of(cloneOf(c)));

        assertEquals(edges, edges());
        assertTrue(network.containsNode(atp));
        assertTrue(network.containsNode(c));
        assertEquals(rootEdges, root.getEdgeCount());

        manager.split(network, null, List.of(c, atp));
        manager.merge(network, null, List.of(cloneOf(c)));
        manager.merge(network, null, List.of(cloneOf(atp)));

        assertEquals(edges, edges());
        assertEquals(rootEdges, root.getEdgeCount());
        assertEquals(2, root.getAdjacentEdgeList(c, CyEdge.Type.ANY).size());
    }

    private CyNode cloneOf(CyNode node) {
        Long clone = manager.getNetwork2CofactorMapper()
                .getClones(network.getSUID(), node.getSUID())
                .iterator()
                .next();
        return network.getNode(clone);
    }

    /** Sessions of older versions stored no original edges of the clone edges. */
    @Test
    void mergeFindsTheOriginalEdgesOfOlderSessions() {
        Set<String> edges = edges();
        int rootEdges = root.getEdgeCount();
        manager.split(network, null, List.of(atp));
        Network2CofactorMapper mapper = manager.getNetwork2CofactorMapper();
        for (Long cloneEdge : mapper.getCloneEdges(network.getSUID()).keySet()) {
            mapper.removeCloneEdge(network.getSUID(), cloneEdge);
        }

        manager.mergeAll(network, null);

        assertEquals(edges, edges());
        assertEquals(rootEdges, root.getEdgeCount());
    }

    @Test
    void mergeAllMergesAllSplitNodes() {
        Map<String, Integer> nodes = nodes();
        manager.split(network, null, List.of(atp));
        manager.split(network, null, List.of(h2o));

        List<CyNode> merged = manager.mergeAll(network, null);

        assertEquals(Set.of(atp, h2o), Set.copyOf(merged));
        assertEquals(nodes, nodes());
        assertFalse(manager.hasClones(network));
    }

    @Test
    void nodesThatCannotBeSplitAreSkipped() {
        CyNode single = node(network, "single", SBML.NODETYPE_SPECIES);
        CyNode unconnected = node(network, "unconnected", SBML.NODETYPE_SPECIES);
        network.addEdge(single, r1, true);
        CyNode group = node(network, "group", SBML.NODETYPE_GROUP);
        network.addEdge(group, r1, true);
        network.addEdge(group, r2, true);
        List<CyNode> clones = manager.split(network, null, List.of(atp));

        assertEquals(List.of(), manager.split(network, null, List.of(single, unconnected, group, clones.get(0))));
        assertTrue(network.containsNode(single));
        assertTrue(network.containsNode(unconnected));
        assertTrue(network.containsNode(group));
        // a node that is no clone is not merged
        assertEquals(List.of(), manager.merge(network, null, List.of(r1)));
    }

    @Test
    void splitAgainAfterMerge() {
        Set<String> edges = edges();
        for (int k = 0; k < 3; k++) {
            manager.split(network, null, List.of(atp, h2o, c));
            manager.mergeAll(network, null);
        }
        assertEquals(edges, edges());
        assertEquals(6, network.getNodeCount());
    }

    @Test
    void destroyedNetworkIsRemoved() {
        manager.split(network, null, List.of(atp));

        manager.handleEvent(
                new NetworkAboutToBeDestroyedEvent(mock(org.cytoscape.model.CyNetworkManager.class), network));

        assertFalse(manager.hasClones(network));
    }

    @Test
    void clonesAreFannedOutAtTheirNeighbor() {
        double[] center = {0, 0};
        double[] origin = {100, 0};

        double[] first = CofactorManager.clonePosition(center, origin, 0);
        double[] second = CofactorManager.clonePosition(center, origin, 1);

        assertEquals(CofactorManager.CLONE_DISTANCE, first[0], 1e-9);
        assertEquals(0.0, first[1], 1e-9);
        assertEquals(CofactorManager.CLONE_DISTANCE, Math.hypot(second[0], second[1]), 1e-9);
        assertTrue(second[1] > 0);
    }
}
