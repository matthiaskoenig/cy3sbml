package org.cy3sbml;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.InputStream;
import java.nio.file.FileVisitResult;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.ArrayUtils;
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

import com.ctc.wstx.util.StringUtil;

import static java.nio.file.FileVisitResult.*;

/**
 * Test all sbml files of the SBML TestCases.
 * Necessary to walk the files recursively to get all the SBML test cases.
 * 
 */
@RunWith(value = Parameterized.class)
public class SBMLTestCaseTest{
private static final Logger logger = LoggerFactory.getLogger(SBMLTestCaseTest.class);
	private String resource;
	
	//parameters pass via the constructor
	public SBMLTestCaseTest(String resource) {
		this.resource = resource;
	}

	@Before
	public void setUp() throws Exception {
	}

	@After
	public void tearDown() throws Exception {
	}
	
	/* Search recursively for all SBML files in given path. */
	public static LinkedList<String> walk(String path){
		LinkedList<String> sbmlList = new LinkedList<String>();
		
        File root = new File(path);
        File[] list = root.listFiles();

        if (list == null){
        	return sbmlList;
        }

        for (File f : list) {
            if (f.isDirectory()) {
                sbmlList.addAll(walk(f.getAbsolutePath()));
                // System.out.println( "Dir:" + f.getAbsoluteFile() );
            }
            else {
                // System.out.println( "File:" + f.getAbsoluteFile() );
                if (f.getName().endsWith(".xml") && f.getName().contains("pass")){
                	
                	//System.out.println( "SBML:" + f.getAbsoluteFile() );
                	sbmlList.add(f.getAbsolutePath());
                }
            }
        }
        return sbmlList;
    }
	
	// Declare parameters
	@Parameters(name= "{index}: {0}")
	public static Iterable<Object[]> data(){
		System.out.println("Working Directory = " + System.getProperty("user.dir"));
		File currentDir = new File(System.getProperty("user.dir"));
		String rootPath = new File(currentDir, "src/test/resources/models/sbml-test-cases").getPath();
		// Get all the SBML files
		LinkedList<String> sbmlPaths = walk(rootPath);
		
		int N = sbmlPaths.size();
		System.out.println("Number of SBML test cases: " + N);
		Object[][] resources = new String[N][1];
		for (int k=0; k<N; k++){
			String path = sbmlPaths.get(k);
			// create the resource
			String[] items = path.split("/");
			int mindex = -1;
			for (int i=0; i<items.length; i++){
				if (items[i].equals("models")){
					mindex = i;
					break;
				}
			}
			String resource = StringUtils.join(ArrayUtils.subarray(items, mindex, items.length), "/");
			// System.out.println(resource);
			resources[k][0] = "/" + resource; // String.format("/models/BiGG/%s", fname);
		}
		return Arrays.asList(resources);
	}
	
	@Test
	/** Single SBMLTestCase: Can be read and is network created ? */
	public void testSingleTestCase() throws Exception {
		logger.info(String.format("SBMLTestCase: %s", resource));
				
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
			t.printStackTrace();
		}
		// Networks could be read
		assertNotNull(networks);
		assertTrue(networks.length >= 1);
	}
}
