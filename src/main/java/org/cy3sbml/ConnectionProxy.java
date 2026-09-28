package org.cy3sbml;

import java.util.Properties;
import org.cytoscape.property.CyProperty;
import org.cytoscape.property.PropertyUpdatedEvent;
import org.cytoscape.property.PropertyUpdatedListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handling the system proxy settings required to use the web services (SOAP, REST).
 * Listens to changes in Cytoscape properties which are used for
 * updating the system proxy settings.
 * <p>
 * Proxy authentication is currently not supported.
 */
public class ConnectionProxy implements PropertyUpdatedListener {
    private static final Logger logger = LoggerFactory.getLogger(ConnectionProxy.class);

    private CyProperty<Properties> cyProperties;

    /**
     * Constructor.
     */
    public ConnectionProxy(CyProperty<Properties> cyProperties) {
        this.cyProperties = cyProperties;
    }

    /**
     * Sets Cytoscape proxy settings.
     */
    public void setSystemProxyFromCyProperties() {
        String type = getProxyType();
        String host = getProxyHost();
        String port = getProxyPort();
        setSystemProxy(type, host, port);
    }

    public String getProxyType() {
        return cyProperties.getProperties().getProperty("proxy.server.type");
    }

    public String getProxyHost() {
        return cyProperties.getProperties().getProperty("proxy.server");
    }

    public String getProxyPort() {
        return cyProperties.getProperties().getProperty("proxy.server.port");
    }

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

    @Override
    public void handleEvent(PropertyUpdatedEvent event) {
        // Cytoscape does not fire this event reliably for proxy property changes,
        // so the event is only logged.

        @SuppressWarnings("rawtypes")
        CyProperty property = event.getSource();
        String name = property.getName();
        logger.debug("PropertyUpdatedEvent: {}", name);
    }
}
