package org.cy3sbml.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import org.cy3sbml.SBML;
import org.cy3sbml.TestUtils;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNetworkFactory;
import org.cytoscape.model.CyNode;
import org.cytoscape.model.NetworkTestSupport;
import org.cytoscape.model.subnetwork.CyRootNetwork;
import org.cytoscape.model.subnetwork.CySubNetwork;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class NetworkUtilTest {

    private CyNetworkFactory networkFactory;
    private CyNetwork network;

    @BeforeEach
    void setUp() {
        networkFactory = new NetworkTestSupport().getNetworkFactory();
        network = networkFactory.createNetwork();
    }

    @Test
    void getRootNetworkSUIDReturnsNullForNullNetwork() {
        assertNull(NetworkUtil.getRootNetworkSUID(null));
    }

    @Test
    void getRootNetworkSUIDReturnsRootNetworkSUID() {
        CyRootNetwork rootNetwork = ((CySubNetwork) network).getRootNetwork();
        assertEquals(rootNetwork.getSUID(), NetworkUtil.getRootNetworkSUID(network));
    }

    @Test
    void getRootNetworkReturnsNullForNullNetwork() {
        assertNull(NetworkUtil.getRootNetwork(null));
    }

    @Test
    void getRootNetworkReturnsRootNetwork() {
        CyRootNetwork rootNetwork = ((CySubNetwork) network).getRootNetwork();
        assertEquals(rootNetwork, NetworkUtil.getRootNetwork(network));
    }

    @Test
    void isSBMLNetworkFalseWithoutColumn() {
        assertFalse(NetworkUtil.isSBMLNetwork(network));
    }

    @Test
    void isSBMLNetworkTrueWithColumn() {
        network.getDefaultNetworkTable().createColumn(SBML.NETWORKTYPE_ATTR, String.class, false);
        assertTrue(NetworkUtil.isSBMLNetwork(network));
    }

    @Test
    void getNetworkBySubNetworkPrefixFindsMatchingNetwork() {
        CyRootNetwork rootNetwork = ((CySubNetwork) network).getRootNetwork();
        CyNetwork other = rootNetwork.addSubNetwork();
        AttributeUtil.set(network, network, CyNetwork.NAME, "All__model", String.class);
        AttributeUtil.set(other, other, CyNetwork.NAME, "Kinetic__model", String.class);

        CyNetwork[] networks = {network, other};
        CyNetwork found = NetworkUtil.getNetworkBySubNetworkPrefix(networks, "Kinetic__");

        assertEquals(other, found);
    }

    @Test
    void getNetworkBySubNetworkPrefixReturnsNullWhenNoMatch() {
        AttributeUtil.set(network, network, CyNetwork.NAME, "All__model", String.class);
        CyNetwork[] networks = {network};

        assertNull(NetworkUtil.getNetworkBySubNetworkPrefix(networks, "Kinetic__"));
    }

    @Test
    void selectByIdSelectsMatchingNodeAndUnselectsOthers() {
        CyNode n1 = network.addNode();
        CyNode n2 = network.addNode();
        AttributeUtil.set(network, n1, SBML.ATTR_ID, "s1", String.class);
        AttributeUtil.set(network, n2, SBML.ATTR_ID, "s2", String.class);
        AttributeUtil.set(network, n1, CyNetwork.SELECTED, true, Boolean.class);

        NetworkUtil.selectById(network, "s2");

        assertFalse(AttributeUtil.get(network, n1, CyNetwork.SELECTED, Boolean.class));
        assertTrue(AttributeUtil.get(network, n2, CyNetwork.SELECTED, Boolean.class));
    }

    @Test
    void selectByMetaIdSelectsMatchingNode() {
        CyNode n1 = network.addNode();
        AttributeUtil.set(network, n1, SBML.ATTR_CYID, "meta1", String.class);

        NetworkUtil.selectByMetaId(network, "meta1");

        assertTrue(AttributeUtil.get(network, n1, CyNetwork.SELECTED, Boolean.class));
    }

    @Test
    void selectByIdDoesNothingWhenNodeNotFound() {
        CyNode n1 = network.addNode();
        AttributeUtil.set(network, n1, SBML.ATTR_ID, "s1", String.class);
        AttributeUtil.set(network, n1, CyNetwork.SELECTED, true, Boolean.class);

        NetworkUtil.selectById(network, "missing");

        assertTrue(AttributeUtil.get(network, n1, CyNetwork.SELECTED, Boolean.class));
    }

    private static CyNetwork network(CyNetwork[] networks, String name) {
        return Arrays.stream(networks)
                .filter(n -> name.equals(n.getRow(n).get(CyNetwork.NAME, String.class)))
                .findFirst()
                .orElseThrow();
    }

    /** The target is found in the base network of its network collection, also if a file is open twice. */
    @Test
    void findsTheNetworkOfTheTargetInItsCollection() throws Exception {
        CyNetwork[] first = TestUtils.readNetwork("/models/comp/koenig-toymodel/toy_top_level.xml");
        CyNetwork[] second = TestUtils.readNetwork("/models/comp/koenig-toymodel/toy_top_level.xml");
        List<CyNetwork> networks = new ArrayList<>(Arrays.asList(first));
        networks.addAll(Arrays.asList(second));
        CyNetwork fba = network(second, "toy_fba");
        Long root = NetworkUtil.getRootNetworkSUID(fba);
        String cyId = fba.getRow(fba.getNodeList().get(0)).get(SBML.ATTR_CYID, String.class);

        assertEquals(Optional.of(fba), NetworkUtil.findTargetNetwork(networks, root, cyId));
        assertEquals(Optional.empty(), NetworkUtil.findTargetNetwork(networks, root, "no_node"));
        assertEquals(Optional.empty(), NetworkUtil.findTargetNetwork(networks, -1L, cyId));
    }

    /** A link to a model without element opens the base network of its collection. */
    @Test
    void findsTheBaseNetworkOfACollection() throws Exception {
        CyNetwork[] networks = TestUtils.readNetwork("/models/comp/koenig-toymodel/toy_top_level.xml");
        CyNetwork main = network(networks, "toy_top_level");

        assertEquals(
                Optional.of(main),
                NetworkUtil.findBaseNetwork(Arrays.asList(networks), NetworkUtil.getRootNetworkSUID(main)));
        assertEquals(Optional.empty(), NetworkUtil.findBaseNetwork(Arrays.asList(networks), -1L));
    }
}
