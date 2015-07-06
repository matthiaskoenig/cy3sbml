package org.cy3sbml;

import static org.junit.Assert.*;

import java.io.InputStream;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.sbml.jsbml.JSBML;
import org.sbml.jsbml.Model;
import org.sbml.jsbml.SBMLDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNetworkFactory;
import org.cytoscape.model.NetworkTestSupport;
import org.cytoscape.view.model.CyNetworkViewFactory;

// @RunWith(Parametrized.class)
public class SBMLReaderTaskTest {
	private static final Logger logger = LoggerFactory.getLogger(SBMLReaderTaskTest.class);
	
	@Before
	public void setUp() throws Exception {
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test 
	/* Test if test model can be read with JSBML. */
	public void testModelLoading(){
		String resource = "/models/BioModels-r29_sbml_curated/BIOMD0000000001.xml";
		
		InputStream instream = getClass().getResourceAsStream(resource);
		try {
			String xml = SBMLReaderTask.readString(instream);
			SBMLDocument document = JSBML.readSBMLFromString(xml);
			Model model = document.getModel();

		} catch (Exception e) {
			logger.warn("Could not read example");
			e.printStackTrace();
		}
	}
	
	@Test
	/* 
	 * Test if the run() method of SBMLReaderTask creates networks
	 * for test model.
	 */
	public void testRunTaskMonitor() throws Exception {
		final NetworkTestSupport nts = new NetworkTestSupport();
		// final CyNetwork network = nts.getNetwork();
		// final CyNetworkManager networkManager = nts.getNetworkManager();
		final CyNetworkFactory networkFactory = nts.getNetworkFactory();
		final CyNetworkViewFactory viewFactory = null;
		
		// read SBML	
		String resource = "/models/BioModels-r29_sbml_curated/BIOMD0000000001.xml";
		String fileName = "BIOMD0000000001.xml";		
		InputStream instream = getClass().getResourceAsStream(resource);
	
		CyNetwork[] networks;
		try {
			// Reader can be tested without service adapter, 
			SBMLReaderTask readerTask = new SBMLReaderTask(instream, fileName, networkFactory, viewFactory);
			readerTask.run(null);
			networks = readerTask.getNetworks();
		} catch (Throwable t){
			networks = null;
		}
		assertNotNull(networks);
		assertTrue(networks.length == 2);
		
		// now test the network content & the attributes
		CyNetwork network = networks[1];
		assertEquals(29, network.getNodeCount());
		assertEquals(34, network.getEdgeCount());
		// attribute table
		// nts.getNetworkTableManager().getTable(network, type, namespace)
	}

	@Test
	/* Test if network attributes have been read correctly. */
	public void testCoreNetworkAttributes(){
		// TODO
		
	}
	
	
}