package org.cy3sbml;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.net.URL;
import java.util.Properties;
import org.junit.jupiter.api.Test;
import org.osgi.framework.Bundle;

/**
 * Testing {@link CyActivator#findBundleResource}, the helper that looks up a bundled
 * resource by path.
 * <p>
 * This is the fix for the bug investigated in task 3.12: a missing bundled resource
 * (e.g. the extension jar dropped by an {@code Include-Resource} packaging mistake)
 * used to dereference a null {@link URL} and throw a {@link NullPointerException} out
 * of {@code CyActivator.start}, aborting the whole activator - including the SBML
 * reader - before it registered a single service.
 */
public class CyActivatorTest {

    @Test
    public void missingResourceReturnsNullInsteadOfThrowing() {
        Bundle bundle = mock(Bundle.class);
        when(bundle.getEntry("extension/missing.jar")).thenReturn(null);

        URL url = CyActivator.findBundleResource(
                bundle, "extension/missing.jar", "cy3sbml panel disabled: extension jar missing from the bundle");

        assertNull(url);
    }

    @Test
    public void presentResourceIsReturnedUnchanged() throws Exception {
        Bundle bundle = mock(Bundle.class);
        URL resourceUrl = getClass().getResource("/logback-test.xml");
        when(bundle.getEntry("logback-test.xml")).thenReturn(resourceUrl);

        URL url = CyActivator.findBundleResource(bundle, "logback-test.xml", "unused");

        assertSame(resourceUrl, url);
    }

    /**
     * Testing {@link CyActivator#cyPropertyServiceProperties}, the OSGi service
     * properties the cy3sbml {@link PropsReader} is registered with.
     * <p>
     * Task 3.15: the cy3sbml properties did not show up in Edit &gt; Preferences &gt;
     * Properties. Both that dialog and CyREST's {@code /v1/properties} endpoint list a
     * {@code CyProperty} service only when its {@code cyPropertyName} service property
     * is set; this pins that contract down so a future change to the registration
     * cannot silently drop it again.
     */
    @Test
    public void cyPropertyServicePropertiesSetsCyPropertyName() {
        Properties serviceProps = CyActivator.cyPropertyServiceProperties(CyActivator.PROPERTIES_FILE);

        assertEquals(CyActivator.PROPERTIES_FILE, serviceProps.getProperty("cyPropertyName"));
        assertEquals("cy3sbml.props", serviceProps.getProperty("cyPropertyName"));
    }
}
