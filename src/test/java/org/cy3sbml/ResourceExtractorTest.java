package org.cy3sbml;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;

public class ResourceExtractorTest {

    @TempDir
    Path bundleRoot;

    @TempDir
    Path appDirectory;

    @Test
    public void extractCopiesBundleResources() throws IOException {
        write(bundleRoot.resolve("gui/help.html"), "help");
        write(bundleRoot.resolve("gui/css/info.css"), "css");

        new ResourceExtractor(bundleContext(), appDirectory.toFile()).extract();

        assertEquals("help", Files.readString(appDirectory.resolve("gui/help.html")));
        assertEquals("css", Files.readString(appDirectory.resolve("gui/css/info.css")));
    }

    /** Resources removed or renamed in a newer version do not survive an upgrade (#404). */
    @Test
    public void extractRemovesResourcesOfOlderVersions() throws IOException {
        write(bundleRoot.resolve("gui/help.html"), "new help");
        write(appDirectory.resolve("gui/help.html"), "old help");
        write(appDirectory.resolve("gui/removed.html"), "removed");
        write(appDirectory.resolve("gui/removed/old.js"), "removed");
        write(appDirectory.resolve("biomodels/biomodels.html"), "obsolete");
        write(appDirectory.resolve("ro/investigation.ro.zip"), "obsolete");
        write(appDirectory.resolve("omex/showcase.omex"), "obsolete");
        write(appDirectory.resolve("cy3sbml-v0.6.0.log"), "log");

        new ResourceExtractor(bundleContext(), appDirectory.toFile()).extract();

        assertEquals(List.of("cy3sbml-v0.6.0.log", "gui", "gui/help.html"), relativePaths(appDirectory));
        assertEquals("new help", Files.readString(appDirectory.resolve("gui/help.html")));
        assertEquals("log", Files.readString(appDirectory.resolve("cy3sbml-v0.6.0.log")));
    }

    /** A bundle serving its entries from {@code bundleRoot}, like the OSGi framework serves the jar entries. */
    private BundleContext bundleContext() throws IOException {
        Bundle bundle = mock(Bundle.class);
        when(bundle.getEntry(anyString()))
                .thenAnswer(invocation -> bundleRoot
                        .resolve(invocation.<String>getArgument(0).substring(1))
                        .toUri()
                        .toURL());
        when(bundle.getEntryPaths(anyString())).thenAnswer(invocation -> entryPaths(invocation.getArgument(0)));
        BundleContext bc = mock(BundleContext.class);
        when(bc.getBundle()).thenReturn(bundle);
        return bc;
    }

    /** Entry paths below {@code directory}, relative to the bundle root, directories ending with "/". */
    private Enumeration<String> entryPaths(String directory) throws IOException {
        Path dir = bundleRoot.resolve(directory.substring(1));
        if (!Files.isDirectory(dir)) {
            return null;
        }
        try (Stream<Path> children = Files.list(dir)) {
            return Collections.enumeration(
                    children.map(p -> relativePath(bundleRoot, p) + (Files.isDirectory(p) ? "/" : ""))
                            .toList());
        }
    }

    private static List<String> relativePaths(Path root) throws IOException {
        try (Stream<Path> paths = Files.walk(root)) {
            return paths.filter(p -> !p.equals(root))
                    .map(p -> relativePath(root, p))
                    .sorted()
                    .toList();
        }
    }

    private static String relativePath(Path root, Path path) {
        return root.relativize(path).toString().replace('\\', '/');
    }

    private static void write(Path file, String content) throws IOException {
        Files.createDirectories(file.getParent());
        Files.writeString(file, content, StandardCharsets.UTF_8);
    }
}
