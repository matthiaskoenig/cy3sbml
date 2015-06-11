package org.cy3sbml.mapping;

import static org.junit.Assert.*;

import java.util.List;

import org.cy3sbml.SBML;
//import org.cytoscape.ding.NetworkViewTestSupport;

import org.cytoscape.model.CyEdge;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNode;
import org.cytoscape.model.CyRow;
import org.junit.Test;

public class NamedSBase2CyNodeMappingTest {
	/*
	private final NetworkViewTestSupport testSupport = new NetworkViewTestSupport();
	private CyNetwork network;
	
	private final CyNetwork buildNetwork() {	
		final CyNetwork network = testSupport.getNetwork();
		final CyNode node1 = network.addNode();
		CyRow attributes = network.getRow(node1);
		checkSchema(attributes, SBML.SBML_ID_ATTR, String.class);
		attributes.set(SBML.SBML_ID_ATTR, "sbml_id1");
		
		final CyNode node2 = network.addNode();
		attributes = network.getRow(node2);
		attributes.set(SBML.SBML_ID_ATTR, "sbml_id2");
		
		final CyEdge edge = network.addEdge(node1, node2, true);
		testSupport.getNetworkManager().addNetwork(network);
		assertEquals(testSupport.getNetworkManager().getNetworkSet().size(), 1);
		return network;
	}
	
	private <T> void checkSchema(CyRow attributes, String attributeName, Class<T> type) {
		if (attributes.getTable().getColumn(attributeName) == null)
			attributes.getTable().createColumn(attributeName, type, false);
	}

	@Test
	public void testNetworkFunctionality() {
		network = buildNetwork();
		assertNotNull(network);
		// ... test more things
	}
	
	
	
	
	
	@Test
	public void testFromNetwork() {
		final CyNetwork network = testSupport.getNetwork();
		final CyNode node1 = network.addNode();
		CyRow attributes = network.getRow(node1);
		checkSchema(attributes, SBML.SBML_ID_ATTR, String.class);
		attributes.set(SBML.SBML_ID_ATTR, "sbml_id1");
		
		final CyNode node2 = network.addNode();
		attributes = network.getRow(node2);
		attributes.set(SBML.SBML_ID_ATTR, "sbml_id2");
		
		final CyEdge edge = network.addEdge(node1, node2, true);
		testSupport.getNetworkManager().addNetwork(network);
		assertEquals(testSupport.getNetworkManager().getNetworkSet().size(), 1);
		
		NamedSBase2CyNodeMapping mapping = NamedSBase2CyNodeMapping.fromSBMLNetwork(null, network);
		assertTrue(mapping.containsKey("sbml_id1"));
		assertTrue(mapping.containsKey("sbml_id2"));
		List<Long> values = mapping.getValues("sbml_id1");
		assertTrue(values.contains(node1.getSUID()));
	}
	*/
}
