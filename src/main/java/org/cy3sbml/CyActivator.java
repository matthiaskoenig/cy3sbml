package org.cy3sbml;

import org.cy3sbml.actions.*;
import org.cy3sbml.archive.*;
import org.cy3sbml.styles.StyleManager;
import org.cy3sbml.validator.ValidationFrame;
import org.cytoscape.group.CyGroupFactory;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;


import java.awt.*;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.net.URL;
import java.util.HashMap;
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


import org.cy3sbml.cofactors.CofactorManager;
import org.cy3sbml.gui.SBaseHTMLFactory;
import org.cy3sbml.gui.WebViewPanel;
import org.cy3sbml.miriam.RegistryUtil;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;

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

            // Loading extension bundle from resources (netscape.javascript)
            String extensionBundle = "extension/org.cy3javascript.extension-0.0.1.jar";
            logger.info("Install extension bundle");
            Bundle bundle = bc.getBundle();
            URL jarUrl = bundle.getEntry(extensionBundle);
            InputStream input = jarUrl.openStream();
            bc.installBundle(jarUrl.getPath(), input);
            input.close();

            // Extract all resource files for JavaFX (no bundle access) & Miriam
            final ResourceExtractor resourceHandler = new ResourceExtractor(bc, appDirectory);
            resourceHandler.extract();

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
            final String[] styles = {
                    SBML.STYLE_CY3SBML,
                    SBML.STYLE_CY3SBML_DARK,
                    ArchiveReaderTask.ARCHIVE_STYLE};
            LoadVizmapFileTaskFactory loadVizmapFileTaskFactory = getService(bc, LoadVizmapFileTaskFactory.class);
            StyleManager styleManager = StyleManager.getInstance(loadVizmapFileTaskFactory, visualMappingManager, styles);
            styleManager.loadStyles();
            registerService(bc, styleManager, SessionLoadedListener.class, new Properties());

            // SBMLManager
            SBMLManager sbmlManager = SBMLManager.getInstance(cyApplicationManager);
            registerService(bc, sbmlManager, NetworkAboutToBeDestroyedListener.class, new Properties());

            // Cofactor manager
            CofactorManager cofactorManager = CofactorManager.getInstance();

            // panels
            WebViewPanel webViewPanel = WebViewPanel.getInstance(adapter);
            registerService(bc, webViewPanel, CytoPanelComponent.class, new Properties());
            registerService(bc, webViewPanel, RowsSetListener.class, new Properties());
            registerService(bc, webViewPanel, SetCurrentNetworkListener.class, new Properties());
            registerService(bc, webViewPanel, NetworkAddedListener.class, new Properties());
            registerService(bc, webViewPanel, NetworkViewAddedListener.class, new Properties());
            registerService(bc, webViewPanel, NetworkViewAboutToBeDestroyedListener.class, new Properties());

            // GUI frames
            ValidationFrame validationFrame = ValidationFrame.getInstance(adapter);
            registerService(bc, validationFrame, SetCurrentNetworkListener.class, new Properties());
            registerService(bc, validationFrame, NetworkAddedListener.class, new Properties());
            registerService(bc, validationFrame, NetworkViewAddedListener.class, new Properties());
            registerService(bc, validationFrame, NetworkViewAboutToBeDestroyedListener.class, new Properties());


            // init actions [100 - 120]
            ChangeStateAction changeStateAction = new ChangeStateAction();
            registerService(bc, changeStateAction, CyAction.class, new Properties());

            ArchiveAction archiveAction = new ArchiveAction(cySwingApplication, fileUtil,
                    loadNetworkFileTaskFactory, synchronousTaskManager);
            registerService(bc, archiveAction, CyAction.class, new Properties());


            ImportAction importAction = new ImportAction(adapter);
            registerService(bc, importAction, CyAction.class, new Properties());

            SBMLEnableTaskFactory sbmlEnableTaskFactory = new SBMLEnableTaskFactory();
            ValidationAction validationAction = new ValidationAction(new HashMap<>(), adapter, sbmlEnableTaskFactory);
            registerService(bc, validationAction, CyAction.class, new Properties());
            registerService(bc, validationAction, SetCurrentNetworkListener.class, new Properties());

            ExamplesAction examplesAction = new ExamplesAction();
            registerService(bc, examplesAction, CyAction.class, new Properties());

            CofactorAction cofactorAction = new CofactorAction(new HashMap<>(), adapter, sbmlEnableTaskFactory);
            registerService(bc, cofactorAction, CyAction.class, new Properties());
            registerService(bc, cofactorAction, SetCurrentNetworkListener.class, new Properties());

            BiomodelsAction biomodelsAction = new BiomodelsAction(adapter);
            registerService(bc, biomodelsAction, CyAction.class, new Properties());

            HelpAction helpAction = new HelpAction();
            registerService(bc, helpAction, CyAction.class, new Properties());

            SaveLayoutAction saveLayoutAction = new SaveLayoutAction(adapter);
            registerService(bc, saveLayoutAction, CyAction.class, new Properties());

            LoadLayoutAction loadLayoutAction = new LoadLayoutAction(adapter);
            registerService(bc, loadLayoutAction, CyAction.class, new Properties());


            // Archive file reader
            CyLayoutAlgorithmManager layoutAlgorithmManager = getService(bc, CyLayoutAlgorithmManager.class);
            CyNetworkFactory networkFactory = getService(bc, CyNetworkFactory.class);
            CyNetworkViewFactory networkViewFactory = getService(bc, CyNetworkViewFactory.class);

            ArchiveFileFilter archiveFilter = new ArchiveFileFilter(streamUtil);
            ArchiveReaderTaskFactory archiveReaderTaskFactory = new ArchiveReaderTaskFactory(
                    archiveFilter,
                    networkFactory,
                    networkViewFactory,
                    visualMappingManager,
                    layoutAlgorithmManager);
            Properties archiveReaderProps = new Properties();
            archiveReaderProps.setProperty("readerDescription", "Archive file reader (cy3sbml)");
            archiveReaderProps.setProperty("readerId", "archiveNetworkReader");
            registerAllServices(bc, archiveReaderTaskFactory, archiveReaderProps);

            // SBML file reader
            SBMLFileFilter sbmlFilter = new SBMLFileFilter(streamUtil);
            SBMLReaderTaskFactory sbmlReaderTaskFactory = new SBMLReaderTaskFactory(sbmlFilter, adapter);
            Properties sbmlReaderProps = new Properties();
            sbmlReaderProps.setProperty("readerDescription", "SBML file reader (cy3sbml)");
            sbmlReaderProps.setProperty("readerId", "cy3sbmlNetworkReader");
            registerAllServices(bc, sbmlReaderTaskFactory, sbmlReaderProps);

            // Session loading & saving
            SessionData sessionData = new SessionData();
            registerService(bc, sessionData, SessionAboutToBeSavedListener.class, new Properties());
            registerService(bc, sessionData, SessionLoadedListener.class, new Properties());

            // proxy listener
            registerService(bc, connectionProxy, PropertyUpdatedListener.class, new Properties());

            // register services for other apps
            registerService(bc, sbmlManager, SBMLManager.class, new Properties());


            // Update and load registry
            Thread miriamThread = new Thread(new Runnable() {
                public void run() {
                    File miriamFile = new File(appDirectory + File.separator + RegistryUtil.FILENAME_MIRIAM);

                    RegistryUtil.updateMiriamXMLWithNewer(miriamFile);
                    RegistryUtil.loadRegistry(miriamFile);
                }
            });
            miriamThread.run();

            // cy3sbml panels
            webViewPanel.activate();

            logger.info("----------------------------");


        } catch (Throwable e) {
            logger.error("Could not start server!", e);
            e.printStackTrace();
        }
    }
}

