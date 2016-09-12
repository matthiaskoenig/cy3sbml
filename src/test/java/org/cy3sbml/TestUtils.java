package org.cy3sbml;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.*;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.cy3sbml.mapping.Network2SBMLMapper;
import org.cy3sbml.util.IOUtil;

import org.cytoscape.group.CyGroupFactory;
import org.cytoscape.group.GroupTestSupport;
import org.cytoscape.ding.NetworkViewTestSupport;
import org.cytoscape.model.*;
import org.cytoscape.view.model.CyNetworkViewFactory;
import org.cytoscape.work.TaskMonitor;

import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import org.sbml.jsbml.JSBML;
import org.sbml.jsbml.Model;
import org.sbml.jsbml.SBMLDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.stream.XMLStreamException;


/**
 * Helper functions to test SBML models.
 */
public class TestUtils {
	public static String BIOMODELS_RESOURCE_PATH = "/models/BioModels-r30_curated";
	public static String BIGGMODELS_RESOURCE_PATH = "/models/bigg_models-v1.2";
	public static String SBMLTESTCASES_RESOURCE_PATH = "/models/sbml-test-suite-3.2.0";
    public static String UNITTESTS_RESOURCE_PATH = "/models/unittests";

	private static final Logger logger = LoggerFactory.getLogger(TestUtils.class);
	
	/** Reads the system proxy variables and sets the 
	 * java system variables so that the tests use these.
	 */
	public static void setSystemProxyForTests(){
		Map<String, String> env = System.getenv();
		String key = "HTTP_PROXY";
		if (env.containsKey(key)){
			String value = env.get(key);
			if (value.startsWith("http://")){
				value = value.substring(7, value.length());
			}
			String[] tokens = value.split(":");
			// we found the proxy settings
			if (tokens.length == 2){
				String host = tokens[0];
				String port = tokens[1];
				logger.info(String.format("Set test proxy: %s:%s", host, port));
				System.setProperty("http.proxyHost", host);
		        System.setProperty("http.proxyPort", port);		
			}
		}	
	}
	
