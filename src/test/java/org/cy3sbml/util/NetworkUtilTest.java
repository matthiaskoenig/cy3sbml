package org.cy3sbml.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
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

    /** The target of a comp reference is found in the base network of its model, not in the flat one. */
    @Test
    void findsTheNetworkOfTheTargetModel() throws Exception {
        CyNetwork[] networks = TestUtils.readNetwork("/models/comp/koenig-toymodel/toy_top_level.xml");
        CyNetwork fba = Arrays.stream(networks)
                .filter(n -> "toy_fba".equals(n.getRow(n).get(CyNetwork.NAME, String.class)))
                .findFirst()
                .orElseThrow();
        String cyId = fba.getRow(fba.getNodeList().get(0)).get(SBML.ATTR_CYID, String.class);

        assertEquals(Optional.of(fba), NetworkUtil.findTargetNetwork(Arrays.asList(networks), "toy_fba", cyId));
        assertEquals(Optional.empty(), NetworkUtil.findTargetNetwork(Arrays.asList(networks), "toy_fba", "no_node"));
        assertEquals(Optional.empty(), NetworkUtil.findTargetNetwork(Arrays.asList(networks), "no_model", cyId));
    }

    /** A link to a model without element opens the base network of the model, not the flat one. */
    @Test
    void findsTheBaseNetworkOfAModel() throws Exception {
        CyNetwork[] networks = TestUtils.readNetwork("/models/comp/koenig-toymodel/toy_top_level.xml");

        Optional<CyNetwork> main = NetworkUtil.findModelNetwork(Arrays.asList(networks), "toy_top_level");

        assertEquals("toy_top_level", main.orElseThrow().getRow(main.get()).get(CyNetwork.NAME, String.class));
        assertEquals(Optional.empty(), NetworkUtil.findModelNetwork(Arrays.asList(networks), "no_model"));
    }
}
