package org.cy3sbml;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Opens the packaged bundle jar itself (not {@code target/classes}, which still has
 * everything and cannot catch an {@code Include-Resource} packaging mistake) and checks
 * it has the resources cy3sbml needs at runtime.
 * <p>
 * This is the regression test for task 3.12: a build whose {@code Include-Resource} did
 * not include {@code {maven-resources}} produced a jar missing the extension jar and
 * every other {@code src/main/resources} file. {@code CyActivator.start} then threw a
 * {@code NullPointerException} looking the extension jar up, aborting before it
 * registered any service, and Cytoscape's own bundled SBML app (with no biojava in its
 * JSBML) silently took over {@code .xml} imports and failed with
 * {@code NoClassDefFoundError: org.sbml.jsbml.SBO}.
 * <p>
 * The jar path comes from the {@code cy3sbml.bundle.jar} system property, set by the pom
 * (see the failsafe plugin configuration) to
 * {@code ${project.build.directory}/${project.build.finalName}.jar}; this test runs in
 * the {@code integration-test} phase, after the bundle jar has been packaged.
 */
public class BundleJarContentIT {

    private static JarFile jar;
    private static List<String> entryNames;

    @BeforeAll
    public static void openJar() throws IOException {
        String jarPath = System.getProperty("cy3sbml.bundle.jar");
        assertNotNull(jarPath, "system property cy3sbml.bundle.jar is not set (see the pom's failsafe config)");
        File jarFile = new File(jarPath);
        assertTrue(jarFile.isFile(), "packaged bundle jar not found: " + jarFile.getAbsolutePath());

        jar = new JarFile(jarFile);
        entryNames = new ArrayList<>();
        Enumeration<JarEntry> entries = jar.entries();
        while (entries.hasMoreElements()) {
            entryNames.add(entries.nextElement().getName());
        }
    }

    @AfterAll
    public static void closeJar() throws IOException {
        if (jar != null) {
            jar.close();
        }
    }

    private static void assertHasEntry(String name) {
        assertTrue(entryNames.contains(name), "bundle jar is missing entry: " + name);
    }

    /**
     * Embedded jars with bnd {@code @ServiceProvider} annotations (e.g. woodstox 7) make bnd
     * add an {@code osgi.extender=osgi.serviceloader.registrar} requirement to the bundle,
     * which Cytoscape does not provide, so the bundle does not resolve. The pom switches
     * that processing off ({@code -metainf-services: none}).
     */
    @Test
    public void requiresNoCapabilityBeyondTheJavaVersion() throws IOException {
        Manifest manifest = jar.getManifest();
        String requireCapability = manifest.getMainAttributes().getValue("Require-Capability");
        assertEquals(
                "osgi.ee;filter:=\"(&(osgi.ee=JavaSE)(version=17))\"",
                requireCapability,
                "the bundle requires capabilities Cytoscape may not provide");
        assertNull(
                manifest.getMainAttributes().getValue("Provide-Capability"),
                "the bundle provides capabilities of its embedded jars");
    }

    @Test
    public void hasTheJavaScriptExtensionBundle() {
        assertHasEntry("extension/org.cy3javascript.extension-0.0.1.jar");
    }

    @Test
    public void hasTheGuiTemplates() {
        assertHasEntry("gui/help.html");
        assertHasEntry("gui/examples.html");
        assertHasEntry("gui/icons.html");
        assertHasEntry("gui/linktemplate.html");
    }

    @Test
    public void hasTheImagesUsedByTheHelpAndExamplesPages() {
        // toolbar action icons (help.html)
        assertHasEntry("gui/images/import.png");
        assertHasEntry("gui/images/help.png");
        assertHasEntry("gui/images/examples.png");
        assertHasEntry("gui/images/changestate.png");
        assertHasEntry("gui/images/cofactor.png");
        assertHasEntry("gui/images/layout-load.png");
        assertHasEntry("gui/images/layout-save.png");
        assertHasEntry("gui/images/favicon.ico");
        // partner/project logos (help.html, examples.html)
        assertHasEntry("gui/images/bigg.png");
        assertHasEntry("gui/images/logos/biomodels_logo.png");
        assertHasEntry("gui/images/logos/jws_logo.png");
        assertHasEntry("gui/images/logos/cytoscape_logo.png");
        assertHasEntry("gui/images/logos/sbml_logo.png");
        // svg icons (help.html)
        assertHasEntry("gui/images/svgs/envelope.svg");
        assertHasEntry("gui/images/svgs/fire.svg");
        assertHasEntry("gui/images/svgs/github.svg");
    }

    @Test
    public void hasTheVisualStyles() {
        assertHasEntry("styles/cy3sbml.xml");
        assertHasEntry("styles/cy3sbml-dark.xml");
        assertHasEntry("styles/robundle.xml");
    }

