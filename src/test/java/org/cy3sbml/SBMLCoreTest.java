package org.cy3sbml;

import static org.junit.Assert.*;

import java.io.InputStream;
import java.util.List;

import org.cy3sbml.util.NetworkUtil;
import org.junit.Test;
import org.cytoscape.model.*;
import org.sbml.jsbml.JSBML;
import org.sbml.jsbml.Model;
import org.sbml.jsbml.SBMLDocument;

import org.cy3sbml.util.IOUtil;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Test reading of SBML core model.
 */
public class SBMLCoreTest {
    private static final Logger logger = LoggerFactory.getLogger(SBMLCoreTest.class);

	public static final String TEST_MODEL_CORE_01 = TestUtils.UNITTESTS_RESOURCE_PATH + "/" + "core_01.xml";
    public static final String TEST_MODEL_CORE_02 = TestUtils.UNITTESTS_RESOURCE_PATH + "/" + "galactose.xml";
    public static final String TEST_MODEL_CORE_03 = TestUtils.UNITTESTS_RESOURCE_PATH + "/" + "yeast_glycolysis.xml";

    /** Load the given model resource. */
    private void loadModel(String resource){
        InputStream instream = getClass().getResourceAsStream(resource);
        try {
            String xml = IOUtil.inputStream2String(instream);
            SBMLDocument document = JSBML.readSBMLFromString(xml);
            @SuppressWarnings("unused")
            Model model = document.getModel();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

	/** Test if model can be read with JSBML. */
	@Test 
	public void testModelLoading_01(){
	    loadModel(TEST_MODEL_CORE_01);
	}

    /** Test if model can be read with JSBML.
     * Tests for InitialAssignments and Rules.
     */
    @Test
    public void testModelLoading_02(){
        loadModel(TEST_MODEL_CORE_02);
    }

    /** Test if model can be read with JSBML.
     * Tests for LocalParameters.
     */
    @Test
    public void testModelLoading_03(){
        loadModel(TEST_MODEL_CORE_03);
    }

	/** Test that networks are created by reader. */
	@Test
	public void testCoreNetwork_01() throws Exception {
		CyNetwork[] networks = new TestUtils().readNetwork(TEST_MODEL_CORE_01);
		assertNotNull(networks);
		assertTrue(networks.length >= 1);

        CyNetwork baseNetwork = NetworkUtil.getNetworkBySubNetworkPrefix(networks, SBML.PREFIX_SUBNETWORK_BASE);
        assertNotNull(baseNetwork);
        assertEquals(29, baseNetwork.getNodeCount());
        assertEquals(34, baseNetwork.getEdgeCount());

        CyNetwork kineticNetwork = NetworkUtil.getNetworkBySubNetworkPrefix(networks, SBML.PREFIX_SUBNETWORK_KINETIC);
        assertNotNull(kineticNetwork);
		assertEquals(82, kineticNetwork.getNodeCount());
		assertEquals(148, kineticNetwork.getEdgeCount());

        CyNetwork allNetwork = NetworkUtil.getNetworkBySubNetworkPrefix(networks, SBML.PREFIX_SUBNETWORK_ALL);
        assertNotNull(allNetwork);
        assertEquals(91, allNetwork.getNodeCount());
        assertEquals(165, allNetwork.getEdgeCount());
	}

    @Test
    public void testCoreNetwork_02() throws Exception {
        CyNetwork[] networks = new TestUtils().readNetwork(TEST_MODEL_CORE_02);
        assertNotNull(networks);
        assertTrue(networks.length >= 1);
    }

    @Test
    public void testCoreNetwork_03() throws Exception {
        CyNetwork[] networks = new TestUtils().readNetwork(TEST_MODEL_CORE_03);
        assertNotNull(networks);
        assertTrue(networks.length >= 1);
    }

    /** Test core edges. */
    @Test
    public void testCoreEdges() throws Exception {
        CyNetwork[] networks = new TestUtils().readNetwork(TEST_MODEL_CORE_01);
        CyNetwork network = NetworkUtil.getNetworkBySubNetworkPrefix(networks, SBML.PREFIX_SUBNETWORK_KINETIC);
        assertNotNull(network);

        // 2 directed edges
        CyNode node = TestUtils.findNodeById("BLL", network);
        List<CyEdge> edgeList = network.getAdjacentEdgeList(node, CyEdge.Type.DIRECTED);
        assertNotNull(edgeList);
        assertEquals(5, edgeList.size());

        // 0 undirected edges
        edgeList = network.getAdjacentEdgeList(node, CyEdge.Type.UNDIRECTED);
        assertNotNull(edgeList);
        assertEquals(0, edgeList.size());

        // 2 incoming edge
        edgeList = network.getAdjacentEdgeList(node, CyEdge.Type.INCOMING);
        assertNotNull(edgeList);
        assertEquals(2, edgeList.size());

        CyEdge e1 = edgeList.get(1);
        CyEdge e2 = edgeList.get(0);
        CyNode n1 = e1.getSource();
        String n1Id = network.getRow(n1).get(SBML.ATTR_ID, String.class);
        if (n1Id.equals("React2")){
            assertEquals(SBML.INTERACTION_REACTION_REACTANT, network.getRow(e1).get(SBML.INTERACTION_ATTR, String.class));
            assertEquals(SBML.INTERACTION_REACTION_PRODUCT, network.getRow(e2).get(SBML.INTERACTION_ATTR, String.class));
        } else {
            assertEquals(SBML.INTERACTION_REACTION_REACTANT, network.getRow(e2).get(SBML.INTERACTION_ATTR, String.class));
            assertEquals(SBML.INTERACTION_REACTION_PRODUCT, network.getRow(e1).get(SBML.INTERACTION_ATTR, String.class));
        }

        // 0 outgoing edge
        edgeList = network.getAdjacentEdgeList(node, CyEdge.Type.OUTGOING);
        assertNotNull(edgeList);
        assertEquals(3, edgeList.size());
    }

    /** Test species attributes. */
    @Test
    public void testCoreSpecies() throws Exception {
        CyNetwork[] networks = new TestUtils().readNetwork(TEST_MODEL_CORE_01);
        CyNetwork network = NetworkUtil.getNetworkBySubNetworkPrefix(networks, SBML.PREFIX_SUBNETWORK_BASE);
        assertNotNull(network);

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
        CyNetwork[] networks = new TestUtils().readNetwork(TEST_MODEL_CORE_01);
        CyNetwork network = NetworkUtil.getNetworkBySubNetworkPrefix(networks, SBML.PREFIX_SUBNETWORK_KINETIC);
        assertNotNull(network);

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

    /**
     * Test parameter attributes & parameter in model.
     */
    @Test
    public void testCoreParameter() throws Exception {
        CyNetwork[] networks = new TestUtils().readNetwork(TEST_MODEL_CORE_01);
        CyNetwork network = NetworkUtil.getNetworkBySubNetworkPrefix(networks, SBML.PREFIX_SUBNETWORK_KINETIC);
        assertNotNull(network);

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
        CyNetwork[] networks = new TestUtils().readNetwork(TEST_MODEL_CORE_01);
        CyNetwork network = NetworkUtil.getNetworkBySubNetworkPrefix(networks, SBML.PREFIX_SUBNETWORK_KINETIC);
        assertNotNull(network);

        // <reaction id="React0" name="React0" metaid="_000016" sboTerm="SBO:0000177">
        CyNode node = TestUtils.findNodeById("React0", network);
        assertNotNull(node);

        CyRow attributes = network.getRow(node);
        assertEquals("React0", attributes.get(SBML.ATTR_ID, String.class));
        assertEquals("React0", attributes.get(SBML.ATTR_NAME, String.class));
        assertEquals("_000016", attributes.get(SBML.ATTR_METAID, String.class));
        assertEquals("SBO:0000177", attributes.get(SBML.ATTR_SBOTERM, String.class));

        /*
        <listOfReactants>
            <speciesReference species="B" metaid="_323321" sboTerm="SBO:0000010"/>
        </listOfReactants>
        <listOfProducts>
            <speciesReference species="BL" metaid="_323334" sboTerm="SBO:0000011"/>
        </listOfProducts>
         */
        List<CyNode> nodeList = network.getNeighborList(node, CyEdge.Type.DIRECTED);
        assertNotNull(nodeList);
        assertEquals(3, nodeList.size());

        CyNode node_B = TestUtils.findNodeById("B", network);
        CyNode node_BL = TestUtils.findNodeById("BL", network);
        assertTrue(nodeList.contains(node_B));
        assertTrue(nodeList.contains(node_BL));
    }

    /**
     * Test if name attribute is accessible in all subnetworks.
     * This tests the issue:
     *      https://github.com/matthiaskoenig/cy3sbml/issues/115
     */
    @Test
    public void testCoreNameSharing() throws Exception {
        CyNetwork[] networks = new TestUtils().readNetwork(TEST_MODEL_CORE_01);
        CyNetwork kineticNetwork = NetworkUtil.getNetworkBySubNetworkPrefix(networks, SBML.PREFIX_SUBNETWORK_KINETIC);
        assertNotNull(kineticNetwork);

        CyNetwork baseNetwork = NetworkUtil.getNetworkBySubNetworkPrefix(networks, SBML.PREFIX_SUBNETWORK_BASE);
        assertNotNull(baseNetwork);

        // <reaction id="React0" name="React0" metaid="_000016" sboTerm="SBO:0000177">
        CyNode n1 = TestUtils.findNodeById("React0", baseNetwork);
        assertNotNull(n1);
        CyRow attributes = baseNetwork.getRow(n1);
        assertEquals("React0", attributes.get(SBML.ATTR_NAME, String.class));

        CyNode n2 = TestUtils.findNodeById("React0", kineticNetwork);
        assertNotNull(n2);
        attributes = kineticNetwork.getRow(n2);
        assertEquals("React0", attributes.get(SBML.ATTR_NAME, String.class));
    }
}
