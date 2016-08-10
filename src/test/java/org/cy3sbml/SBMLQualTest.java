package org.cy3sbml;

import org.cytoscape.model.CyNetwork;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * Testing qual models.
 */
public class SBMLQualTest {
    public static final String TEST_MODEL_QUAL = TestUtils.UNITTESTS_RESOURCE_PATH + "/" + "qual_01.xml";

    /** Test qual model reading. */
    @Test
    public void testQual() throws Exception {
        CyNetwork[] networks = new TestUtils().readNetwork(TEST_MODEL_QUAL);
        CyNetwork network = networks[0];
        assertNotNull(network);
        assertEquals(54, network.getNodeCount());
        assertEquals(57, network.getEdgeCount());
    }
}
