package org.cy3sbml.reader;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import java.io.InputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.cy3sbml.SBML;
import org.cytoscape.group.CyGroup;
import org.cytoscape.group.GroupTestSupport;
import org.cytoscape.group.internal.CyGroupManagerImpl;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNode;
import org.cytoscape.model.NetworkTestSupport;
import org.cytoscape.model.subnetwork.CyRootNetwork;
import org.cytoscape.model.subnetwork.CySubNetwork;
import org.cytoscape.work.TaskMonitor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * The groups of a model in the networks of its collection (#171): every network has its own
 * Cytoscape group per SBML group, with the members in the network, so Cytoscape can collapse,
 * expand and restore the groups of every network from a session.
 * <p>
 * {@code groups_02.xml}: {@code g_mixed} (species, parameter, compartment), {@code g_params}
 * (parameters), {@code g_reactions}, {@code g_overlap} (a reaction of {@code g_reactions} and a
 * species) and {@code g_nested} ({@code g_reactions} and a species, defined before
 * {@code g_reactions}).
 */
class GroupsSubnetworksTest {

    private CyGroupManagerImpl groupManager;
    private Map<String, CyNetwork> networks;

    @BeforeEach
    void read() throws Exception {
        GroupTestSupport groupSupport = new GroupTestSupport();
        groupManager = groupSupport.getGroupManager();
        try (InputStream stream = getClass().getResourceAsStream("/models/unittests/groups_02.xml")) {
            assertNotNull(stream);
            SBMLReaderTask task = new SBMLReaderTask(
                    stream,
                    "groups_02.xml",
                    new NetworkTestSupport().getNetworkFactory(),
                    groupSupport.getGroupFactory());
            task.run(mock(TaskMonitor.class));
            networks = new HashMap<>();
            for (CyNetwork network : task.getNetworks()) {
                networks.put(network.getRow(network).get(CyNetwork.NAME, String.class), network);
            }
        }
    }

    @Test
    void everyNetworkHasItsOwnGroupsWithItsMembers() {
        Set<CyNode> groupNodes = new HashSet<>();
        for (CyNetwork network : networks.values()) {
            for (CyGroup group : groupManager.getGroupSet(network)) {
                assertEquals(
                        Set.of(network),
                        group.getNetworkSet().stream()
                                .filter(n -> !(n instanceof CyRootNetwork))
                                .collect(Collectors.toSet()),
                        "networks of the group " + id(group));
                assertTrue(groupNodes.add(group.getGroupNode()), "group node in two networks: " + id(group));
                for (CyNode member : group.getNodeList()) {
                    assertTrue(
                            network.containsNode(member) || groupManager.isGroup(member, network),
                            "member of " + id(group) + " not in " + network);
                }
            }
        }
    }

    @Test
    void membersAreTheNodesOfTheNetwork() {
        assertEquals(
                Map.of(
                        "g_mixed", Set.of("S1", "k1", "c"),
                        "g_params", Set.of("k1", "k2"),
                        "g_reactions", Set.of("R1", "R2"),
                        "g_overlap", Set.of("R2", "S2"),
                        "g_nested", Set.of("g_reactions", "S3")),
                members(networks.get("All__groups_02")));
        // the base network has species and reactions, no parameters and compartments
        assertEquals(
                Map.of(
                        "g_mixed", Set.of("S1"),
                        "g_reactions", Set.of("R1", "R2"),
                        "g_overlap", Set.of("R2", "S2"),
                        "g_nested", Set.of("g_reactions", "S3")),
                members(networks.get("groups_02")));
    }

    @Test
    void nestedGroupHasTheGroupNodeOfItsNetwork() {
        for (CyNetwork network : networks.values()) {
            Map<String, CyGroup> groups = groupsById(network);
            assertTrue(
                    groups.get("g_nested")
                            .getNodeList()
                            .contains(groups.get("g_reactions").getGroupNode()),
                    "nested group in " + network);
        }
    }

    @Test
    void groupNodesHaveTheAttributesOfTheGroup() {
        for (CyNetwork network : networks.values()) {
            CyRootNetwork root = ((CySubNetwork) network).getRootNetwork();
            for (CyGroup group : groupManager.getGroupSet(network)) {
                assertEquals(
                        SBML.NODETYPE_GROUP, root.getRow(group.getGroupNode()).get(SBML.NODETYPE_ATTR, String.class));
                assertEquals(id(group), root.getRow(group.getGroupNode()).get(SBML.ATTR_CYID, String.class));
            }
        }
    }

    @Test
    void groupEdgesAreTheEdgesOfTheNetwork() {
        for (CyNetwork network : networks.values()) {
            for (CyGroup group : groupManager.getGroupSet(network)) {
                group.getInternalEdgeList()
                        .forEach(edge -> assertTrue(network.containsEdge(edge), "internal edge of " + id(group)));
                group.getExternalEdgeList().stream()
                        .filter(edge -> !isMetaEdge(network, edge.getSource(), edge.getTarget()))
                        .forEach(edge -> assertTrue(network.containsEdge(edge), "external edge of " + id(group)));
            }
        }
    }

    /** Meta edges connect a group node, which is not in the network while its group is expanded. */
    private boolean isMetaEdge(CyNetwork network, CyNode source, CyNode target) {
        return groupManager.isGroup(source, network) || groupManager.isGroup(target, network);
    }

    /** SBML ids of the members by group, group nodes have their attributes in the root network. */
    private Map<String, Set<String>> members(CyNetwork network) {
        CyRootNetwork root = ((CySubNetwork) network).getRootNetwork();
        return groupManager.getGroupSet(network).stream()
                .collect(Collectors.toMap(
                        GroupsSubnetworksTest::id,
                        group -> group.getNodeList().stream()
                                .map(node -> (network.containsNode(node) ? network : root)
                                        .getRow(node)
                                        .get(SBML.ATTR_ID, String.class))
                                .collect(Collectors.toSet())));
    }

    private Map<String, CyGroup> groupsById(CyNetwork network) {
        return groupManager.getGroupSet(network).stream()
                .collect(Collectors.toMap(GroupsSubnetworksTest::id, group -> group));
    }

    private static String id(CyGroup group) {
        return group.getRootNetwork().getRow(group.getGroupNode()).get(SBML.ATTR_CYID, String.class);
    }
}
