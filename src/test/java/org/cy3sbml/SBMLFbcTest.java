package org.cy3sbml;

import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNode;
import org.cytoscape.model.CyRow;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * Testing fbc models.
 */
public class SBMLFbcTest {
    public static final String TEST_MODEL_FBC = TestUtils.UNITTESTS_RESOURCE_PATH + "/" + "e_coli_core.xml";


    /** Test fbc species attributes. */
    @Test
    public void testFbcSpecies() throws Exception {
        CyNetwork[] networks = new TestUtils().readNetwork(TEST_MODEL_FBC);
        CyNetwork network = networks[0];
        assertNotNull(network);
        assertEquals(358, network.getNodeCount());
        assertEquals(588, network.getEdgeCount());

        // Test species node
        // <species boundaryCondition="false" constant="false" metaid="M_13dpg_c" hasOnlySubstanceUnits="false" sboTerm="SBO:0000247" compartment="c" name="3-Phospho-D-glyceroyl phosphate"
        // fbc:chemicalFormula="C3H4O10P2" id="M_13dpg_c">

        CyNode node = TestUtils.findNodeById("M_13dpg_c", network);
        assertNotNull(node);
        CyRow attributes = network.getRow(node);
        assertEquals("M_13dpg_c", attributes.get(SBML.ATTR_ID, String.class));

        assertEquals(false, attributes.get(SBML.ATTR_BOUNDARY_CONDITION, Boolean.class));
        assertEquals(false, attributes.get(SBML.ATTR_CONSTANT, Boolean.class));
        assertEquals(false, attributes.get(SBML.ATTR_HAS_ONLY_SUBSTANCE_UNITS, Boolean.class));
        assertEquals("SBO:0000247", attributes.get(SBML.ATTR_SBOTERM, String.class));
        assertEquals("c", attributes.get(SBML.ATTR_COMPARTMENT, String.class));
        assertEquals("C3H4O10P2", attributes.get(SBML.ATTR_FBC_CHEMICAL_FORMULA, String.class));
    }
}
