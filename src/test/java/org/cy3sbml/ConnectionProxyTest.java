package org.cy3sbml;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import org.cytoscape.property.CyProperty;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Isolated;

// sets the proxy system properties, which the other tests read
@Isolated
class ConnectionProxyTest {
    private static final List<String> KEYS =
            List.of("http.proxyHost", "http.proxyPort", "https.proxyHost", "https.proxyPort");
    private final Map<String, String> saved = new HashMap<>();

    @BeforeEach
    void clearProxyProperties() {
        for (String key : KEYS) {
            saved.put(key, System.getProperty(key));
            System.clearProperty(key);
        }
    }

    @AfterEach
    void restoreProxyProperties() {
        for (String key : KEYS) {
            String value = saved.get(key);
            if (value == null) {
                System.clearProperty(key);
            } else {
                System.setProperty(key, value);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private static ConnectionProxy proxyFor(Properties properties) {
        CyProperty<Properties> cyProperty = mock(CyProperty.class);
        when(cyProperty.getProperties()).thenReturn(properties);
        return new ConnectionProxy(cyProperty);
    }

    private static Properties properties(String... keyValues) {
        Properties properties = new Properties();
        for (int i = 0; i < keyValues.length; i += 2) {
            properties.setProperty(keyValues[i], keyValues[i + 1]);
        }
        return properties;
    }

    @Test
    void httpProxyIsSetAsSystemProperties() {
        proxyFor(properties(
                        "proxy.server.type", "http", "proxy.server", "proxy.example.org", "proxy.server.port", "3128"))
                .setSystemProxyFromCyProperties();

        assertEquals("proxy.example.org", System.getProperty("http.proxyHost"));
        assertEquals("3128", System.getProperty("http.proxyPort"));
        assertEquals("proxy.example.org", System.getProperty("https.proxyHost"));
        assertEquals("3128", System.getProperty("https.proxyPort"));
    }

    @Test
    void httpProxyWithoutHostIsIgnored() {
        ConnectionProxy proxy = proxyFor(properties("proxy.server.type", "http", "proxy.server.port", "3128"));

        assertDoesNotThrow(proxy::setSystemProxyFromCyProperties);
        for (String key : KEYS) {
            assertNull(System.getProperty(key), key);
        }
    }

    @Test
    void httpProxyWithoutPortIsIgnored() {
        ConnectionProxy proxy = proxyFor(properties("proxy.server.type", "http", "proxy.server", "proxy.example.org"));

        assertDoesNotThrow(proxy::setSystemProxyFromCyProperties);
        for (String key : KEYS) {
            assertNull(System.getProperty(key), key);
        }
    }
}
