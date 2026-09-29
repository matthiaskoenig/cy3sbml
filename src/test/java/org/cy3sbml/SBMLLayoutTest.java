package org.cy3sbml;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.cy3sbml.util.NetworkUtil;
import org.cytoscape.model.CyNetwork;
import org.junit.jupiter.api.Test;

/**
 * Testing layout models.
 */
public class SBMLLayoutTest {
    public static final String TEST_MODEL_LAYOUT = TestUtils.UNITTESTS_RESOURCE_PATH + "/" + "layout_01.xml";

    /**
     * Test comp model reading.
     */
    @Test
    public void testLayout() throws Exception {
        CyNetwork[] networks = TestUtils.readNetwork(TEST_MODEL_LAYOUT);
        CyNetwork network = NetworkUtil.getSubnetwork(networks, SBML.SUFFIX_SUBNETWORK_ALL);
        assertNotNull(network);
        assertEquals(138, network.getNodeCount());
        assertEquals(207, network.getEdgeCount());
    }
}
