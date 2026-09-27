package org.cy3sbml;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class extracts bundled resources to a local directory.
 * This provides access to the local resources via
 * file:// uris.
 * Required to allow JavaFX access to bundle resources.
 * JavaFX does currently not support the access via bundle: uris.
 */
public class ResourceExtractor {
    private static final Logger logger = LoggerFactory.getLogger(ResourceExtractor.class);

    public static final String GUI_RESOURCES = "/gui/";
    public static final String RO_RESOURCES = "/ro/";
    public static final String OMEX_RESOURCES = "/omex/";

    public static final Set<String> RESOURCES;

    static {
        Set<String> set = new HashSet<>();
        set.add(GUI_RESOURCES);
        set.add(RO_RESOURCES);
        set.add(OMEX_RESOURCES);
        RESOURCES = Collections.unmodifiableSet(set);
    }

    private final BundleContext bc;
    private final File appDirectory;

    /**
     * Constructor.
     */
    public ResourceExtractor(final BundleContext bc, final File appDirectory) {
        this.bc = bc;
        this.appDirectory = appDirectory;
    }

    /**
     * Returns the file URI in the application folder for given resource string.
     * Example "/gui/query.html"
     * Replacement of
     * getClass().getResource("/gui/info.html");
     * which does not work for bundle resources in JavaFX.
     *
     * @param resource resource String
     * @return fileURI of resource, or null if not existing
     */
    public String getResource(String resource) {
        URI fileURI = fileURIforResource(resource);
        if (fileURI == null) {
            return null;
        } else {
            return fileURI.toString();
        }
    }

    /**
     * Returns the file URI in the application folder for given resource string.
     * Example "/gui/query.html"
     *
     * @param resource resource path
     * @return String representation of fileURI or null
     */
    public URI fileURIforResource(String resource) {
        if (appDirectory == null) {
            logger.error("appDirectory is not set in ResourceExtractor");
            return null;
        }
        // resource file
        File file = new File(appDirectory + resource);
        if (!file.exists()) {
            logger.error(String.format("Resource <%s> does not exist in <%s>.", resource, appDirectory));
            return null;
        }
        URI fileURI = file.toURI();
        return fileURI;
    }

    /**
     * Extracts the bundle resources from the BundleContext in the
     * application directory.
     * <p>
     * BundleContext and application directory have to be provided.
     */
    public void extract() {
        if (bc == null || appDirectory == null) {
            logger.error("BundleContext or application directory not set. Files not extracted");
            return;
        }
        logger.debug("-------------------------------------------------");
        logger.debug("Extract bundle resources");
        logger.debug("-------------------------------------------------");
        // bundle root
        Bundle bundle = bc.getBundle();
        URL rootURL = bundle.getEntry("/");
        for (String resource : RESOURCES) {
            extractDirectory(rootURL, resource);
        }
        logger.debug("-------------------------------------------------");
    }

    /**
     * Extract the resources in given directory.
     * Existing files are overwritten, files of older versions are not removed (#404).
     */
    private void extractDirectory(URL rootURL, String directory) {
        // list all GUI resources of bundle and extract them
        Enumeration<String> e = bc.getBundle().getEntryPaths(directory);

        while (e.hasMoreElements()) {
            String path = e.nextElement();

            // copy via stream from bundle URL to application file
            try {
                URL inURL = new URL(rootURL.toString() + path);

                try (InputStream inputStream = inURL.openConnection().getInputStream()) {
                    File outFile = new File(appDirectory + "/" + path);
                    // create directory
                    if (path.endsWith("/")) {
                        outFile.mkdirs();
                        // extract subdirectory recursively
                        extractDirectory(rootURL, "/" + path);
                    } else {
                        // create directories for file if required
                        File parent = outFile.getParentFile();
                        if (!parent.exists() && !parent.mkdirs()) {
                            throw new IllegalStateException("Couldn't create dir: " + parent);
                        }

                        logger.debug(" --> " + outFile.getAbsolutePath());
                        try (OutputStream outputStream = new FileOutputStream(outFile)) {
                            inputStream.transferTo(outputStream);
                        }
                    }
                } catch (IOException e1) {
                    logger.error("Directory could not be extracted", e1);
                    return;
                }

            } catch (MalformedURLException me) {
                logger.error("Problems with url", me);
                return;
            }
        }
    }
}
