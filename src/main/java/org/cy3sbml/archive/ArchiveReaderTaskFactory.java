package org.cy3sbml.archive;

import org.cytoscape.io.CyFileFilter;
import org.cytoscape.io.read.AbstractInputStreamTaskFactory;
import org.cytoscape.model.CyNetworkFactory;
import org.cytoscape.view.layout.CyLayoutAlgorithmManager;
import org.cytoscape.view.model.CyNetworkViewFactory;
import org.cytoscape.view.vizmap.VisualMappingManager;
import org.cytoscape.work.TaskIterator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;


/**
 * TaskFactory for ArchiveReaderTask.
 */
public class ArchiveReaderTaskFactory extends AbstractInputStreamTaskFactory {
    private static final Logger logger = LoggerFactory.getLogger(ArchiveReaderTaskFactory.class);

    private final CyNetworkFactory networkFactory;
    private final CyNetworkViewFactory networkViewFactory;
    private final VisualMappingManager visualMappingManager;
    private final CyLayoutAlgorithmManager layoutAlgorithmManager;

    /**
     * Constructor.
     */
    public ArchiveReaderTaskFactory(CyFileFilter filter,
                                    CyNetworkFactory networkFactory,
                                    CyNetworkViewFactory networkViewFactory,
                                    VisualMappingManager visualMappingManager,
                                    CyLayoutAlgorithmManager layoutAlgorithmManager) {
        super(filter);
        logger.debug("new ArchiveReaderTaskFactory");
        this.networkFactory = networkFactory;
        this.networkViewFactory = networkViewFactory;
        this.visualMappingManager = visualMappingManager;
        this.layoutAlgorithmManager = layoutAlgorithmManager;
    }

    /**
     * Create the TaskIterator.
     *
     * @param inputStream
     * @param inputName
     * @return
     */
    @Override
    public TaskIterator createTaskIterator(InputStream inputStream, String inputName) {
        logger.debug("createTaskIterator: input stream name: " + inputName);
        // BufferedInput stream even for the zip files

        ArchiveReaderTask task;
        try {
            task = new ArchiveReaderTask(
                    inputStream,
                    inputName,
                    networkFactory,
                    networkViewFactory,
                    visualMappingManager,
                    layoutAlgorithmManager);
        } catch (Exception e){
            task = null;
            logger.error("Error copying stream", e);
        }
        return new TaskIterator(task);
    }
}
