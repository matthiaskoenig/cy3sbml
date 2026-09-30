package org.cy3sbml;

import java.io.IOException;
import java.io.InputStream;
import org.cy3sbml.reader.FailedReaderTask;
import org.cy3sbml.reader.SBMLReaderTask;
import org.cy3sbml.util.IOUtil;
import org.cytoscape.io.read.AbstractInputStreamTaskFactory;
import org.cytoscape.work.TaskIterator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * SBMLReaderTaskFactory class
 * TaskFactory for the SBMLReaderTask.
 */
public class SBMLReaderTaskFactory extends AbstractInputStreamTaskFactory {
    private static final Logger logger = LoggerFactory.getLogger(SBMLReaderTaskFactory.class);
    private final SBMLFileFilter filter;
    private final ServiceAdapter adapter;
    private final SBMLManager sbmlManager;

    /**
     * Constructor.
     */
    public SBMLReaderTaskFactory(SBMLFileFilter filter, ServiceAdapter adapter, SBMLManager sbmlManager) {
        super(filter);
        this.filter = filter;
        this.adapter = adapter;
        this.sbmlManager = sbmlManager;
    }

    @Override
    public TaskIterator createTaskIterator(InputStream is, String inputName) {
        logger.debug("createTaskIterator: input stream name: {}", inputName);

        try {
            return new TaskIterator(new SBMLReaderTask(
                    IOUtil.copyInputStream(is),
                    inputName,
                    filter.takeAcceptedUri(inputName).orElse(null),
                    adapter.cyNetworkFactory,
                    adapter.cyGroupFactory,
                    adapter.cyNetworkViewFactory,
                    adapter.visualMappingManager,
                    adapter.cyLayoutAlgorithmManager,
                    adapter.cy3sbmlProperties,
                    sbmlManager));
        } catch (IOException e) {
            logger.error("Error in creating TaskIterator for SBMLReaderTaskFactory.", e);
            return new TaskIterator(new FailedReaderTask(inputName, e));
        }
    }
}
