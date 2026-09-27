package org.cy3sbml.biomodel;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
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

    private final String id;
    private final ServiceAdapter adapter;
    private File file;
    private String error;

    public LoadBioModelTaskFactory(String id, BiomodelsQuery query, ServiceAdapter adapter) {
        this.id = id;
        this.adapter = adapter;

        try {
            // download to a tmp file and use the core-task read Network from file task,
            // a failed download removes the file
            File tempFile = File.createTempFile(id, SUFFIX);
            tempFile.deleteOnExit();
            query.downloadSBML(id, tempFile.toPath());
            file = tempFile;
        } catch (IOException e) {
            error = e.getMessage();
            logger.warn("Could not download BioModel {}: {}", id, error);
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

    /**
     * Downloads the SBML of the given BioModels, one factory per id. Accesses the web
     * service, so it must not run on the Swing event dispatch thread.
     */
    public static List<LoadBioModelTaskFactory> download(
            List<String> ids, BiomodelsQuery query, ServiceAdapter adapter) {
        List<LoadBioModelTaskFactory> factories = new ArrayList<>();
        for (String id : ids) {
            factories.add(new LoadBioModelTaskFactory(id, query, adapter));
        }
        return factories;
    }

    /** Returns the BioModel id. */
    public String getId() {
        return id;
    }

    /** Returns the reason why the SBML could not be downloaded, null if it was. */
    public String getError() {
        return error;
    }
}
