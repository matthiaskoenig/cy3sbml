package org.cy3sbml;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.Arrays;
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
        CyNetwork network = NetworkUtil.getSubnetwork(networks, SBML.SUFFIX_SUBNETWORK_ALL);
        assertNotNull(network);

        CyNode node = TestUtils.findNodeById("C", network);
        assertEquals(
                "standardDeviation=0.1 litre", network.getRow(node).get(SBML.ATTR_DISTRIB_UNCERTAINTY, String.class));
        assertEquals(1, network.getRow(node).get(SBML.ATTR_DISTRIB_UNCERTAINTY_COUNT, Integer.class));
    }

    @Test
    void flatModelHasTheRenamedUncertainties() throws Exception {
        CyNetwork[] networks = TestUtils.readNetwork("/models/distrib/distrib_comp.xml");
        CyNetwork flat = Arrays.stream(networks)
                .filter(n -> "Flat__distrib_comp__all".equals(n.getRow(n).get(CyNetwork.NAME, String.class)))
                .findFirst()
                .orElseThrow();

        CyNode node = TestUtils.findNodeById("A__k1", flat);
        assertEquals(
                "standardDeviation=A__sd A__per_s; range=[A__sd, A__k1]",
                flat.getRow(node).get(SBML.ATTR_DISTRIB_UNCERTAINTY, String.class));
    }
}
