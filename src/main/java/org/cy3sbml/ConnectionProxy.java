package org.cy3sbml;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Handling the proxy settings required to use the web services.
 * Especially listen to the Cytoscape proxy changes and update the System proxy settings
 * respectively.
 *
 */
public class ConnectionProxy {
	private static final Logger logger = LoggerFactory.getLogger(ConnectionProxy.class);
	
	private static final boolean useHTTPProxy = true;
	
	public static boolean isUseHTTPProxy(){
		return useHTTPProxy;
	}
	
	public static String getHTTPHost(){
		//return "proxy.charite.de";
		return "";
	}
	public static String getHTTPPort(){
		//return "8080";
		return "";
	}
	
	public static void setCytoscapeProxy(){
		// TODO: listen to the Cytoscape proxy changes 
		// and set the selected proxy.
		
		// TODO: get settings from Cytoscape
		/*
		String proxyHost = CytoscapeInit.getProperties().getProperty(
				ProxyHandler.PROXY_HOST_PROPERTY_NAME,null);
		String proxyPort = CytoscapeInit.getProperties().getProperty(
				ProxyHandler.PROXY_PORT_PROPERTY_NAME,null);
		*/
	}
	
	public static void setProxy() {
		String host = getHTTPHost();
		String port = getHTTPPort();
				
		logger.info("set proxy: " + host + ":" + port);
		if (isUseHTTPProxy()) {
		    // HTTP/HTTPS Proxy
			System.setProperty("http.proxyHost", host);
	        System.setProperty("http.proxyPort", port);
	        System.setProperty("https.proxyHost", host);
	        System.setProperty("https.proxyPort", port);
		}
			
        /* with additional authentification
        if (isUseHTTPAuth()) {
            String encoded = new String(Base64.encodeBase64((getHTTPUsername() + ":" + getHTTPPassword()).getBytes()));
            con.setRequestProperty("Proxy-Authorization", "Basic " + encoded);
            Authenticator.setDefault(new ProxyAuth(getHTTPUsername(), getHTTPPassword()));
        }
        */
	}
	


}
