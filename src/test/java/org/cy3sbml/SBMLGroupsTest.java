package org.cy3sbml;

import org.cy3sbml.util.NetworkUtil;
import org.cytoscape.model.CyNetwork;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * Testing groups models.
 */
public class SBMLGroupsTest {
    public static final String TEST_MODEL_GROUPS = TestUtils.UNITTESTS_RESOURCE_PATH + "/" + "groups_01.xml";

    /** Test comp model reading. */
    @Test
    public void testGroups() throws Exception {
        CyNetwork[] networks = new TestUtils().readNetwork(TEST_MODEL_GROUPS);
        CyNetwork network = NetworkUtil.getNetworkBySubNetworkPrefix(networks, SBML.PREFIX_SUBNETWORK_BASE);

        assertNotNull(network);
        assertEquals(1294, network.getNodeCount());
        assertEquals(2592, network.getEdgeCount());
    }
}
