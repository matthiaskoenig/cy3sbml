package org.cy3sbml;

import org.cytoscape.service.util.AbstractCyActivator;
import org.cytoscape.task.read.LoadVizmapFileTaskFactory;
import org.cytoscape.task.visualize.ApplyPreferredLayoutTaskFactory;
import org.cytoscape.task.visualize.ApplyVisualStyleTaskFactory;
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
import org.cytoscape.view.vizmap.VisualStyleFactory;
import org.cytoscape.work.TaskManager;
import org.cytoscape.io.util.StreamUtil;
import org.cytoscape.io.read.InputStreamTaskFactory;
import org.cytoscape.util.swing.OpenBrowser;
import org.osgi.framework.BundleContext;

import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.util.Properties;

import javax.swing.ImageIcon;

import org.cy3sbml.SBMLFileFilter;
import org.cy3sbml.SBMLNetworkViewTaskFactory;
import org.cy3sbml.actions.ControlPanelAction;
import org.cy3sbml.actions.HelpAction;
import org.cy3sbml.actions.ImportAction;
import org.cy3sbml.gui.SBMLControlPanel;


public class CyActivator extends AbstractCyActivator {
	public CyActivator() {
		super();
	}
	
	public void start(BundleContext bc) {
		try {
			System.out.println("cy3sbml: start init");
			
			// taskmanager for executing tasks
			
			
			// register SBML file reader
			ApplyVisualStyleTaskFactory applyVisualStyle = getService(bc, ApplyVisualStyleTaskFactory.class);
			CyNetworkFactory cyNetworkFactory = getService(bc, CyNetworkFactory.class);
			CyNetworkViewFactory cyNetworkViewFactory = getService(bc, CyNetworkViewFactory.class);
			StreamUtil streamUtilRef = getService(bc, StreamUtil.class);
			TaskManager taskManager = getService(bc, TaskManager.class);
			VisualMappingManager visualMappingManager = getService(bc, VisualMappingManager.class);
			CyLayoutAlgorithmManager cyLayoutAlgorithmManager = getService(bc, CyLayoutAlgorithmManager.class);
			
			SBMLFileFilter sbmlFilter = new SBMLFileFilter("SBML files (*.xml)",streamUtilRef);
			ApplyPreferredLayoutTaskFactory applyPreferredLayout = getService(bc, ApplyPreferredLayoutTaskFactory.class);
			
			SBMLNetworkViewTaskFactory sbmlNetworkViewTaskFactory = new SBMLNetworkViewTaskFactory(sbmlFilter, cyNetworkFactory, cyNetworkViewFactory,
																						visualMappingManager, cyLayoutAlgorithmManager, taskManager);
			
			Properties sbmlNetworkViewTaskFactoryProps = new Properties();
			sbmlNetworkViewTaskFactoryProps.setProperty("readerDescription","SBML (Cy3SBML) file reader");
			sbmlNetworkViewTaskFactoryProps.setProperty("readerId","cy3sbmlNetworkViewReader");
			registerService(bc,sbmlNetworkViewTaskFactory,InputStreamTaskFactory.class, sbmlNetworkViewTaskFactoryProps);
			
			// browser support
			OpenBrowser openBrowser = getService(bc, OpenBrowser.class);
			
			// register cy3sbml Control Panel
			CySwingApplication cySwingApplication = getService(bc, CySwingApplication.class);
			

			
			
			// TODO: handle the creation of the navigation panel
			// Send browser reference
			SBMLControlPanel navControlPanel = SBMLControlPanel.getInstance(openBrowser);
			ControlPanelAction controlPanelAction = new ControlPanelAction(cySwingApplication);
			
			registerService(bc, navControlPanel, CytoPanelComponent.class, new Properties());
			registerService(bc, controlPanelAction, CyAction.class, new Properties());
	
			CyNetworkManager cyNetworkManager = getService(bc, CyNetworkManager.class);
			CyApplicationManager cyApplicationManager = getService(bc, CyApplicationManager.class);
			
			// set visible
			// ? 
			
			// init the cy3sbml SBMLManager
			// handling the mapping between networks and sbml files
			SBMLManager.getInstance(cyNetworkManager, cyApplicationManager);
			
			/* cy3sbml actions */
			// Register all the necessary actions
			// ImportAction
			ImportAction importAction = new ImportAction(cySwingApplication);
			registerService(bc, importAction, CyAction.class, new Properties());
			
			HelpAction helpAction = new HelpAction(cySwingApplication, openBrowser);
			registerService(bc, helpAction, CyAction.class, new Properties());
			
			// TODO: BiomodelAction
			// TODO: ValidationAction
			// TODO: ChangeStateAction
			// TODO: HelpAction
			// TODO: SaveLayoutAction
			// TODO: LoadLayoutAction
			
			// Row selection listener
			// Handling the node selections
			registerService(bc, navControlPanel, RowsSetListener.class, new Properties());
			
			// Load the visual styles
			// TODO: Set name for task
			// TOOD: handle multiple loading of same style (probably better in separate task)
			LoadVizmapFileTaskFactory loadVizmapFileTaskFactory =  getService(bc, LoadVizmapFileTaskFactory.class);
			InputStream stream = getClass().getResourceAsStream("/styles/cy3sbml.xml");
			loadVizmapFileTaskFactory.loadStyles(stream);
			
			// Show the cy3sbml panel
			controlPanelAction.actionPerformed(null);
		
		} catch (Exception e){
			e.printStackTrace();
		}
		System.out.println("cy3sbml: end init");
	}
}

