package org.cy3sbml.miriam;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URL;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * The MIRIAM registry falls back to the bundled copy when the online registry cannot be
 * downloaded, so annotations still resolve offline.
 */
class RegistryUtilFallbackTest {

    @Test
    void loadsBundledRegistry() throws Exception {
        Map<String, Namespace> registry = RegistryUtil.loadBundledRegistry();
        assertNotNull(registry.get("go"));
        assertNotNull(registry.get("chebi"));
    }

    @Test
    void fallsBackToBundledRegistryWhenDownloadFails() throws Exception {
        // nothing listens on port 1, the connection is refused immediately
        Map<String, Namespace> registry = RegistryUtil.getMiriamContent(new URL("http://127.0.0.1:1/registry"));
        assertNotNull(registry);
        assertTrue(registry.size() > 100);
        assertNotNull(registry.get("go"));
    }
}
