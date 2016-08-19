package org.cy3sbml;

import org.cy3sbml.gui.ValidationPanel;
import org.cytoscape.group.CyGroupFactory;
import org.osgi.framework.BundleContext;


import java.io.File;
import java.util.Properties;

import org.cytoscape.model.events.NetworkAboutToBeDestroyedListener;
import org.cytoscape.property.CyProperty;
import org.cytoscape.property.PropertyUpdatedListener;
import org.cytoscape.service.util.AbstractCyActivator;
import org.cytoscape.session.events.SessionAboutToBeSavedListener;
import org.cytoscape.session.events.SessionLoadedListener;
import org.cytoscape.task.read.LoadNetworkFileTaskFactory;
import org.cytoscape.task.read.LoadVizmapFileTaskFactory;
import org.cytoscape.application.CyApplicationConfiguration;
import org.cytoscape.application.CyApplicationManager;
import org.cytoscape.application.events.SetCurrentNetworkListener;
import org.cytoscape.application.swing.CyAction;
import org.cytoscape.application.swing.CySwingApplication;
import org.cytoscape.application.swing.CytoPanelComponent;
import org.cytoscape.model.CyNetworkFactory;
import org.cytoscape.model.CyNetworkManager;
import org.cytoscape.model.events.NetworkAddedListener;
import org.cytoscape.model.events.RowsSetListener;
import org.cytoscape.view.layout.CyLayoutAlgorithmManager;
import org.cytoscape.view.model.CyNetworkViewFactory;
import org.cytoscape.view.model.CyNetworkViewManager;
import org.cytoscape.view.model.events.NetworkViewAboutToBeDestroyedListener;
import org.cytoscape.view.model.events.NetworkViewAddedListener;
import org.cytoscape.view.vizmap.VisualMappingManager;
import org.cytoscape.work.SynchronousTaskManager;
import org.cytoscape.work.TaskManager;
import org.cytoscape.work.swing.DialogTaskManager;
import org.cytoscape.io.util.StreamUtil;
import org.cytoscape.util.swing.FileUtil;
import org.cytoscape.util.swing.OpenBrowser;


import org.cy3sbml.actions.BiomodelsAction;
import org.cy3sbml.actions.ChangeStateAction;
import org.cy3sbml.actions.CofactorAction;
import org.cy3sbml.actions.ExamplesAction;
import org.cy3sbml.actions.HelpAction;
import org.cy3sbml.actions.ImportAction;
import org.cy3sbml.actions.LoadLayoutAction;
import org.cy3sbml.actions.SaveLayoutAction;
import org.cy3sbml.actions.ValidationAction;

import org.cy3sbml.cofactors.CofactorManager;
import org.cy3sbml.gui.SBaseHTMLFactory;
import org.cy3sbml.gui.WebViewPanel;
import org.cy3sbml.miriam.RegistryUtil;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Entry point to cy3sbml.
 * <p>
 * The CyActivator registers the cy3sbml services with OSGI. This is the class
 * used for startup of the app by Cytoscape 3.
 */
public class CyActivator extends AbstractCyActivator {
    public static final String PROPERTIES_FILE = "cy3sbml.props";
    private static Logger logger;

    public CyActivator() {
        super();
    }

