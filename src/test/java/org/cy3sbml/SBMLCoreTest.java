package org.cy3sbml;

import static org.junit.Assert.*;

import java.io.InputStream;
import java.util.List;


import org.junit.Test;
import org.cytoscape.model.*;
import org.sbml.jsbml.JSBML;
import org.sbml.jsbml.Model;
import org.sbml.jsbml.SBMLDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Test reading of SBML core model.
 */
public class SBMLCoreTest {

	private static final Logger logger = LoggerFactory.getLogger(SBMLCoreTest.class);
	public static final String TEST_MODEL_01 = TestUtils.BIOMODELS_RESOURCE_PATH + "/" + "BIOMD0000000001.xml";

    /** Load the given model resource. */
    private void loadModel(String resource){
        InputStream instream = getClass().getResourceAsStream(resource);
        try {
            String xml = SBMLReaderTask.readString(instream);
            SBMLDocument document = JSBML.readSBMLFromString(xml);
            @SuppressWarnings("unused")
            Model model = document.getModel();

        } catch (Exception e) {
            logger.warn("Could not read example");
            e.printStackTrace();
        }
    }

	/** Test if model can be read with JSBML. */
	@Test 
	public void testModelLoading_01(){
	    loadModel(TEST_MODEL_01);
	}


	/** Test that networks are created by reader. */
	@Test
	public void testCoreNetwork() throws Exception {
		// read SBML networks
		CyNetwork[] networks = new TestUtils().readNetwork(TEST_MODEL_01);
		assertNotNull(networks);
		assertTrue(networks.length == 2);
		
		// test nodes and edges
        CyNetwork network = networks[0];
        assertEquals(29, network.getNodeCount());
        assertEquals(34, network.getEdgeCount());

        CyNetwork kineticNetwork = networks[1];
		assertEquals(82, kineticNetwork.getNodeCount());
		assertEquals(148, kineticNetwork.getEdgeCount());
	}

    /** Test core edges. */
    @Test
    public void testCoreEdges() throws Exception {
        CyNetwork[] networks = new TestUtils().readNetwork(TEST_MODEL_01);

        CyNetwork baseNetworks = networks[0];

        CyNode node = TestUtils.findNodeById("BLL", baseNetworks);
        List<CyEdge> edgeList = baseNetworks.getAdjacentEdgeList(node, CyEdge.Type.DIRECTED);
        assertNotNull(edgeList);
        // connected to 2 reactions
        assertEquals(2, edgeList.size());


        // TODO: check the number of different edges (ingoing, outgoing, directed, undirected)

    }

    /** Test species attributes. */
    @Test
    public void testCoreSpecies() throws Exception {
        CyNetwork[] networks = new TestUtils().readNetwork(TEST_MODEL_01);
        CyNetwork network = networks[1];

        // Test species node
        // <species id="BLL" initialAmount="0" name="BasalACh2" metaid="_000003" sboTerm="SBO:0000297" compartment="comp1">
        CyNode node = TestUtils.findNodeById("BLL", network);
        assertNotNull(node);
        CyRow attributes = network.getRow(node);
        assertEquals("BLL", attributes.get(SBML.ATTR_ID, String.class));
        assertEquals((Double) 0.0, attributes.get(SBML.ATTR_INITIAL_AMOUNT, Double.class));
        assertEquals("BasalACh2", attributes.get(SBML.ATTR_NAME, String.class));
        assertEquals("_000003", attributes.get(SBML.ATTR_METAID, String.class));
        assertEquals("SBO:0000297", attributes.get(SBML.ATTR_SBOTERM, String.class));
        assertEquals("comp1", attributes.get(SBML.ATTR_COMPARTMENT, String.class));
    }

    /** Test compartment attributes. */
    @Test
    public void testCoreCompartment() throws Exception {
        CyNetwork[] networks = new TestUtils().readNetwork(TEST_MODEL_01);
        CyNetwork network = networks[1];

        // <compartment id="comp1" name="compartment1" metaid="_000002" sboTerm="SBO:0000290" size="1E-16">
        CyNode node = TestUtils.findNodeById("comp1", network);
        assertNotNull(node);

        CyRow attributes = network.getRow(node);
        assertEquals("comp1", attributes.get(SBML.ATTR_ID, String.class));
        assertEquals("compartment1", attributes.get(SBML.ATTR_NAME, String.class));
        assertEquals("_000002", attributes.get(SBML.ATTR_METAID, String.class));
        assertEquals("SBO:0000290", attributes.get(SBML.ATTR_SBOTERM, String.class));
        assertEquals((Double) 1E-16, attributes.get(SBML.ATTR_SIZE, Double.class));
    }

    /** Test parameter attributes. */
    @Test
    public void testCoreParameter() throws Exception {
        CyNetwork[] networks = new TestUtils().readNetwork(TEST_MODEL_01);
        CyNetwork network = networks[1];

        //   <parameter id="kf_0" constant="false" metaid="metaid_0000037" value="3000" sboTerm="SBO:0000035"/>
        CyNode node = TestUtils.findNodeById("kf_0", network);
        assertNotNull(node);

        CyRow attributes = network.getRow(node);
        assertEquals("kf_0", attributes.get(SBML.ATTR_ID, String.class));
        assertNull(attributes.get(SBML.ATTR_NAME, String.class));
        assertEquals("metaid_0000037", attributes.get(SBML.ATTR_METAID, String.class));
        assertEquals((Double) 3000.0, attributes.get(SBML.ATTR_VALUE, Double.class));
        assertEquals("SBO:0000035", attributes.get(SBML.ATTR_SBOTERM, String.class));
    }

    /** Test reaction attributes. */
    @Test
    public void testCoreReaction() throws Exception {
        CyNetwork[] networks = new TestUtils().readNetwork(TEST_MODEL_01);
        CyNetwork network = networks[1];

        // <reaction id="React0" name="React0" metaid="_000016" sboTerm="SBO:0000177">
        CyNode node = TestUtils.findNodeById("React0", network);
        assertNotNull(node);

        CyRow attributes = network.getRow(node);
        assertEquals("React0", attributes.get(SBML.ATTR_ID, String.class));
        assertEquals("React0", attributes.get(SBML.ATTR_NAME, String.class));
        assertEquals("_000016", attributes.get(SBML.ATTR_METAID, String.class));
        assertEquals("SBO:0000177", attributes.get(SBML.ATTR_SBOTERM, String.class));

        // TODO: check for neighbor nodes
        /*
        <listOfReactants>
            <speciesReference species="B" metaid="_323321" sboTerm="SBO:0000010"/>
        </listOfReactants>
        <listOfProducts>
            <speciesReference species="BL" metaid="_323334" sboTerm="SBO:0000011"/>
        </listOfProducts>
         */
    }

}