	/**
	 * Get an iteratable over the resources in the resourcePath.
	 *
	 * Resources in the skip set are skipped.
	 * If a filter string is given only the resources matching the filter are returned.
	 */
	public static Iterable<Object[]> findResources(String resourcePath, String extension, String filter, HashSet<String> skip){
		
		File currentDir = new File(System.getProperty("user.dir"));
		// String rootPath = new File(currentDir, resourcePath).getPath();
		String rootPath = currentDir.getAbsolutePath() + "/src/test/resources" + resourcePath;
		
		System.out.println("curDir:" + currentDir);
		System.out.println("rootPath:" + rootPath);
		
		// Get SBML files for passed tests
		LinkedList<String> sbmlPaths = TestUtils.findFiles(rootPath, extension, filter, skip);
		Collections.sort(sbmlPaths);
		
		int N = sbmlPaths.size();
		System.out.println("Number of resources: " + N);
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
			resources[k][0] = "/" + resource;
		}
		return Arrays.asList(resources);
	}
	
	
	/**
	 * Search recursively for all SBML files in given path.
	 * SBML files have to end in ".xml" and pass the filter expression
	 * and is not in the skip set.
	 */
	public static LinkedList<String> findFiles(String path, String extension, String filter, HashSet<String> skip){
		LinkedList<String> fileList = new LinkedList<>();
		
        File root = new File(path);
        File[] list = root.listFiles();
        
        if (list == null){
        	return fileList;
        }
        if (skip == null){
        	skip = new HashSet<>();
        }

        for (File f : list) {
        	String fpath = f.getAbsolutePath();
        	// recursively search directories
            if (f.isDirectory()) {
                fileList.addAll(findFiles(fpath, extension, filter, skip));
            }
            else {
            	String fname = f.getName();
                if (fname.endsWith(extension) && !skip.contains(fname)){
                	// no filter add
                	if (filter == null){
                		fileList.add(fpath);
                	} else {
                		// filter matches add
                        Pattern pattern = Pattern.compile(filter);
                        Matcher m = pattern.matcher(fname);
                		if (m.find()){
                			fileList.add(fpath);	 
                   	 	}
                	}
                }
            }
        }
        return fileList;
    }
	
	public static LinkedList<String> findFiles(String path, String extension){
		return findFiles(path, extension, null, null);
	}

	/**
	 * Read the CyNetworks from given SBML file resource.
     */
	public CyNetwork[] readNetwork(String resource) throws Exception {

        MockitoAnnotations.initMocks(this);
        final CyNetworkFactory networkFactory = new NetworkTestSupport().getNetworkFactory();
        final CyNetworkViewFactory networkViewFactory = new NetworkViewTestSupport().getNetworkViewFactory();
        final CyGroupFactory groupFactory = new GroupTestSupport().getGroupFactory();
		
		// read SBML	
		InputStream instream = TestUtils.class.getResourceAsStream(resource);
		String[] tokens = resource.split("/");
		String fileName = tokens[tokens.length-1];
		CyNetwork[] networks;
		try {
			// Reader can be tested without service adapter, 
			SBMLReaderTask readerTask = new SBMLReaderTask(instream, fileName, networkFactory, groupFactory);

			readerTask.run(null);
			networks = readerTask.getNetworks();
		} catch (Throwable t){
			networks = null;
		}
		return networks;
	}


	public static CyNode findNodeById(String sbmlId, CyNetwork network) {
		for (CyNode node : network.getNodeList()) {
			CyRow attributes = network.getRow(node);
			String id = attributes.get(SBML.ATTR_ID, String.class);
			if (id != null && id.equals(sbmlId)) {
				return node;
			}
		}
		return null;
	}

    /**
     * Perform the network test for a given SBML resource.
     *
     * There is a memory leak in the network creation, probably the following issue
     * 	http://code.cytoscape.org/redmine/issues/3507
     *
     * See also:
     * This aborts the travis build.
     */
    public static void testNetwork(TaskMonitor taskMonitor, String testType, String resource){
        logger.info("--------------------------------------------------------");
        logger.info(String.format("%s : %s", testType, resource));

        final CyNetworkFactory networkFactory = new NetworkTestSupport().getNetworkFactory();
        final CyGroupFactory groupFactory = new GroupTestSupport().getGroupFactory();

        // read SBML
        String[] tokens = resource.split("/");
        String fileName = tokens[2];
        InputStream instream = TestUtils.class.getResourceAsStream(resource);

        CyNetwork[] networks;
        try {
            // Reader can be tested without service adapter
            // calls networkFactory.createNetwork()
            SBMLReaderTask readerTask = new SBMLReaderTask(instream, fileName, networkFactory, groupFactory);

            readerTask.run(taskMonitor);
            networks = readerTask.getNetworks();
            assertFalse(readerTask.getError());

            for (CyNetwork network: networks){
                network.dispose();
            }

        } catch (Throwable t){
            networks = null;
            t.printStackTrace();
        }
        try {
            instream.close();
        } catch (IOException e){
            e.printStackTrace();
        }

        // Networks could be read
        assertNotNull(networks);
        assertTrue(networks.length >= 1);
    }

    /**
     * Perform the network test for a given SBML resource.
     *
     * There is a memory leak in the network creation, probably the following issue
     * 	http://code.cytoscape.org/redmine/issues/3507
     *
     * See also:
     * This aborts the travis build.
     */
    public static void testNetworkSerialization(String testType, String resource) throws IOException, XMLStreamException, ClassNotFoundException {
        logger.info("--------------------------------------------------------");
        logger.info(String.format("%s : %s", testType, resource));

        // read SBML
        InputStream instream = TestUtils.class.getResourceAsStream(resource);
        String xml = IOUtil.inputStream2String(instream);
        SBMLDocument doc = JSBML.readSBMLFromString(xml);
        assertNotNull(doc);

        // Serialize SBMLDocument
        File tempFile = File.createTempFile("sbml", ".ser");

        FileOutputStream fileOut = new FileOutputStream(tempFile.getAbsolutePath());
        ObjectOutputStream out = new ObjectOutputStream(fileOut);
        out.writeObject(doc);
        out.close();
        fileOut.close();

        // Deserialize
        InputStream inputStream = new FileInputStream(tempFile.getAbsolutePath());
        InputStream buffer = new BufferedInputStream(inputStream);
        ObjectInput input = new ObjectInputStream (buffer);

        SBMLDocument docSerialized = (SBMLDocument)input.readObject();
        assertNotNull(docSerialized);
    }
}
