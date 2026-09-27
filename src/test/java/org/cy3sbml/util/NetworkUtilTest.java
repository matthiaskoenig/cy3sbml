package org.cy3sbml.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.cy3sbml.SBML;
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
}
