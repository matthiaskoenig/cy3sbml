package org.cy3sbml;

import org.cytoscape.model.CyNetwork;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * Testing comp models.
 */
public class SBMLCompTest {
    public static final String TEST_MODEL_COMP = TestUtils.UNITTESTS_RESOURCE_PATH + "/" + "small_population.xml";

    /** Test comp model reading. */
    @Test
    public void testComp() throws Exception {
        CyNetwork[] networks = new TestUtils().readNetwork(TEST_MODEL_COMP);
        CyNetwork network = networks[0];
        assertNotNull(network);
        assertEquals(2, network.getNodeCount());
        assertEquals(1, network.getEdgeCount());
    }
}
