package org.cy3sbml;

import java.io.InputStream;
import java.util.Map;

import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNetworkFactory;
import org.cytoscape.model.NetworkTestSupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TestUtils {
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
	
	public static CyNetwork[] readNetwork(String resource) throws Exception {
		final NetworkTestSupport nts = new NetworkTestSupport();
		final CyNetworkFactory networkFactory = nts.getNetworkFactory();
		
		// read SBML	
		InputStream instream = TestUtils.class.getResourceAsStream(resource);
		String[] tokens = resource.split("/");
		String fileName = tokens[tokens.length-1];
		CyNetwork[] networks;
		try {
			// Reader can be tested without service adapter, 
			SBMLReaderTask readerTask = new SBMLReaderTask(instream, fileName, networkFactory, null, null);
			readerTask.run(null);
			networks = readerTask.getNetworks();
		} catch (Throwable t){
			networks = null;
		}
		return networks;
	}
	
}
