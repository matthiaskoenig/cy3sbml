package org.cy3sbml;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.List;
import java.util.stream.Stream;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Extracts the bundled GUI resources into the app directory, so the JavaFX WebView can
 * load them via file URIs (it cannot load bundle: URIs).
 * <p>
 * The extracted directories are owned by the extractor: each extraction replaces
 * them completely, so resources removed or renamed in a newer version do not
 * accumulate in the app directory (#404).
 */
public final class ResourceExtractor {
    private static final Logger logger = LoggerFactory.getLogger(ResourceExtractor.class);

    public static final String GUI_RESOURCES = "/gui/";

    /** Bundle directories extracted into the app directory. */
    public static final List<String> RESOURCES = List.of(GUI_RESOURCES);

    /** Directories extracted by earlier cy3sbml versions, removed on extraction. */
    static final List<String> OBSOLETE_RESOURCES = List.of("/biomodels/", "/ro/", "/omex/");

    private final BundleContext bc;
    private final File appDirectory;

    /**
     * @param bc the context of the bundle with the resources
     * @param appDirectory the directory the resources are extracted into
     */
    public ResourceExtractor(final BundleContext bc, final File appDirectory) {
        this.bc = bc;
        this.appDirectory = appDirectory;
    }

    /**
     * Extracts the bundle resources from the BundleContext in the
     * application directory, replacing the previously extracted resources.
     * <p>
     * BundleContext and application directory have to be provided.
     */
    public void extract() {
        if (bc == null || appDirectory == null) {
            logger.error("BundleContext or application directory not set. Files not extracted");
            return;
        }
        logger.debug("Extract bundle resources into <{}>", appDirectory);
        for (String resource : OBSOLETE_RESOURCES) {
            deleteDirectory(resolve(resource));
        }
        Bundle bundle = bc.getBundle();
        for (String resource : RESOURCES) {
            deleteDirectory(resolve(resource));
            extractDirectory(bundle, resource);
        }
    }

    /** Path in the app directory of a bundle path like "/gui/" or "gui/help.html". */
    private Path resolve(String bundlePath) {
        return appDirectory.toPath().resolve(bundlePath.replaceFirst("^/", ""));
    }

    /**
     * Deletes the directory with all its content, if it exists.
     * Symbolic links are deleted, not followed.
     */
    private static void deleteDirectory(Path directory) {
        if (!Files.exists(directory)) {
            return;
        }
        try (Stream<Path> paths = Files.walk(directory)) {
            for (Path path : paths.sorted(Comparator.reverseOrder()).toList()) {
                Files.delete(path);
            }
        } catch (IOException e) {
            logger.error("Could not delete extracted resources <{}>", directory, e);
        }
    }

    /**
     * Extracts the resources in given bundle directory recursively.
     */
    private void extractDirectory(Bundle bundle, String directory) {
        Enumeration<String> paths = bundle.getEntryPaths(directory);
        if (paths == null) {
            logger.error("Bundle directory <{}> does not exist, not extracted", directory);
            return;
        }
        while (paths.hasMoreElements()) {
            String path = paths.nextElement();
            Path target = resolve(path);
            try {
                if (path.endsWith("/")) {
                    Files.createDirectories(target);
                    extractDirectory(bundle, "/" + path);
                } else {
                    Files.createDirectories(target.getParent());
                    logger.debug(" --> {}", target);
                    try (InputStream inputStream = bundle.getEntry("/" + path).openStream()) {
                        Files.copy(inputStream, target, StandardCopyOption.REPLACE_EXISTING);
                    }
                }
            } catch (IOException e) {
                logger.error("Resource <{}> could not be extracted", path, e);
            }
        }
    }
}
