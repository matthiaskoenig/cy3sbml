package org.cy3sbml;

import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.Proxy.Type;
import java.util.Properties;

import org.cytoscape.property.CyProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Handling the system proxy settings required to use the web services (SOAP, REST).
 * The proxy settings of Cytoscape are used at startup to set the system proxy settings.
 * So restart will be necessary to update the proxySettings for cy3sbml.
 * 
 * Proxy authentication is currently not supported.
 * Will be supported if requested. 
 * 
 * TODO: listen to changes in settings
 */
public class ConnectionProxy {
	private static final Logger logger = LoggerFactory.getLogger(ConnectionProxy.class);
	
	private static CyProperty<Properties> cyProperties;
	
	public static void setCyProperties(CyProperty<Properties> cyProperties){
		ConnectionProxy.cyProperties = cyProperties;
	}
	
	public static Proxy getProxy() {
		Properties properties = cyProperties.getProperties();
		final String proxyType = properties.getProperty("proxy.server.type");
		if ("direct".equals(proxyType))
			return Proxy.NO_PROXY;
		
		String hostName = properties.getProperty("proxy.server");
		String portString = properties.getProperty("proxy.server.port");
		
		try {
			int port = Integer.parseInt(portString);
			Type type = null;
			
			if ("http".equals(proxyType))
				type = Type.HTTP;
			if ("socks".equals(proxyType))
				type = Type.SOCKS;
			if (type == null)
				return Proxy.NO_PROXY;
			
			return new Proxy(type, new InetSocketAddress(hostName, port));
		} catch (NumberFormatException e) {
		}
		return Proxy.NO_PROXY;
	}
	
	private static String getProxyHost() {
		return  cyProperties.getProperties().getProperty("proxy.server");
	}
	private static String getProxyPort() {
		return  cyProperties.getProperties().getProperty("proxy.server.port");
	}
	
	public static void setSystemProxy(){
		// Use the Cytoscape properties
		String host = getProxyHost();
		String port = getProxyPort();
		setSystemProxy(host, port);
	}
	
	public static void setSystemProxy(String host, String port) {
		logger.info("set proxy: " + host + ":" + port);
		// HTTP/HTTPS Proxy
		System.setProperty("http.proxyHost", host);
	    System.setProperty("http.proxyPort", port);
	    System.setProperty("https.proxyHost", host);
	    System.setProperty("https.proxyPort", port);
	}
}
