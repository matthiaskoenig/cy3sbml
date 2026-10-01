package org.cy3sbml.archive;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.cy3sbml.SBML;
import org.cy3sbml.SBMLManager;
import org.cy3sbml.SBMLReaderError;
import org.cy3sbml.reader.SBMLReaderTask;
import org.cy3sbml.util.AttributeUtil;
import org.cytoscape.io.read.CyNetworkReader;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.subnetwork.CyRootNetwork;
import org.cytoscape.model.subnetwork.CySubNetwork;
import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.work.AbstractTask;
import org.cytoscape.work.TaskMonitor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Reads the SBML models of a COMBINE archive (OMEX): the archive is unpacked, and every
 * SBML file to import ({@link ArchiveInfo#modelsToImport()}) is read by an
 * {@link SBMLReaderTask} with the unpacked file as its location, so that the SBML files of
 * the archive can reference each other (comp external model definitions).
 * <p>
 * An SBML file that cannot be read is reported and skipped; the task fails if none can be
 * read. The unpacked files are deleted when the task is done: the SBML reader reads the
 * external models of a document during the import.
 */
public class CombineArchiveReaderTask extends AbstractTask implements CyNetworkReader {
    private static final Logger logger = LoggerFactory.getLogger(CombineArchiveReaderTask.class);

    /** Creates the reader of an SBML file of the archive. */
    @FunctionalInterface
    public interface SBMLReaders {
        SBMLReaderTask create(InputStream stream, String fileName, URI location);
    }

    private final InputStream stream;
    private final String fileName;
    private final ArchiveDirectories directories;
    private final SBMLReaders readers;
    private final SBMLManager sbmlManager;

    private ArchiveInfo info;
    // the reader of every network, in the order they were read
    private final Map<CyNetwork, SBMLReaderTask> readerOfNetwork = new LinkedHashMap<>();
    private volatile SBMLReaderTask current;

    /**
     * @param sbmlManager the manager the archive of the imported models is registered in;
     *     null outside of Cytoscape
     */
    public CombineArchiveReaderTask(
            InputStream stream,
            String fileName,
            ArchiveDirectories directories,
            SBMLReaders readers,
            SBMLManager sbmlManager) {
        this.stream = stream;
        this.fileName = fileName;
        this.directories = directories;
        this.readers = readers;
        this.sbmlManager = sbmlManager;
    }

    /** The content of the archive, null before the task ran. */
    public ArchiveInfo getArchiveInfo() {
        return info;
    }

    @Override
    public void run(TaskMonitor taskMonitor) throws Exception {
        taskMonitor.setTitle("cy3sbml COMBINE archive reader");
        Path directory = directories.newDirectory(fileName);
        try {
            readArchive(directory, taskMonitor);
        } finally {
            // the SBML reader has read the files, the external models included
            directories.delete(directory);
        }
    }

    private void readArchive(Path directory, TaskMonitor taskMonitor) throws Exception {
        try {
            info = CombineArchive.extract(stream, fileName, directory);
        } catch (CombineArchiveException e) {
            logger.error(e.getMessage());
            throw new SBMLReaderError(e.getMessage(), e);
        }

        List<ArchiveInfo.Entry> models = info.modelsToImport();
        List<String> errors = new ArrayList<>();
        for (int i = 0; i < models.size() && !cancelled; i++) {
            ArchiveInfo.Entry entry = models.get(i);
            taskMonitor.setStatusMessage("Reading " + entry.location());
            taskMonitor.setProgress((double) i / models.size());
            try {
                read(entry, directory.resolve(entry.location()), taskMonitor);
            } catch (SBMLReaderError | IOException e) {
                errors.add(e.getMessage());
                taskMonitor.showMessage(TaskMonitor.Level.ERROR, e.getMessage());
            }
        }
        if (readerOfNetwork.isEmpty() && !cancelled) {
            throw new SBMLReaderError(String.format(
                    "cy3sbml could not read the SBML files of the archive %s: %s", fileName, String.join(" ", errors)));
        }
        taskMonitor.setProgress(1.0);
    }

    private void read(ArchiveInfo.Entry entry, Path file, TaskMonitor taskMonitor) throws Exception {
        try (InputStream sbml = Files.newInputStream(file)) {
            SBMLReaderTask reader = readers.create(sbml, file.getFileName().toString(), file.toUri());
            current = reader;
            reader.run(taskMonitor);
            for (CyNetwork network : reader.getNetworks()) {
                readerOfNetwork.put(network, reader);
                CyRootNetwork root = ((CySubNetwork) network).getRootNetwork();
                AttributeUtil.set(root, root, SBML.ATTR_ARCHIVE, fileName, String.class);
                // registered like the SBML, also for a network Cytoscape creates no view for
                if (sbmlManager != null) {
                    sbmlManager.addArchive(root.getSUID(), new ArchiveImport(info, entry.location()));
                }
            }
        } finally {
            current = null;
        }
    }

    @Override
    public void cancel() {
        super.cancel();
        SBMLReaderTask reader = current;
        if (reader != null) {
            reader.cancel();
        }
    }

    @Override
    public CyNetwork[] getNetworks() {
        return readerOfNetwork.keySet().toArray(new CyNetwork[0]);
    }

    /** The view of the network by the reader that read it. */
    @Override
    public CyNetworkView buildCyNetworkView(CyNetwork network) {
        SBMLReaderTask reader = readerOfNetwork.get(network);
        if (reader == null) {
            throw new IllegalArgumentException("The network was not read from the archive " + fileName);
        }
        return reader.buildCyNetworkView(network);
    }
}