    /**
     * Starts the cy3sbml OSGI bundle.
     */
    public void start(BundleContext bc) {
        try {
            BundleInformation bundleInfo = new BundleInformation(bc);

            // Default configuration directory used for all cy3sbml files
            // Used for retrieving
            CyApplicationConfiguration configuration = getService(bc, CyApplicationConfiguration.class);
            File cyDirectory = configuration.getConfigurationDirectoryLocation();
            File appDirectory = new File(cyDirectory, bundleInfo.getName());

            if (appDirectory.exists() == false) {
                appDirectory.mkdir();
            }
            // store bundle information (for display of dependencies, versions, ...)
            File logFile = new File(appDirectory, bundleInfo.getInfo() + ".log");
            System.setProperty("logfile.name", logFile.getAbsolutePath());
            logger = LoggerFactory.getLogger(CyActivator.class);

            logger.info("----------------------------");
            logger.info("Start " + bundleInfo.getInfo());
            logger.info("----------------------------");
            logger.info("directory = " + appDirectory.getAbsolutePath());
            logger.info("logfile = " + logFile.getAbsolutePath());

            // Set baseDir for HTML generation
            // allows the dynamical generated HTML to resolve the gui resources
            SBaseHTMLFactory.setBaseDirFromAppDir(appDirectory);

            // cy3sbml properties
            PropsReader propsReader = new PropsReader(bundleInfo.getName(), PROPERTIES_FILE);
            Properties propsReaderServiceProps = new Properties();
            propsReaderServiceProps.setProperty("cyPropertyName", PROPERTIES_FILE);
            registerAllServices(bc, propsReader, propsReaderServiceProps);

            /** Get services */
            CySwingApplication cySwingApplication = getService(bc, CySwingApplication.class);

            CyApplicationManager cyApplicationManager = getService(bc, CyApplicationManager.class);
            CyNetworkManager cyNetworkManager = getService(bc, CyNetworkManager.class);
            CyNetworkViewManager cyNetworkViewManager = getService(bc, CyNetworkViewManager.class);
            VisualMappingManager visualMappingManager = getService(bc, VisualMappingManager.class);
            CyLayoutAlgorithmManager cyLayoutAlgorithmManager = getService(bc, CyLayoutAlgorithmManager.class);

            DialogTaskManager dialogTaskManager = getService(bc, DialogTaskManager.class);
            @SuppressWarnings("rawtypes")
            SynchronousTaskManager synchronousTaskManager = getService(bc, SynchronousTaskManager.class);
            @SuppressWarnings("rawtypes")
            TaskManager taskManager = getService(bc, TaskManager.class);

            CyNetworkFactory cyNetworkFactory = getService(bc, CyNetworkFactory.class);
            CyNetworkViewFactory cyNetworkViewFactory = getService(bc, CyNetworkViewFactory.class);

            @SuppressWarnings("unchecked")
            CyProperty<Properties> cyProperties = getService(bc, CyProperty.class, "(cyPropertyName=cytoscape3.props)");
            @SuppressWarnings("unchecked")
            CyProperty<Properties> appProperties = getService(bc, CyProperty.class, "(cyPropertyName=cy3sbml.props)");
            StreamUtil streamUtil = getService(bc, StreamUtil.class);
            OpenBrowser openBrowser = getService(bc, OpenBrowser.class);
            FileUtil fileUtil = getService(bc, FileUtil.class);

            CyGroupFactory cyGroupFactory = getService(bc, CyGroupFactory.class);

            LoadNetworkFileTaskFactory loadNetworkFileTaskFactory = getService(bc, LoadNetworkFileTaskFactory.class);

            // Use Cytoscape properties to set proxy for webservices
            ConnectionProxy connectionProxy = new ConnectionProxy(cyProperties);
            connectionProxy.setSystemProxyFromCyProperties();

            /** Create ServiceAdapter */
            ServiceAdapter adapter = ServiceAdapter.getInstance(
                    cySwingApplication,
                    cyApplicationManager,
                    cyNetworkManager,
                    cyNetworkViewManager,
                    visualMappingManager,
                    cyLayoutAlgorithmManager,
                    dialogTaskManager,
                    synchronousTaskManager,
                    taskManager,

                    cyNetworkFactory,
                    cyGroupFactory,
                    cyNetworkViewFactory,

                    appProperties,
                    appDirectory,
                    streamUtil,
                    openBrowser,
                    connectionProxy,
                    loadNetworkFileTaskFactory,
                    fileUtil
            );

            // load visual styles
            LoadVizmapFileTaskFactory loadVizmapFileTaskFactory = getService(bc, LoadVizmapFileTaskFactory.class);
            SBMLStyleManager sbmlStyleManager = SBMLStyleManager.getInstance(loadVizmapFileTaskFactory, visualMappingManager);
            sbmlStyleManager.loadStyles();
            registerService(bc, sbmlStyleManager, SessionLoadedListener.class, new Properties());

            // SBMLManager
            SBMLManager sbmlManager = SBMLManager.getInstance(cyApplicationManager);
            registerService(bc, sbmlManager, NetworkAboutToBeDestroyedListener.class, new Properties());

            // Cofactor manager
            CofactorManager cofactorManager = CofactorManager.getInstance();

            // init actions [100 - 120]
            // FIXME: currently not possible to set separators in menu bar
            // JToolBar toolBar = cySwingApplication.getJToolBar();
            // toolBar.addSeparator(new Dimension(89.0));

            ChangeStateAction changeStateAction = new ChangeStateAction();
            registerService(bc, changeStateAction, CyAction.class, new Properties());

            ImportAction importAction = new ImportAction(adapter);
            registerService(bc, importAction, CyAction.class, new Properties());

            ValidationAction validationAction = new ValidationAction(adapter);
            registerService(bc, validationAction, CyAction.class, new Properties());

            ExamplesAction examplesAction = new ExamplesAction();
            registerService(bc, examplesAction, CyAction.class, new Properties());

            CofactorAction cofactorAction = new CofactorAction(adapter);
            registerService(bc, cofactorAction, CyAction.class, new Properties());

            BiomodelsAction biomodelsAction = new BiomodelsAction(adapter);
            registerService(bc, biomodelsAction, CyAction.class, new Properties());

            HelpAction helpAction = new HelpAction();
            registerService(bc, helpAction, CyAction.class, new Properties());

            SaveLayoutAction saveLayoutAction = new SaveLayoutAction(adapter);
            registerService(bc, saveLayoutAction, CyAction.class, new Properties());

            LoadLayoutAction loadLayoutAction = new LoadLayoutAction(adapter);
            registerService(bc, loadLayoutAction, CyAction.class, new Properties());


            // SBML file reader
            SBMLFileFilter sbmlFilter = new SBMLFileFilter(streamUtil);
            SBMLReader sbmlReader = new SBMLReader(sbmlFilter, adapter);
            Properties sbmlReaderProps = new Properties();
            sbmlReaderProps.setProperty("readerDescription", "SBML file reader (cy3sbml)");
            sbmlReaderProps.setProperty("readerId", "cy3sbmlNetworkReader");
            registerAllServices(bc, sbmlReader, sbmlReaderProps);

            // Session loading & saving
            SessionData sessionData = new SessionData();
            registerService(bc, sessionData, SessionAboutToBeSavedListener.class, new Properties());
            registerService(bc, sessionData, SessionLoadedListener.class, new Properties());

            // proxy listener
            registerService(bc, connectionProxy, PropertyUpdatedListener.class, new Properties());

            // panels
            WebViewPanel webViewPanel = WebViewPanel.getInstance(adapter);
            registerService(bc, webViewPanel, CytoPanelComponent.class, new Properties());
            registerService(bc, webViewPanel, RowsSetListener.class, new Properties());
            registerService(bc, webViewPanel, SetCurrentNetworkListener.class, new Properties());
            registerService(bc, webViewPanel, NetworkAddedListener.class, new Properties());
            registerService(bc, webViewPanel, NetworkViewAddedListener.class, new Properties());
            registerService(bc, webViewPanel, NetworkViewAboutToBeDestroyedListener.class, new Properties());

            ValidationPanel validationPanel = ValidationPanel.getInstance(adapter);
            registerService(bc, validationPanel, CytoPanelComponent.class, new Properties());
            registerService(bc, validationPanel, SetCurrentNetworkListener.class, new Properties());
            registerService(bc, validationPanel, NetworkAddedListener.class, new Properties());
            registerService(bc, validationPanel, NetworkViewAddedListener.class, new Properties());
            registerService(bc, validationPanel, NetworkViewAboutToBeDestroyedListener.class, new Properties());


            // register services for other apps
            registerService(bc, sbmlManager, SBMLManager.class, new Properties());

            // Extract all resource files for JavaFX (no bundle access)
            final ResourceExtractor resourceHandler = new ResourceExtractor(bc, appDirectory);
            resourceHandler.extract();

            // Update and load registry
            File miriamFile = new File(appDirectory + File.separator + RegistryUtil.FILENAME_MIRIAM);
            RegistryUtil.updateMiriamXMLWithNewer(miriamFile);
            RegistryUtil.loadRegistry(miriamFile);

            // cy3sbml panels
            ValidationPanel.getInstance().activate();
            WebViewPanel.getInstance().activate();

            logger.info("----------------------------");

            // research object not working due to xerces
            /*
            XMLChar c;

            System.out.println("--------------------------------------");
			System.out.println("Research Object");
			System.out.println("--------------------------------------");
			// URL url = bc.getBundle().getEntry("/ro/investigation-96-2.ro.zip");
            // bundle://119.0:0/ro/investigation-96-2.ro.zip
            URI fileURI = ResourceExtractor.fileURIforResource("/ro/investigation-96-2.ro.zip");
            System.out.println("uri: " + fileURI);

			Path roPath = Paths.get(fileURI);
            System.out.println("path: " + roPath);
            System.out.println("read bundle");
			ROBundle.readBundle(roPath);
			*/


        } catch (Throwable e) {
            logger.error("Could not start server!", e);
            e.printStackTrace();
        }
    }
}