    @Test
    public void hasTheBundledMiriamRegistry() {
        assertHasEntry("miriam/MiriamRegistry.json");
    }

    @Test
    public void hasSboAndItsOboResource() {
        assertHasEntry("org/sbml/jsbml/SBO.class");
        assertHasEntry("org/sbml/jsbml/resources/cfg/SBO_OBO.obo");
    }

    @Test
    public void hasTheBiojavaOntologyClassesReachableFromTheNestedJar() throws IOException {
        String biojavaVersion = System.getProperty("biojava.version");
        assertNotNull(biojavaVersion, "system property biojava.version is not set (see the pom's failsafe config)");
        String nestedJarName = "biojava-ontology-" + biojavaVersion + ".jar";
        assertHasEntry(nestedJarName);

        Manifest manifest = jar.getManifest();
        String classPath = manifest.getMainAttributes().getValue("Bundle-ClassPath");
        assertNotNull(classPath, "bundle manifest has no Bundle-ClassPath");
        assertTrue(
                classPath.contains(nestedJarName),
                "Bundle-ClassPath does not list " + nestedJarName + ": " + classPath);

        // the nested jar itself must contain the class SBO.<clinit> needs
        // (org.sbml.jsbml.SBO uses org.biojava.nbio.ontology.io.OboParser to read the OBO
        // resource above); this is exactly the class missing from Cytoscape's own bundled
        // SBML app, which is what routed imports there instead of to cy3sbml.
        JarEntry nestedEntry = jar.getJarEntry(nestedJarName);
        assertNotNull(nestedEntry, "nested jar entry not found: " + nestedJarName);
        try (InputStream nestedIn = jar.getInputStream(nestedEntry);
                JarInputStreamCloser closer = new JarInputStreamCloser(nestedIn)) {
            assertTrue(
                    closer.hasEntry("org/biojava/nbio/ontology/io/OboParser.class"),
                    "nested " + nestedJarName + " is missing org/biojava/nbio/ontology/io/OboParser.class");
        }
    }

    @Test
    public void manifestDeclaresTheActivator() throws IOException {
        Manifest manifest = jar.getManifest();
        assertEquals("org.cy3sbml.CyActivator", manifest.getMainAttributes().getValue("Bundle-Activator"));
    }

    @Test
    public void manifestExportsJsbmlWithTheJsbmlVersion() throws IOException {
        String exports = jar.getManifest().getMainAttributes().getValue("Export-Package");
        assertNotNull(exports, "bundle manifest has no Export-Package");
        // org.sbml.jsbml types are part of the API of the SBMLManager service, so the
        // packages are exported, but with the version of JSBML, not of the app
        Matcher clause = Pattern.compile("(?:^|,)(org\\.sbml\\.jsbml[\\w.]*);").matcher(exports);
        int count = 0;
        while (clause.find()) {
            Matcher version = Pattern.compile("version=\"([^\"]+)\"").matcher(exports);
            assertTrue(version.find(clause.end()), clause.group(1) + " has no version");
            assertTrue(
                    version.group(1).startsWith("1.7.0"),
                    clause.group(1) + " is exported with version " + version.group(1));
            count++;
        }
        assertTrue(count > 0, "org.sbml.jsbml is not exported");
    }

    @Test
    public void hasNoJUnitTestClasses() {
        List<String> junitLike = new ArrayList<>();
        for (String name : entryNames) {
            if (name.endsWith("JUnitTests.class")
                    || name.endsWith("JUnitTests$1.class")
                    || name.startsWith("org/sbml/jsbml/test/")
                    || name.startsWith("org/junit/")
                    || name.contains("/junit/")) {
                junitLike.add(name);
            }
        }
        assertTrue(junitLike.isEmpty(), "bundle jar contains JUnit test classes: " + junitLike);
    }

    /** Small helper that reads a nested jar's entry names from an {@link InputStream}. */
    private static final class JarInputStreamCloser implements AutoCloseable {
        private final java.util.jar.JarInputStream jarInputStream;
        private final List<String> nestedEntryNames;

        JarInputStreamCloser(InputStream in) throws IOException {
            this.jarInputStream = new java.util.jar.JarInputStream(in);
            List<String> names = new ArrayList<>();
            JarEntry entry;
            while ((entry = jarInputStream.getNextJarEntry()) != null) {
                names.add(entry.getName());
            }
            this.nestedEntryNames = Collections.unmodifiableList(names);
        }

        boolean hasEntry(String name) {
            return nestedEntryNames.contains(name);
        }

        @Override
        public void close() throws IOException {
            jarInputStream.close();
        }
    }
}
