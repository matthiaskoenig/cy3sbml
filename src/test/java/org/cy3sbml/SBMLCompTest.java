package org.cy3sbml;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.cy3sbml.util.NetworkUtil;
import org.cytoscape.model.CyNetwork;
import org.junit.jupiter.api.Test;

/**
 * Testing comp models.
 */
public class SBMLCompTest {
    public static final String TEST_MODEL_COMP_01 = TestUtils.UNITTESTS_RESOURCE_PATH + "/" + "small_population.xml";
    public static final String TEST_MODEL_COMP_02 = TestUtils.UNITTESTS_RESOURCE_PATH + "/" + "toy_top_level.xml";
    public static final String TEST_MODEL_COMP_03 = TestUtils.UNITTESTS_RESOURCE_PATH + "/" + "01134-sbml-l3v1.xml";

    /**
     * Test comp model reading.
     */
    @Test
    public void testComp_01() throws Exception {
        CyNetwork[] networks = TestUtils.readNetwork(TEST_MODEL_COMP_01);
        CyNetwork network = NetworkUtil.getSubnetwork(networks, SBML.SUFFIX_SUBNETWORK_ALL);
        assertEquals(38, network.getNodeCount());
        assertEquals(1, network.getEdgeCount());
    }

    /**
     * Test comp model reading.
     */
    @Test
    public void testComp_02() throws Exception {
        CyNetwork[] networks = TestUtils.readNetwork(TEST_MODEL_COMP_02);
        // FIXME: there can be multiple subnetworks with the same prefix
        CyNetwork network = NetworkUtil.getSubnetwork(networks, SBML.SUFFIX_SUBNETWORK_ALL);
        assertNotNull(network);
        assertEquals(81, network.getNodeCount());
        // 70 edges and one from each of the 46 replaced elements and the replaced by to its submodel
        assertEquals(117, network.getEdgeCount());
    }

    /**
     * Test comp model reading.
     */
    @Test
    public void testComp_03() throws Exception {
        CyNetwork[] networks = TestUtils.readNetwork(TEST_MODEL_COMP_03);
        CyNetwork network = NetworkUtil.getSubnetwork(networks, SBML.SUFFIX_SUBNETWORK_ALL);
        assertNotNull(network);
        assertEquals(10, network.getNodeCount());
        // 6 edges and one from each of the two replacements to its submodel
        assertEquals(8, network.getEdgeCount());
    }
}
