package org.cy3sbml;

import org.cytoscape.property.CyProperty;
import org.cytoscape.property.PropertyUpdatedListener;
import org.cytoscape.service.util.AbstractCyActivator;
import org.cytoscape.task.read.LoadVizmapFileTaskFactory;
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
import org.cytoscape.view.model.events.NetworkViewAboutToBeDestroyedEvent;
import org.cytoscape.view.model.events.NetworkViewAboutToBeDestroyedListener;
import org.cytoscape.view.vizmap.VisualMappingManager;
import org.cytoscape.work.SynchronousTaskManager;
import org.cytoscape.work.TaskManager;
import org.cytoscape.io.util.StreamUtil;
import org.cytoscape.util.swing.OpenBrowser;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.util.Properties;

import org.cy3sbml.gui.ResultsPanel;
import org.cy3sbml.SBMLFileFilter;
import org.cy3sbml.actions.BioModelAction;
import org.cy3sbml.actions.ChangeStateAction;
import org.cy3sbml.actions.HelpAction;


public class CyActivator extends AbstractCyActivator {
	private static final Logger logger = LoggerFactory.getLogger(CyActivator.class);
	
	public CyActivator() {
		super();
	}
	
	public void start(BundleContext bc) {
		try {
			logger.info("starting server ...");
			// store bundle information
			BundleInformation.getInstance(bc);
			
			// cy3sbml properties
			PropsReader propsReader = new PropsReader("cy3sbml", "cy3sbml.props");
			Properties propsReaderServiceProps = new Properties();
			propsReaderServiceProps.setProperty("cyPropertyName", "cy3sbml.props");
			registerAllServices(bc, propsReader, propsReaderServiceProps);
			
			/**
			 * Get services 
			 */
			CySwingApplication cySwingApplication = getService(bc, CySwingApplication.class);
			
			CyApplicationManager cyApplicationManager = getService(bc, CyApplicationManager.class);
			CyNetworkManager cyNetworkManager = getService(bc, CyNetworkManager.class);
			CyNetworkViewManager cyNetworkViewManager = getService(bc, CyNetworkViewManager.class);
			VisualMappingManager visualMappingManager = getService(bc, VisualMappingManager.class);
			CyLayoutAlgorithmManager cyLayoutAlgorithmManager = getService(bc, CyLayoutAlgorithmManager.class);
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
			
			// Use the Cytoscape properties to set proxy for webservices
			ConnectionProxy connectionProxy = new ConnectionProxy(cyProperties);
			connectionProxy.setSystemProxyFromCyProperties();
						
			/**  
			 * Create ServiceAdapter
			 */
			ServiceAdapter adapter = ServiceAdapter.getInstance(
					cySwingApplication,
					cyApplicationManager,
					cyNetworkManager,
					cyNetworkViewManager,
					visualMappingManager,
					cyLayoutAlgorithmManager,
					synchronousTaskManager,
					taskManager,
					
					cyNetworkFactory,
					cyNetworkViewFactory,
					
					cy3sbmlProperties,
					streamUtil,
					openBrowser,
					connectionProxy
			);
			
			/**
			 * Create things depending on services (with adapter)
			 */ 
			// load cy3sbml styles
			LoadVizmapFileTaskFactory loadVizmapFileTaskFactory =  getService(bc, LoadVizmapFileTaskFactory.class);
			InputStream stream = getClass().getResourceAsStream("/styles/cy3sbml.xml");
			loadVizmapFileTaskFactory.loadStyles(stream);
			
			// init SBML manager
			SBMLManager sbmlManager = SBMLManager.getInstance(adapter);
			// init cy3sbml ControlPanel
			ResultsPanel resultsPanel = ResultsPanel.getInstance(adapter);
			// init actions
			// ResultsPanelAction resultsPanelAction = new ResultsPanelAction(cySwingApplication);
			HelpAction helpAction = new HelpAction(cySwingApplication, openBrowser);
			ChangeStateAction changeStateAction = new ChangeStateAction(cySwingApplication);
			BioModelAction bioModelAction = new BioModelAction(adapter);
			
			SBMLFileFilter sbmlFilter = new SBMLFileFilter("SBML files (*.xml)", streamUtil);
			// SBMLNetworkViewTaskFactory sbmlNetworkViewTaskFactory = new SBMLNetworkViewTaskFactory(sbmlFilter, adapter);

			
			/**
			 * Register services 
			 */			
			// SBML file reader
			SBMLReader sbmlReader = new SBMLReader(sbmlFilter, adapter);
			Properties sbmlReaderProps = new Properties();
			sbmlReaderProps.setProperty("readerDescription","SBML file reader (cy3sbml)");
			sbmlReaderProps.setProperty("readerId","cy3sbmlNetworkReader");
			registerAllServices(bc, sbmlReader, sbmlReaderProps);
			
			
			registerService(bc, resultsPanel, CytoPanelComponent.class, new Properties());
			// actions
			// registerService(bc, resultsPanelAction, CyAction.class, new Properties());
			registerService(bc, helpAction, CyAction.class, new Properties());
			registerService(bc, changeStateAction, CyAction.class, new Properties());
			registerService(bc, bioModelAction, CyAction.class, new Properties());
			
			// listeners
			registerService(bc, resultsPanel, RowsSetListener.class, new Properties());
			registerService(bc, connectionProxy, PropertyUpdatedListener.class, new Properties());
			registerService(bc, sbmlManager, SetCurrentNetworkListener.class, new Properties());
			registerService(bc, sbmlManager, NetworkAddedListener.class, new Properties());
			registerService(bc, sbmlManager, NetworkAddedListener.class, new Properties());
			registerService(bc, sbmlManager, NetworkViewAboutToBeDestroyedListener.class, new Properties());
			

			// Network added / handle selection of networks and network views
			// registerService(bc, navControlPanel, NetworkDestroyedEvent.class, new Properties());
			// registerService(bc, navControlPanel, NetworkViewAddedEvent.class, new Properties());
			
			// TODO: ImportAction
			// ImportAction importAction = new ImportAction(cySwingApplication);
			// registerService(bc, importAction, CyAction.class, new Properties());
			// TODO: ValidationAction
			// TODO: SaveLayoutAction
			// TODO: LoadLayoutAction
			
			// Show the cy3sbml panel
			ResultsPanel.getInstance().activate();
			logger.info("server started");
		} catch (Throwable e){
			logger.error("Could not start server!", e);
			e.printStackTrace();
		}
	}
}

