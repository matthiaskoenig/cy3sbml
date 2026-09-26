package org.cy3sbml.biomodel;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.apache.commons.io.IOUtils;
import org.cy3sbml.ServiceAdapter;
import org.cytoscape.work.TaskFactory;
import org.cytoscape.work.TaskIterator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Downloads the SBML of a BioModel into a temporary file and loads it with the
 * Cytoscape network file loader.
 * <p>
 * The factory is only ready if the download succeeded, callers check {@link #isReady()}
 * before creating the task iterator.
 */
public class LoadBioModelTaskFactory implements TaskFactory {
    private static final Logger logger = LoggerFactory.getLogger(LoadBioModelTaskFactory.class);
    public static final String SUFFIX = ".xml"; // has to match the reader

    private ServiceAdapter adapter;
    private File file;

    public LoadBioModelTaskFactory(String id, ServiceAdapter adapter) {
        this.adapter = adapter;

        try {
            String sbml = BiomodelsQuery.getBioModelSBMLById(id);

            if (sbml == null || sbml.equals("") || sbml.startsWith(id)) {
                logger.warn("No SBML for BioModel: {}", id);
                return;
            }
            InputStream instream = new ByteArrayInputStream(sbml.getBytes(StandardCharsets.UTF_8));
            // convert to tmp file and use the core-task read Network from file task
            final File tempFile = File.createTempFile(id, SUFFIX);
            tempFile.deleteOnExit();

            try (FileOutputStream out = new FileOutputStream(tempFile)) {
                IOUtils.copy(instream, out);
            }
            file = tempFile;
        } catch (IOException e) {
            logger.error("Could not load biomodel: {}", id, e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.error("Interrupted while loading biomodel: {}", id, e);
        }
    }

    @Override
    public TaskIterator createTaskIterator() {
        if (file == null) {
            throw new IllegalStateException("BioModel SBML was not downloaded, check isReady()");
        }
        return adapter.loadNetworkFileTaskFactory.createTaskIterator(file);
    }

    /** Returns true if the SBML was downloaded. */
    @Override
    public boolean isReady() {
        return file != null;
    }
}
