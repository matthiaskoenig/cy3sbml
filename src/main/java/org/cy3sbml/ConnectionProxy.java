package org.cy3sbml;

import java.util.Properties;
import org.cytoscape.property.CyProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Sets the Java system proxy properties for the web services (OLS, UniProt, ChEBI,
 * identifiers.org, BioModels) from the proxy settings in the Cytoscape properties, when the
 * app starts.
 * <p>
 * Proxy authentication is not supported.
 */
public class ConnectionProxy {
    private static final Logger logger = LoggerFactory.getLogger(ConnectionProxy.class);

    private final CyProperty<Properties> cyProperties;

    /**
     * Creates the proxy settings from the Cytoscape properties.
     *
     * @param cyProperties the Cytoscape properties ({@code cytoscape3.props})
     */
    public ConnectionProxy(CyProperty<Properties> cyProperties) {
        this.cyProperties = cyProperties;
    }

    /** Sets the system proxy from the Cytoscape proxy settings. */
    public void setSystemProxyFromCyProperties() {
        String type = getProxyType();
        String host = getProxyHost();
        String port = getProxyPort();
        setSystemProxy(type, host, port);
    }

    /** The proxy type of the Cytoscape settings: {@code direct}, {@code http} or {@code socks}. */
    public String getProxyType() {
        return cyProperties.getProperties().getProperty("proxy.server.type");
    }

    /** The proxy host of the Cytoscape settings. */
    public String getProxyHost() {
        return cyProperties.getProperties().getProperty("proxy.server");
    }

    /** The proxy port of the Cytoscape settings. */
    public String getProxyPort() {
        return cyProperties.getProperties().getProperty("proxy.server.port");
    }

    /**
     * Sets the HTTP and HTTPS system proxy: none for {@code direct}, the host and port for
     * {@code http}. Other types leave the system properties unchanged.
     *
     * @param type the proxy type
     * @param host the proxy host
     * @param port the proxy port
     */
    public void setSystemProxy(String type, String host, String port) {
        logger.debug("set proxy: {} {}:{}", type, host, port);
        if ("direct".equals(type)) {
            System.setProperty("http.proxyHost", "");
            System.setProperty("http.proxyPort", "");
            System.setProperty("https.proxyHost", "");
            System.setProperty("https.proxyPort", "");
        } else if ("http".equals(type)) {
            // HTTP/HTTPS Proxy
            if (host == null || host.isBlank() || port == null || port.isBlank()) {
                logger.warn(
                        "HTTP proxy ignored: the Cytoscape proxy settings have no host or port (host '{}', port '{}')",
                        host,
                        port);
                return;
            }
            System.setProperty("http.proxyHost", host);
            System.setProperty("http.proxyPort", port);
            System.setProperty("https.proxyHost", host);
            System.setProperty("https.proxyPort", port);
        }
    }
}
