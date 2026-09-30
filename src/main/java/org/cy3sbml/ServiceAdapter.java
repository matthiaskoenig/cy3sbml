package org.cy3sbml;

import java.io.File;
import java.util.Properties;
import org.cytoscape.application.CyApplicationManager;
import org.cytoscape.application.swing.CySwingApplication;
import org.cytoscape.group.CyGroupFactory;
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
import org.cytoscape.work.swing.DialogTaskManager;

/**
 * The Cytoscape services and app settings that the actions, readers and the info panel need,
 * so that they do not each take a long list of constructor arguments. {@link CyActivator}
 * creates the one instance.
 */
public class ServiceAdapter {
    public final CySwingApplication cySwingApplication;
    public final CyApplicationManager cyApplicationManager;
    public final CyNetworkManager cyNetworkManager;
    public final CyNetworkViewManager cyNetworkViewManager;
    public final VisualMappingManager visualMappingManager;
    public final CyLayoutAlgorithmManager cyLayoutAlgorithmManager;
    public final DialogTaskManager dialogTaskManager;

    public final CyNetworkFactory cyNetworkFactory;
    public final CyGroupFactory cyGroupFactory;
    public final CyNetworkViewFactory cyNetworkViewFactory;

    /** The cy3sbml properties ({@code cy3sbml.props}). */
    public final CyProperty<Properties> cy3sbmlProperties;
    /** The app directory in the Cytoscape configuration directory. */
    public final File cy3sbmlDirectory;

    public final OpenBrowser openBrowser;
    public final LoadNetworkFileTaskFactory loadNetworkFileTaskFactory;
    public final FileUtil fileUtil;

    /**
     * Creates the adapter; a service a test does not need may be null.
     *
     * @param cySwingApplication the Swing application
     * @param cyApplicationManager the application manager
     * @param cyNetworkManager the network manager
     * @param cyNetworkViewManager the network view manager
     * @param visualMappingManager the visual mapping manager
     * @param cyLayoutAlgorithmManager the layout algorithm manager
     * @param dialogTaskManager the task manager with a progress dialog
     * @param cyNetworkFactory the network factory
     * @param cyGroupFactory the group factory
     * @param cyNetworkViewFactory the network view factory
     * @param cy3sbmlProperties the cy3sbml properties
     * @param cy3sbmlDirectory the app directory
     * @param openBrowser the browser service of Cytoscape
     * @param loadNetworkFileTaskFactory the task factory to load network files
     * @param fileUtil the file dialogs of Cytoscape
     */
    public ServiceAdapter(
            CySwingApplication cySwingApplication,
            CyApplicationManager cyApplicationManager,
            CyNetworkManager cyNetworkManager,
            CyNetworkViewManager cyNetworkViewManager,
            VisualMappingManager visualMappingManager,
            CyLayoutAlgorithmManager cyLayoutAlgorithmManager,
            DialogTaskManager dialogTaskManager,
            CyNetworkFactory cyNetworkFactory,
            CyGroupFactory cyGroupFactory,
            CyNetworkViewFactory cyNetworkViewFactory,
            CyProperty<Properties> cy3sbmlProperties,
            File cy3sbmlDirectory,
            OpenBrowser openBrowser,
            LoadNetworkFileTaskFactory loadNetworkFileTaskFactory,
            FileUtil fileUtil) {
        this.cySwingApplication = cySwingApplication;
        this.cyApplicationManager = cyApplicationManager;
        this.cyNetworkManager = cyNetworkManager;
        this.cyNetworkViewManager = cyNetworkViewManager;
        this.visualMappingManager = visualMappingManager;
        this.cyLayoutAlgorithmManager = cyLayoutAlgorithmManager;
        this.dialogTaskManager = dialogTaskManager;
        this.cyNetworkFactory = cyNetworkFactory;
        this.cyGroupFactory = cyGroupFactory;
        this.cyNetworkViewFactory = cyNetworkViewFactory;
        this.cy3sbmlProperties = cy3sbmlProperties;
        this.cy3sbmlDirectory = cy3sbmlDirectory;
        this.openBrowser = openBrowser;
        this.loadNetworkFileTaskFactory = loadNetworkFileTaskFactory;
        this.fileUtil = fileUtil;
    }
}
