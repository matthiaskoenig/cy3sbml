package org.cy3sbml;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;

import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNetworkFactory;
import org.cytoscape.model.NetworkTestSupport;
import org.cytoscape.view.model.CyNetworkViewFactory;
import org.cytoscape.work.TaskMonitor;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RunWith(value = Parameterized.class)
public class BiGGTest {
private static final Logger logger = LoggerFactory.getLogger(BiGGTest.class);
	private String resource;
	
	//parameters pass via the constructor
	public BiGGTest(String resource) {
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
		
		
		/* Seems incredible difficult to get the filenames from a folder
		 * in a resource in java, so just list all the file :/
		
		// get all file names in resource folder
		ClassLoader classLoader = BiGGTest.class.getClassLoader();
		URL url = classLoader.getResource("/models/BiGG/");
		File file = new File(url.getFile());
		LinkedList<String> fnames = new LinkedList<String>();
		if (file.isDirectory()){
			File[] files = file.listFiles();
			for (File f : files){
				fnames.add(f.getName());
			}
		}
		int N = fnames.size();
		*/
		String[] fnames = {
				"e_coli_core.xml", "iAB_RBC_283.xml", "iAF1260.xml", "iAF692.xml", "iAF987.xml", 
				"iAPECO1_1312.xml", "iAT_PLT_636.xml", "iB21_1397.xml", "iBWG_1329.xml", "ic_1306.xml",
				"iE2348C_1286.xml", "iEC042_1314.xml", "iEC55989_1330.xml", "iECABU_c1320.xml", "iECB_1328.xml",
				"iECBD_1354.xml", "iECD_1391.xml", "iECDH10B.xml", "iEcDH1_1363.xml", "iECDH1ME8569_1439.xml",
				"iEcE24377_1341.xml", "iECED1_1282.xml", "iECH74115_1262.xml", "iEcHS_1320.xml", "iECIAI1_1343.xml",
				"iECIAI39_1322.xml", "iECNA114_1301.xml", "iECO103_1326.xml", "iECO111_1330.xml", "iECO26_1355.xml",
				"iECOK1_1307.xml", "iEcolC_1368.xml", "iECP_1309.xml", "iECs_1301.xml", "iECS88_1305.xml", "iECSE_1348.xml",
				"iECSF_1327.xml", "iEcSMS35_1347.xml", "iECSP_1301.xml", "iECUMN_1333.xml", "iECW_1372.xml", "iEKO11_1354.xml",
				"iETEC_1333.xml", "iG2583_1286.xml", "iHN637.xml", "iIJ478.xml", "iIT341.xml", "iJN678.xml", "iJN746.xml", "iJO1366.xml",
				"iJR904.xml", "iLF82_1304.xml", "iMM1415.xml", "iMM904.xml", "iND750.xml", "iNJ661.xml", "iNRG857_1313.xml", "iPC815.xml",
				"iRC1080.xml", "iS_1188.xml", "iSB619.xml", "iSbBS512_1146.xml", "iSBO_1134.xml", "iSDY_1059.xml", "iSF_1195.xml",
				"iSFV_1184.xml", "iSFxv_1172.xml", "iSSON_1240.xml", "iUMN146_1321.xml", "iUMNK88_1353.xml", "iUTI89_1310.xml",
				"iWFL_1372.xml", "iYL1228.xml", "iZ_1308.xml", "RECON1.xml", "STM_v1_0.xml"
		};

		
		int N = fnames.length;
		Object[][] resources = new String[N][1];
		for (int k=0; k<N; k++){
			String fname = fnames[k];
			resources[k][0] = String.format("/models/BiGG/%s", fname);
		}
		return Arrays.asList(resources);
	}
	
	@Test
	/** Single test for BiGG model: Can be read and is network created ? */
	public void testSingleBiGG() throws Exception {
		logger.info(String.format("BioModelsTest: %s", resource));
		
		/*
		HashSet<String> exclude = new HashSet<String>();
		exclude.add("iECDH10B"); // missing zip
		exclude.add("iEcHS_1320"); // missing zip
		exclude.add("iJN678"); // missing zip
		exclude.add("iMM1415"); // missing zip
		
		String fname  = resource.split("/")[3];
		String id = fname.split(".")[0];
		if (exclude.contains(id)){
			logger.info("BiGG excluded:" + id);
			return;
		}
		*/
		
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
			assertFalse(readerTask.getError());
		} catch (Throwable t){
			networks = null;
		}
		// Networks could be read
		assertNotNull(networks);
		assertTrue(networks.length >= 1);
		
	}
}
