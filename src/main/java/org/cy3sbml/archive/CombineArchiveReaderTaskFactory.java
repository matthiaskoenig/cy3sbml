package org.cy3sbml.archive;

import java.io.IOException;
import java.io.InputStream;
import org.cy3sbml.SBMLManager;
import org.cy3sbml.ServiceAdapter;
import org.cy3sbml.reader.FailedReaderTask;
import org.cy3sbml.reader.SBMLReaderTask;
import org.cy3sbml.util.IOUtil;
import org.cytoscape.io.read.AbstractInputStreamTaskFactory;
import org.cytoscape.work.TaskIterator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Creates the task that reads the SBML models of a COMBINE archive.
 */
public class CombineArchiveReaderTaskFactory extends AbstractInputStreamTaskFactory {
    private static final Logger logger = LoggerFactory.getLogger(CombineArchiveReaderTaskFactory.class);

    private final ServiceAdapter adapter;
    private final SBMLManager sbmlManager;
    private final ArchiveDirectories directories;

    /**
     * @param directories the directories the archives are unpacked into
     */
    public CombineArchiveReaderTaskFactory(
            CombineArchiveFileFilter filter,
            ServiceAdapter adapter,
            SBMLManager sbmlManager,
            ArchiveDirectories directories) {
        super(filter);
        this.adapter = adapter;
        this.sbmlManager = sbmlManager;
        this.directories = directories;
    }

    @Override
    public TaskIterator createTaskIterator(InputStream is, String inputName) {
        try {
            return new TaskIterator(new CombineArchiveReaderTask(
                    IOUtil.copyInputStream(is),
                    inputName,
                    directories,
                    (stream, fileName, location) -> new SBMLReaderTask(
                            stream,
                            fileName,
                            location,
                            adapter.cyNetworkFactory,
                            adapter.cyGroupFactory,
                            adapter.cyNetworkViewFactory,
                            adapter.visualMappingManager,
                            adapter.cyLayoutAlgorithmManager,
                            adapter.cy3sbmlProperties,
                            sbmlManager),
                    sbmlManager));
        } catch (IOException e) {
            logger.error("Error in creating TaskIterator for CombineArchiveReaderTaskFactory.", e);
            return new TaskIterator(new FailedReaderTask(inputName, e));
        }
    }
}
