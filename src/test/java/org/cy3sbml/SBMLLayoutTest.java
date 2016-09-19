package org.cy3sbml;

import org.cy3sbml.util.NetworkUtil;
import org.cytoscape.model.CyNetwork;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * Testing layout models.
 */
public class SBMLLayoutTest {
    public static final String TEST_MODEL_LAYOUT = TestUtils.UNITTESTS_RESOURCE_PATH + "/" + "layout_01.xml";

    /** Test comp model reading. */
    @Test
    public void testLayout() throws Exception {
        CyNetwork[] networks = new TestUtils().readNetwork(TEST_MODEL_LAYOUT);
        CyNetwork network = NetworkUtil.getNetworkBySubNetworkPrefix(networks, SBML.PREFIX_SUBNETWORK_BASE);
        assertNotNull(network);
        assertEquals(137, network.getNodeCount());
        assertEquals(140, network.getEdgeCount());
    }

}
