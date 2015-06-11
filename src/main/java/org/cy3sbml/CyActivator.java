package org.cy3sbml;

import org.cytoscape.property.CyProperty;
import org.cytoscape.service.util.AbstractCyActivator;
import org.cytoscape.task.read.LoadVizmapFileTaskFactory;
import org.cytoscape.application.CyApplicationManager;
import org.cytoscape.application.swing.CyAction;
import org.cytoscape.application.swing.CySwingApplication;
import org.cytoscape.application.swing.CytoPanelComponent;
import org.cytoscape.model.CyNetworkFactory;
import org.cytoscape.model.CyNetworkManager;
import org.cytoscape.model.events.RowsSetListener;
import org.cytoscape.view.layout.CyLayoutAlgorithmManager;
import org.cytoscape.view.model.CyNetworkViewFactory;
import org.cytoscape.view.vizmap.VisualMappingManager;
import org.cytoscape.work.SynchronousTaskManager;
import org.cytoscape.work.TaskManager;
import org.cytoscape.io.util.StreamUtil;
import org.cytoscape.io.read.InputStreamTaskFactory;
import org.cytoscape.util.swing.OpenBrowser;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.util.Properties;

import org.cy3sbml.SBMLFileFilter;
import org.cy3sbml.SBMLNetworkViewTaskFactory;
import org.cy3sbml.actions.ControlPanelAction;
import org.cy3sbml.actions.HelpAction;
import org.cy3sbml.gui.ControlPanel;


public class CyActivator extends AbstractCyActivator {
	private static final Logger logger = LoggerFactory.getLogger(CyActivator.class);
	
	public CyActivator() {
		super();
	}
	
	public void start(BundleContext bc) {
		try {
			logger.info("starting server ...");
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
			VisualMappingManager visualMappingManager = getService(bc, VisualMappingManager.class);
			CyLayoutAlgorithmManager cyLayoutAlgorithmManager = getService(bc, CyLayoutAlgorithmManager.class);
			SynchronousTaskManager synchronousTaskManager = getService(bc, SynchronousTaskManager.class);
			TaskManager taskManager = getService(bc, TaskManager.class);
			
			CyNetworkFactory cyNetworkFactory = getService(bc, CyNetworkFactory.class);
			CyNetworkViewFactory cyNetworkViewFactory = getService(bc, CyNetworkViewFactory.class);
			
			CyProperty<Properties> cy3sbmlProperties = getService(bc, CyProperty.class, "(cyPropertyName=cy3sbml.props)");
			StreamUtil streamUtil = getService(bc, StreamUtil.class);
			OpenBrowser openBrowser = getService(bc, OpenBrowser.class);

			/**  
			 * Create ServiceAdapter
			 */
			ServiceAdapter adapter = ServiceAdapter.getInstance(
					cySwingApplication,
					cyApplicationManager,
					cyNetworkManager,
					visualMappingManager,
					cyLayoutAlgorithmManager,
					synchronousTaskManager,
					taskManager,
					
					cyNetworkFactory,
					cyNetworkViewFactory,
					
					cy3sbmlProperties,
					streamUtil,
					openBrowser
			);
			
			/**
			 * Create things depending on services (with adapter)
			 */ 
			SBMLFileFilter sbmlFilter = new SBMLFileFilter("SBML files (*.xml)", streamUtil);
			SBMLNetworkViewTaskFactory sbmlNetworkViewTaskFactory = new SBMLNetworkViewTaskFactory(sbmlFilter, adapter);
			
			// Load visual styles
			// TOOD: handle multiple loading of same style (probably better in separate task)
			LoadVizmapFileTaskFactory loadVizmapFileTaskFactory =  getService(bc, LoadVizmapFileTaskFactory.class);
			InputStream stream = getClass().getResourceAsStream("/styles/cy3sbml.xml");
			loadVizmapFileTaskFactory.loadStyles(stream);
			
			SBMLManager.getInstance(cyNetworkManager, cyApplicationManager);
			ControlPanel navControlPanel = ControlPanel.getInstance(openBrowser);
			
			// actions
			ControlPanelAction controlPanelAction = new ControlPanelAction(cySwingApplication);
			HelpAction helpAction = new HelpAction(cySwingApplication, openBrowser);
			
			/**
			 * Register services 
			 */
			// SBML file reader
			Properties sbmlNetworkViewTaskFactoryProps = new Properties();
			sbmlNetworkViewTaskFactoryProps.setProperty("readerDescription","SBML (Cy3SBML) file reader");
			sbmlNetworkViewTaskFactoryProps.setProperty("readerId","cy3sbmlNetworkViewReader");
			registerService(bc,sbmlNetworkViewTaskFactory,InputStreamTaskFactory.class, sbmlNetworkViewTaskFactoryProps);
			
			
			registerService(bc, navControlPanel, CytoPanelComponent.class, new Properties());
			// actions
			registerService(bc, controlPanelAction, CyAction.class, new Properties());
			registerService(bc, helpAction, CyAction.class, new Properties());
			
			// listeners
			registerService(bc, navControlPanel, RowsSetListener.class, new Properties());
			
			// set visible
			// ? 
			
			// TODO: ChangeStateAction
			
			// TODO: ImportAction
			// ImportAction importAction = new ImportAction(cySwingApplication);
			// registerService(bc, importAction, CyAction.class, new Properties());
			
			// TODO: BiomodelAction
			// TODO: ValidationAction
			// TODO: SaveLayoutAction
			// TODO: LoadLayoutAction
			
			
			// Show the cy3sbml panel
			controlPanelAction.actionPerformed(null);
			logger.info("server started");
		
		} catch (Throwable e){
			logger.error("Could not start server!", e);
			e.printStackTrace();
		}
	}
}

