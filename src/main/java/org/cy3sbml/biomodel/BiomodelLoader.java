package org.cy3sbml.biomodel;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;
import org.cytoscape.task.read.LoadNetworkFileTaskFactory;
import org.cytoscape.work.AbstractTask;
import org.cytoscape.work.TaskIterator;
import org.cytoscape.work.TaskMonitor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Loads BioModels: downloads the SBML of each model into the {@code biomodels} folder of
 * the cy3sbml directory, {@code <id>.xml}, and loads it with the Cytoscape network file
 * loader.
 * <p>
 * All web service access runs in the tasks, so the task iterator can be created and
 * executed from the Swing event dispatch thread.
 */
public class BiomodelLoader {
    private static final Logger logger = LoggerFactory.getLogger(BiomodelLoader.class);

    /** Suffix of the downloaded files, has to match the SBML reader. */
    public static final String SUFFIX = ".xml";

    private static final Pattern VALID_ID = Pattern.compile("[A-Za-z0-9_]+");

    private final BiomodelsQuery query;
    private final Path directory;
    private final LoadNetworkFileTaskFactory loadNetworkFileTaskFactory;

    /**
     * @param directory the folder of the downloaded SBML files, created if missing
     */
    public BiomodelLoader(BiomodelsQuery query, Path directory, LoadNetworkFileTaskFactory loadNetworkFileTaskFactory) {
        this.query = query;
        this.directory = directory;
        this.loadNetworkFileTaskFactory = loadNetworkFileTaskFactory;
    }

    /**
     * Creates the tasks that download and load the given BioModels, one after another. A
     * model that cannot be downloaded does not stop the others; the last task fails with
     * the models that could not be downloaded.
     */
    public TaskIterator createTaskIterator(Collection<String> ids) {
        List<String> failures = Collections.synchronizedList(new ArrayList<>());
        TaskIterator iterator = new TaskIterator();
        for (String id : ids) {
            iterator.append(new LoadBiomodelTask(id, failures));
        }
        iterator.append(new ReportFailuresTask(failures));
        return iterator;
    }

    /**
     * Downloads the SBML of the given BioModel into {@code <directory>/<id>.xml}, replacing
     * an earlier download only if the download succeeded.
     *
     * @return the downloaded file
     * @throws IOException if the id is invalid or the download failed
     */
    Path download(String id) throws IOException {
        if (!VALID_ID.matcher(id).matches()) {
            throw new IOException("Invalid BioModel id: " + id);
        }
        Files.createDirectories(directory);
        Path file = directory.resolve(id + SUFFIX);
        Path part = Files.createTempFile(directory, id, ".part");
        try {
            query.downloadSBML(id, part);
            Files.move(part, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } finally {
            Files.deleteIfExists(part);
        }
        return file;
    }

    /**
     * Downloads the SBML of a BioModel and inserts the tasks that load it.
     */
    private final class LoadBiomodelTask extends AbstractTask {
        private final String id;
        private final List<String> failures;
        private final Object lock = new Object();
        /** The thread running the download, guarded by lock. */
        private Thread runner;

        LoadBiomodelTask(String id, List<String> failures) {
            this.id = id;
            this.failures = failures;
        }

        @Override
        public void run(TaskMonitor taskMonitor) {
            taskMonitor.setTitle("Load BioModel " + id);
            taskMonitor.setStatusMessage("Downloading the SBML of " + id + " from BioModels ...");
            synchronized (lock) {
                if (cancelled) {
                    return;
                }
                runner = Thread.currentThread();
            }
            Path file;
            try {
                file = download(id);
            } catch (IOException e) {
                if (cancelled) {
                    return;
                }
                logger.warn("Could not download BioModel {}: {}", id, e.getMessage());
                failures.add(id + ": " + e.getMessage());
                taskMonitor.showMessage(TaskMonitor.Level.ERROR, "Could not download BioModel " + id);
                return;
            } finally {
                synchronized (lock) {
                    runner = null;
                }
                // clear the interrupt of a cancel that came after the download
                Thread.interrupted();
            }
            if (!cancelled) {
                insertTasksAfterCurrentTask(loadNetworkFileTaskFactory.createTaskIterator(file.toFile()));
            }
        }

        @Override
        public void cancel() {
            synchronized (lock) {
                super.cancel();
                // stops a running download
                if (runner != null) {
                    runner.interrupt();
                }
            }
        }
    }

    /**
     * Fails with the BioModels that could not be downloaded, so Cytoscape reports them.
     */
    private static final class ReportFailuresTask extends AbstractTask {
        private final List<String> failures;

        ReportFailuresTask(List<String> failures) {
            this.failures = failures;
        }

        @Override
        public void run(TaskMonitor taskMonitor) throws IOException {
            synchronized (failures) {
                if (!failures.isEmpty()) {
                    throw new IOException(
                            "No SBML could be downloaded for the BioModels:\n" + String.join("\n", failures));
                }
            }
        }
    }
}
