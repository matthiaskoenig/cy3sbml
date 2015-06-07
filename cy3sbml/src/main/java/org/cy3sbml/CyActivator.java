package org.cy3sbml;


import org.cytoscape.application.CyApplicationManager;
import org.cytoscape.application.swing.CyAction;
import org.cytoscape.application.swing.CySwingApplication;
import org.cytoscape.application.swing.CytoPanelComponent;
import org.cytoscape.model.CyNetworkFactory;
import org.cytoscape.model.CyNetworkManager;
import org.cytoscape.util.swing.OpenBrowser;
import org.cytoscape.view.model.CyNetworkViewFactory;
import org.cytoscape.io.util.StreamUtil;
import org.cytoscape.io.read.InputStreamTaskFactory;
import org.cytoscape.service.util.AbstractCyActivator;
import org.osgi.framework.BundleContext;

import java.util.Properties;

import org.cy3sbml.SBMLFileFilter;
import org.cy3sbml.SBMLNetworkViewTaskFactory;
import org.cy3sbml.actions.ControlPanelAction;
import org.cy3sbml.actions.ImportAction;
import org.cy3sbml.gui.SBMLControlPanel;


// TODO: create unit tests
// TODO: create logging for cy3sbml


public class CyActivator extends AbstractCyActivator {
	public CyActivator() {
		super();
	}

	public void printInfo(){
		System.out.println("Start *** cy3sbml ***");	
	}
	
	public void start(BundleContext bc) {
		try {
		printInfo();
		// register the file reader
		CyNetworkFactory cyNetworkFactory = getService(bc, CyNetworkFactory.class);
		CyNetworkViewFactory cyNetworkViewFactory = getService(bc, CyNetworkViewFactory.class);
		StreamUtil streamUtilRef = getService(bc,StreamUtil.class);
		
		SBMLFileFilter sbmlFilter = new SBMLFileFilter("SBML files (*.xml)",streamUtilRef);
		SBMLNetworkViewTaskFactory sbmlNetworkViewTaskFactory = new SBMLNetworkViewTaskFactory(sbmlFilter, cyNetworkFactory, cyNetworkViewFactory);
		
		Properties sbmlNetworkViewTaskFactoryProps = new Properties();
		sbmlNetworkViewTaskFactoryProps.setProperty("readerDescription","SBML (Cy3SBML) file reader");
		sbmlNetworkViewTaskFactoryProps.setProperty("readerId","cy3sbmlNetworkViewReader");
		registerService(bc,sbmlNetworkViewTaskFactory,InputStreamTaskFactory.class, sbmlNetworkViewTaskFactoryProps);
		
		
		// register the Control Panel
		CySwingApplication cySwingApplication = getService(bc, CySwingApplication.class);
		
		SBMLControlPanel navControlPanel = SBMLControlPanel.getInstance();
		ControlPanelAction controlPanelAction = new ControlPanelAction(cySwingApplication, navControlPanel);
		
		registerService(bc, navControlPanel, CytoPanelComponent.class, new Properties());
		registerService(bc, controlPanelAction, CyAction.class, new Properties());

		CyNetworkManager cyNetworkManager = getService(bc, CyNetworkManager.class);
		CyApplicationManager cyApplicationManager = getService(bc, CyApplicationManager.class);
		
		
		// create the SBMLManager
		SBMLManager.getInstance(cyNetworkManager, cyApplicationManager);
		
		// browser support
		// TODO: send to the NavPanel to listen to the links
		OpenBrowser openBrowser = getService(bc, OpenBrowser.class);
		
		/* cy3sbml actions */
		// ImportAction
		ImportAction importAction = new ImportAction(cySwingApplication);
		registerService(bc, importAction, CyAction.class, new Properties());
		
		// TODO: BiomodelAction
		// TODO: ValidationAction
		// TODO: ChangeStateAction
		// TODO: HelpAction
		// TODO: SaveLayoutAction
		// TODO: LoadLayoutAction
		
		
		} catch (Exception e){
			e.printStackTrace();
		}
	}
}

