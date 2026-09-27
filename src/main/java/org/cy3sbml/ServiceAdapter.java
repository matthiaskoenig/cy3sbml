package org.cy3sbml;

import java.io.File;
import java.util.Properties;
import org.cytoscape.application.CyApplicationManager;
import org.cytoscape.application.swing.CySwingApplication;
import org.cytoscape.group.CyGroupFactory;
import org.cytoscape.io.util.StreamUtil;
import org.cytoscape.model.CyNetworkFactory;
import org.cytoscape.model.CyNetworkManager;
import org.cytoscape.property.CyProperty;
import org.cytoscape.task.read.LoadNetworkFileTaskFactory;
import org.cytoscape.util.swing.FileUtil;
import org.cytoscape.util.swing.OpenBrowser;
import org.cytoscape.view.layout.CyLayoutAlgorithmManager;
import org.cytoscape.view.model.CyNetworkViewFactory;
import org.cytoscape.view.model.CyNetworkViewManager;
import org.cytoscape.view.vizmap.VisualMappingManager;
import org.cytoscape.work.SynchronousTaskManager;
import org.cytoscape.work.TaskManager;
import org.cytoscape.work.swing.DialogTaskManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Adapter for working with services.
 * <p>
 * Avoids to have to pass around the services to everywhere, but provides
 * a one-stop shop for getting the necessary things.
 */
public class ServiceAdapter {
    private static final Logger logger = LoggerFactory.getLogger(ServiceAdapter.class);

    public final CySwingApplication cySwingApplication;
    public final CyApplicationManager cyApplicationManager;
    public final CyNetworkManager cyNetworkManager;
    public final CyNetworkViewManager cyNetworkViewManager;
    public final VisualMappingManager visualMappingManager;
    public final CyLayoutAlgorithmManager cyLayoutAlgorithmManager;
    public final DialogTaskManager dialogTaskManager;

    @SuppressWarnings("rawtypes")
    public final SynchronousTaskManager synchronousTaskManager;

    @SuppressWarnings("rawtypes")
    public final TaskManager taskManager;

    public final CyNetworkFactory cyNetworkFactory;
    public final CyGroupFactory cyGroupFactory;
    public final CyNetworkViewFactory cyNetworkViewFactory;

    public final CyProperty<Properties> cy3sbmlProperties;
    public final File cy3sbmlDirectory;
    public final StreamUtil streamUtil;
    public final OpenBrowser openBrowser;
    public final ConnectionProxy connectionProxy;
    public final LoadNetworkFileTaskFactory loadNetworkFileTaskFactory;
    public final FileUtil fileUtil;

    @SuppressWarnings("rawtypes")
    public ServiceAdapter(
            CySwingApplication cySwingApplication,
            CyApplicationManager cyApplicationManager,
            CyNetworkManager cyNetworkManager,
            CyNetworkViewManager cyNetworkViewManager,
            VisualMappingManager visualMappingManager,
            CyLayoutAlgorithmManager cyLayoutAlgorithmManager,
            DialogTaskManager dialogTaskManager,
            SynchronousTaskManager synchronousTaskManager,
            TaskManager taskManager,
            CyNetworkFactory cyNetworkFactory,
            CyGroupFactory cyGroupFactory,
            CyNetworkViewFactory cyNetworkViewFactory,
            CyProperty<Properties> cy3sbmlProperties,
            File cy3sbmlDirectory,
            StreamUtil streamUtil,
            OpenBrowser openBrowser,
            ConnectionProxy connectionProxy,
            LoadNetworkFileTaskFactory loadNetworkFileTaskFactory,
            FileUtil fileUtil) {
        logger.debug("ServiceAdapter created");
        this.cySwingApplication = cySwingApplication;
        this.cyApplicationManager = cyApplicationManager;
        this.cyNetworkManager = cyNetworkManager;
        this.cyNetworkViewManager = cyNetworkViewManager;
        this.visualMappingManager = visualMappingManager;
        this.cyLayoutAlgorithmManager = cyLayoutAlgorithmManager;
        this.dialogTaskManager = dialogTaskManager;
        this.synchronousTaskManager = synchronousTaskManager;
        this.taskManager = taskManager;
        this.cyNetworkFactory = cyNetworkFactory;
        this.cyGroupFactory = cyGroupFactory;
        this.cyNetworkViewFactory = cyNetworkViewFactory;
        this.cy3sbmlProperties = cy3sbmlProperties;
        this.cy3sbmlDirectory = cy3sbmlDirectory;
        this.streamUtil = streamUtil;
        this.openBrowser = openBrowser;
        this.connectionProxy = connectionProxy;
        this.loadNetworkFileTaskFactory = loadNetworkFileTaskFactory;
        this.fileUtil = fileUtil;
    }
}
