package org.cy3sbml;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.cy3sbml.util.NetworkUtil;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNode;
import org.junit.jupiter.api.Test;

/**
 * Reading distrib models.
 */
class SBMLDistribTest {
    static final String TEST_MODEL_DISTRIB = "/models/distrib/distrib_uncertainties.xml";

    @Test
    void importWritesTheUncertainties() throws Exception {
        CyNetwork[] networks = TestUtils.readNetwork(TEST_MODEL_DISTRIB);
        CyNetwork network = NetworkUtil.getNetworkBySubNetworkPrefix(networks, SBML.PREFIX_SUBNETWORK_ALL);
        assertNotNull(network);

        CyNode node = TestUtils.findNodeById("C", network);
        assertEquals(
                "standardDeviation=0.1 litre", network.getRow(node).get(SBML.ATTR_DISTRIB_UNCERTAINTY, String.class));
        assertEquals(1, network.getRow(node).get(SBML.ATTR_DISTRIB_UNCERTAINTY_COUNT, Integer.class));
    }
}
