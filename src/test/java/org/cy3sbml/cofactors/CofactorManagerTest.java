package org.cy3sbml.cofactors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Set;
import org.cy3sbml.SBML;
import org.cy3sbml.util.AttributeUtil;
import org.cytoscape.model.CyEdge;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNode;
import org.cytoscape.model.NetworkTestSupport;
import org.cytoscape.model.subnetwork.CyRootNetwork;
import org.cytoscape.model.subnetwork.CySubNetwork;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests for CofactorManager: splitting a cofactor node into clones, and merging the
 * clones back into the original node.
 * <p>
 * Merging looks the removed cofactor node back up in the root network (it is only
 * removed from the subnetwork view, see {@code CofactorManager.splitCofactorNode}). That
 * only works when the node also stays alive in the root network's base network, exactly
 * like the real "All__" and "Kinetic__"/base subnetworks the reader creates: nodes are
 * added on the root's base network first, then a second, non-base subnetwork is created
 * from that subset. The test mirrors that shape rather than adding nodes directly to a
 * bare, single subnetwork.
 */
class CofactorManagerTest {

    private CyNetwork network;
    private CyNode cofactor;
    private CyNode n1;
    private CyNode n2;
    private CyNode n3;
    private CofactorManager manager;

    @BeforeEach
    void setUp() {
        CyNetwork base = new NetworkTestSupport().getNetworkFactory().createNetwork();
        CyRootNetwork rootNetwork = ((CySubNetwork) base).getRootNetwork();

        cofactor = base.addNode();
        n1 = base.addNode();
        n2 = base.addNode();
        n3 = base.addNode();
        AttributeUtil.set(base, cofactor, SBML.NODETYPE_ATTR, SBML.NODETYPE_SPECIES, String.class);
        CyEdge e1 = base.addEdge(cofactor, n1, true);
        CyEdge e2 = base.addEdge(cofactor, n2, true);
        CyEdge e3 = base.addEdge(cofactor, n3, true);
        for (CyEdge e : List.of(e1, e2, e3)) {
            AttributeUtil.set(base, e, SBML.INTERACTION_ATTR, SBML.INTERACTION_REACTION_REACTANT, String.class);
        }

        network = rootNetwork.addSubNetwork(Set.of(cofactor, n1, n2, n3), Set.of(e1, e2, e3));

        manager = new CofactorManager();
    }

    @Test
    void splittingACofactorNodeCreatesOneCloneEdgePerNeighbor() {
        manager.processNodes(network, List.of(cofactor));

        // the original cofactor node is removed from the (sub)network, replaced by one
        // clone per former neighbor
        assertEquals(6, network.getNodeCount());
        assertEquals(3, network.getEdgeCount());
        for (CyNode neighbor : List.of(n1, n2, n3)) {
            List<CyEdge> neighborEdges = network.getAdjacentEdgeList(neighbor, CyEdge.Type.ANY);
            assertEquals(1, neighborEdges.size());
            // the clone edge must keep the interaction type of the edge it was cloned from
            assertEquals(
                    SBML.INTERACTION_REACTION_REACTANT,
                    AttributeUtil.get(network, neighborEdges.get(0), SBML.INTERACTION_ATTR, String.class));
        }
    }

    @Test
    void cloneNodesGetTheClonedNodeTypeSuffix() {
        manager.processNodes(network, List.of(cofactor));

        List<CyNode> clones = network.getNodeList().stream()
                .filter(n -> !List.of(n1, n2, n3).contains(n))
                .toList();
        assertEquals(3, clones.size());
        for (CyNode clone : clones) {
            assertEquals(
                    SBML.NODETYPE_SPECIES + "-clone",
                    AttributeUtil.get(network, clone, SBML.NODETYPE_ATTR, String.class));
        }
    }

    @Test
    void mergingAnyCloneRestoresTheOriginalCofactorNode() {
        manager.processNodes(network, List.of(cofactor));
        List<CyNode> clones = network.getNodeList().stream()
                .filter(n -> !List.of(n1, n2, n3).contains(n))
                .toList();
        assertEquals(3, clones.size());

        // selecting any single clone triggers merging all clones of the cofactor back in
        manager.processNodes(network, List.of(clones.get(0)));

        assertEquals(4, network.getNodeCount());
        assertEquals(3, network.getEdgeCount());
        assertTrue(network.containsNode(cofactor));
        assertEquals(SBML.NODETYPE_SPECIES, AttributeUtil.get(network, cofactor, SBML.NODETYPE_ATTR, String.class));
        for (CyNode neighbor : List.of(n1, n2, n3)) {
            List<CyEdge> neighborEdges = network.getAdjacentEdgeList(neighbor, CyEdge.Type.ANY);
            assertEquals(1, neighborEdges.size());
            // the restored edge must keep its original interaction type through the
            // split/merge round trip
            assertEquals(
                    SBML.INTERACTION_REACTION_REACTANT,
                    AttributeUtil.get(network, neighborEdges.get(0), SBML.INTERACTION_ATTR, String.class));
        }
    }
}
