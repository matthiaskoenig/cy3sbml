package org.cy3sbml;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;
import java.net.URL;
import java.util.Properties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Version;

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

    private static BundleContext bundleContext(String name, String version) {
        Bundle bundle = mock(Bundle.class);
        when(bundle.getSymbolicName()).thenReturn(name);
        when(bundle.getVersion()).thenReturn(Version.parseVersion(version));
        BundleContext bc = mock(BundleContext.class);
        when(bc.getBundle()).thenReturn(bundle);
        return bc;
    }

    @Test
    public void logFileIsInTheAppDirectoryOfTheCytoscapeConfiguration(@TempDir File configuration) {
        File logFile = CyActivator.logFile(bundleContext("cy3sbml", "0.6.0"), () -> configuration);

        assertEquals(new File(new File(configuration, "cy3sbml"), "cy3sbml-v0.6.0.log"), logFile);
    }

    @Test
    public void logFileWithoutTheConfigurationServiceIsInTheDefaultConfigurationDirectory() {
        // startCore fails without the service, and logs that error to this file
        File logFile = CyActivator.logFile(bundleContext("cy3sbml", "0.6.0"), () -> {
            throw new IllegalStateException("no CyApplicationConfiguration");
        });

        File expected = new File(
                new File(new File(System.getProperty("user.home"), "CytoscapeConfiguration"), "cy3sbml"),
                "cy3sbml-v0.6.0.log");
        assertEquals(expected, logFile);
    }

    @Test
    public void logFileWithoutBundleInformationUsesTheAppName(@TempDir File configuration) {
        BundleContext bc = mock(BundleContext.class);

        File logFile = CyActivator.logFile(bc, () -> configuration);

        assertEquals(new File(new File(configuration, "cy3sbml"), "cy3sbml.log"), logFile);
    }
}
