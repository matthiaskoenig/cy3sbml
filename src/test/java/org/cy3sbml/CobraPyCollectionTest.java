package org.cy3sbml;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.InputStream;
import java.util.Arrays;

import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNetworkFactory;
import org.cytoscape.model.NetworkTestSupport;
import org.cytoscape.view.model.CyNetworkViewFactory;
import org.cytoscape.work.TaskMonitor;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RunWith(value = Parameterized.class)
public class CobraPyCollectionTest {
private static final Logger logger = LoggerFactory.getLogger(CobraPyCollectionTest.class);
	private String resource;
	
	//parameters pass via the constructor
	public CobraPyCollectionTest(String resource) {
		this.resource = resource;
	}

	@Before
	public void setUp() throws Exception {
	}

	@After
	public void tearDown() throws Exception {
	}
	
	// Declare parameters
	@Parameters(name= "{index}: {0}")
	public static Iterable<Object[]> data(){
		
		String[] fnames = {
				"iPS189_fixed.xml", 
				"iSB619.xml", 
				"iLL672.xml", 
				"iNJ661.xml", 
				"iRS1597.xml", 
				"iAF1260.xml", 
				"iAI549.xml", 
				"iMH551.xml", 
				"iCS291.xml", 
				"iRR1083.xml", 
				"AraGEM.xml", 
				"iNJ661m.xml", 
				"iCA1273.xml", 
				"iSR432.xml", 
				"iPP668.xml", 
				"iAF692.xml", 
				"iSS724.xml", 
				"iWV1314.xml", 
				"iSO783.xml", 
				"SpoMBEL1693.xml", 
				"iHD666_fixed.xml", 
				"iYL1228.xml", 
				"iBT721_fixed.xml", 
				"iRS605_fixed.xml", 
				"iFF708.xml", 
				"iJN746.xml", 
				"iCac802.xml", 
				"iAN840m.xml", 
				"VvuMBEL943.xml", 
				"iMP429_fixed.xml", 
				"iRS1563.xml", 
				"iJS747.xml", 
				"iOR363.xml", 
				"iJN678.xml", 
				"iTY425_fixed.xml", 
				"iTH366.xml", 
				"iSyn669.xml", 
				"iKF1028.xml", 
				"iLC915.xml", 
				"iIT341.xml", 
				"PpuMBEL1071.xml", 
				"PpaMBEL1254.xml", 
				"S_coilicolor_fixed.xml", 
				"iIB711.xml", 
				"iJP815.xml", 
				"iOG654.xml", 
				"iRC1080.xml", 
				"iMA871.xml", 
				"iNV213.xml", 
				"iMM904.xml", 
				"iAO358.xml", 
				"GSMN_TB.xml", 
				"textbook.xml", 
				"iWZ663.xml", 
				"iTL885.xml", 
				"iZM363.xml", 
				"iJO1366.xml", 
				"iCS400.xml", 
				"iMM1415.xml", 
				"iND750.xml", 
				"iAC560.xml", 
				"iCR744.xml", 
				"iCB925.xml", 
				"iGB555_fixed.xml", 
				"iJL432.xml", 
				"iMA945.xml", 
				"iJR904.xml", 
				"iRsp1095.xml", 
				"iZmobMBEL601.xml", 
				"mus_musculus.xml", 
				"STM_v1_0.xml", 
				"iAbaylyiV4.xml", 
				"iVM679.xml", 
				"iSS884.xml", 
				"iSH335.xml", 
				"iRM588.xml", 
				"iVS941_fixed.xml", 
				"iYO844.xml", 
				"iBsu1103.xml", 
				"iMO1056.xml", 
				"AbyMBEL891.xml", 
				"T_Maritima.xml", 
				"iMB745.xml"
		};

		
		int N = fnames.length;
		Object[][] resources = new String[N][1];
		for (int k=0; k<N; k++){
			String fname = fnames[k];
			resources[k][0] = String.format("/models/m_model_collection/sbml3/%s", fname);
		}
		return Arrays.asList(resources);
	}
	
	@Test
	public void testSingleCobra() throws Exception {
		logger.info(String.format("CobraPyTest: %s", resource));
		
		final NetworkTestSupport nts = new NetworkTestSupport();
		final CyNetworkFactory networkFactory = nts.getNetworkFactory();
		final CyNetworkViewFactory viewFactory = null;
		TaskMonitor taskMonitor = null;
		
		// read SBML	
		String[] tokens = resource.split("/");
		String fileName = tokens[tokens.length-1];		
		InputStream instream = getClass().getResourceAsStream(resource);
	
		CyNetwork[] networks;
		try {
			// Reader can be tested without service adapter, 
			SBMLReaderTask readerTask = new SBMLReaderTask(instream, fileName, networkFactory, viewFactory);
			readerTask.run(taskMonitor);
			networks = readerTask.getNetworks();
			assertFalse(readerTask.getError());
		} catch (Throwable t){
			networks = null;
		}
		// Networks could be read
		assertNotNull(networks);
		assertTrue(networks.length >= 1);
		
	}
}
