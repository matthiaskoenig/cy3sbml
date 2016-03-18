package org.cy3sbml;

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
import org.cytoscape.view.vizmap.VisualMappingManager;
import org.cytoscape.work.SynchronousTaskManager;
import org.cytoscape.work.TaskManager;
import org.cytoscape.work.swing.DialogTaskManager;
import org.cytoscape.io.util.StreamUtil;
import org.cytoscape.util.swing.FileUtil;
import org.cytoscape.util.swing.OpenBrowser;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.InputStream;
import java.util.Properties;

import org.cy3sbml.gui.ResultsPanel;
import org.cy3sbml.SBMLFileFilter;
import org.cy3sbml.actions.BioModelAction;
import org.cy3sbml.actions.ChangeStateAction;
import org.cy3sbml.actions.CofactorNodesAction;
import org.cy3sbml.actions.ExamplesAction;
import org.cy3sbml.actions.HelpAction;
import org.cy3sbml.actions.ImportAction;
import org.cy3sbml.actions.LoadLayoutAction;
import org.cy3sbml.actions.SaveLayoutAction;
import org.cy3sbml.actions.ValidationAction;

/**
 * Entry point to cy3sbml.
 * 
 * The CyActivator registers the cy3sbml services with OSGI. This is the class
 * used for startup of the app by Cytoscape 3.
 * 
 */
public class CyActivator extends AbstractCyActivator {
	private static Logger logger;
	private static final String PROPERTIES_FILE = "cy3sbml.props";
	
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
			
			if(appDirectory.exists() == false) {
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
			CyProperty<Properties> cy3sbmlProperties = getService(bc, CyProperty.class, "(cyPropertyName=cy3sbml.props)");
			StreamUtil streamUtil = getService(bc, StreamUtil.class);
			OpenBrowser openBrowser = getService(bc, OpenBrowser.class);
			FileUtil fileUtil = getService(bc, FileUtil.class);
			
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
					cyNetworkViewFactory,
					
					cy3sbmlProperties,
					appDirectory,
					streamUtil,
					openBrowser,
					connectionProxy,
					loadNetworkFileTaskFactory,
					fileUtil
			);
			
			// visual styles
			LoadVizmapFileTaskFactory loadVizmapFileTaskFactory =  getService(bc, LoadVizmapFileTaskFactory.class);
			InputStream stream = getClass().getResourceAsStream("/styles/cy3sbml.xml");
			loadVizmapFileTaskFactory.loadStyles(stream);
			
			// init SBML manager
			SBMLManager sbmlManager = SBMLManager.getInstance(adapter);
			// init cy3sbml ControlPanel
			ResultsPanel resultsPanel = ResultsPanel.getInstance(adapter);
			// init actions
			ChangeStateAction changeStateAction = new ChangeStateAction(cySwingApplication);
			ImportAction importAction = new ImportAction(adapter);
			BioModelAction bioModelAction = new BioModelAction(adapter);
			ValidationAction validationAction = new ValidationAction(adapter);
			ExamplesAction examplesAction = new ExamplesAction(cySwingApplication);
			HelpAction helpAction = new HelpAction(cySwingApplication, openBrowser);
			CofactorNodesAction cofactorNodesAction = new CofactorNodesAction(adapter);
			SaveLayoutAction saveLayoutAction = new SaveLayoutAction(adapter);
			LoadLayoutAction loadLayoutAction = new LoadLayoutAction(adapter);
			
			// SBML Filter
			SBMLFileFilter sbmlFilter = new SBMLFileFilter("SBML files (*.xml)", streamUtil);
			
			/** Register services */			
			// SBML file reader
			SBMLReader sbmlReader = new SBMLReader(sbmlFilter, adapter);
			Properties sbmlReaderProps = new Properties();
			sbmlReaderProps.setProperty("readerDescription","SBML file reader (cy3sbml)");
			sbmlReaderProps.setProperty("readerId","cy3sbmlNetworkReader");
			registerAllServices(bc, sbmlReader, sbmlReaderProps);
			
			// Session loading & saving
			SessionData sessionData = new SessionData(appDirectory);
			registerService(bc, sessionData, SessionAboutToBeSavedListener.class, new Properties());
			registerService(bc, sessionData, SessionLoadedListener.class, new Properties());
			
			// panels
			registerService(bc, resultsPanel, CytoPanelComponent.class, new Properties());
			// actions
			registerService(bc, helpAction, CyAction.class, new Properties());
			registerService(bc, changeStateAction, CyAction.class, new Properties());
			registerService(bc, bioModelAction, CyAction.class, new Properties());
			registerService(bc, validationAction, CyAction.class, new Properties());
			registerService(bc, importAction, CyAction.class, new Properties());
			registerService(bc, examplesAction, CyAction.class, new Properties());
			registerService(bc, cofactorNodesAction, CyAction.class, new Properties());
			registerService(bc, saveLayoutAction, CyAction.class, new Properties());
			registerService(bc, loadLayoutAction, CyAction.class, new Properties());
			
			// listeners
			registerService(bc, resultsPanel, RowsSetListener.class, new Properties());
			registerService(bc, connectionProxy, PropertyUpdatedListener.class, new Properties());
			registerService(bc, sbmlManager, SetCurrentNetworkListener.class, new Properties());
			registerService(bc, sbmlManager, NetworkAddedListener.class, new Properties());
			registerService(bc, sbmlManager, NetworkAddedListener.class, new Properties());
			registerService(bc, sbmlManager, NetworkViewAboutToBeDestroyedListener.class, new Properties());
			
			// register cy3sbml services for other plugins
			registerService(bc, sbmlManager, SBMLManager.class, new Properties());
			
			// show cy3sbml panel
			ResultsPanel.getInstance().activate();
			logger.info("----------------------------");
			
		} catch (Throwable e){
			logger.error("Could not start server!", e);
			e.printStackTrace();
		}
	}
}

