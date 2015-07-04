package org.cy3sbml;

import java.io.InputStream;
import java.util.Arrays;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNetworkFactory;
import org.cytoscape.model.NetworkTestSupport;
import org.cytoscape.view.model.CyNetworkViewFactory;
import org.cytoscape.work.TaskMonitor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RunWith(value = Parameterized.class)
public class BioModelsTest {
private static final Logger logger = LoggerFactory.getLogger(BioModelsTest.class);
	private String resource;
	
	//parameters pass via the constructor
	public BioModelsTest(String resource) {
		this.resource = resource;
	}

	@Before
	public void setUp() throws Exception {
	}

	@After
	public void tearDown() throws Exception {
	}
	
	// Declare parameters
	@Parameters
	public static Iterable<Object[]> data(){
		int N = 575;
		String[][] resources = new String[1][N];
		for (int i=1; i<=N; i++){
			// zero tabbed 
			resources[0][i-1] = String.format("/models/BioModels-r29_sbml_curated/BIOMD0000000%03d.xml", i);
		}
		/*
		new Object[][]{
		{ "/models/BioModels-r29_sbml_curated/BIOMD0000000001.xml" },
		{ "/models/BioModels-r29_sbml_curated/BIOMD0000000002.xml"  }
		...
		}
		*/

		return Arrays.asList(resources);
	}
	
	@Test
	/** Single test for one BioModel: Can be read and creates network. */
	public void testSingleBiomodel() throws Exception {
		logger.info(String.format("BioModelsTest: %s", resource));
		final NetworkTestSupport nts = new NetworkTestSupport();
		final CyNetworkFactory networkFactory = nts.getNetworkFactory();
		final CyNetworkViewFactory viewFactory = null;
		TaskMonitor taskMonitor = null;
		
		// read SBML	
		String[] tokens = resource.split("/");
		String fileName = tokens[2];		
		InputStream instream = getClass().getResourceAsStream(resource);
	
		CyNetwork[] networks;
		try {
			// Reader can be tested without service adapter, 
			SBMLReaderTask readerTask = new SBMLReaderTask(instream, fileName, networkFactory, viewFactory);
			readerTask.run(taskMonitor);
			networks = readerTask.getNetworks();
		} catch (Throwable t){
			networks = null;
		}
		// Networks could be read
		assertNotNull(networks);
		assertTrue(networks.length >= 1);
	}
}
