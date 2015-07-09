package org.cy3sbml;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TestHelper {
	private static final Logger logger = LoggerFactory.getLogger(TestHelper.class);
	
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
	
}
