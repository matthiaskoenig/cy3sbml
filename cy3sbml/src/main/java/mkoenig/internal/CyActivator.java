package mkoenig.internal;

import org.cytoscape.model.CyNetworkFactory;
import org.cytoscape.model.CyNetworkManager;
import org.cytoscape.view.model.CyNetworkViewManager;
import org.cytoscape.view.model.CyNetworkViewFactory;
import org.cytoscape.io.util.StreamUtil;
import org.cytoscape.io.read.InputStreamTaskFactory;
import org.cytoscape.service.util.AbstractCyActivator;
import org.cytoscape.work.TaskFactory;
import org.cytoscape.session.CyNetworkNaming;
import org.osgi.framework.BundleContext;

import java.util.Properties;

import mkoenig.internal.SBMLFileFilter;
import mkoenig.internal.SBMLNetworkViewTaskFactory;


public class CyActivator extends AbstractCyActivator {
	public CyActivator() {
		super();
	}

	public void printInfo(){
		System.out.println("********************************");
		System.out.println("Cy3SBML - Reader.run()");
		System.out.println("********************************");	
	}
	
	public void start(BundleContext bc) {
		printInfo();
		// register the file reader
		CyNetworkFactory cyNetworkFactoryServiceRef = getService(bc,CyNetworkFactory.class);
		CyNetworkViewFactory cyNetworkViewFactoryServiceRef = getService(bc,CyNetworkViewFactory.class);
		StreamUtil streamUtilRef = getService(bc,StreamUtil.class);
		
		SBMLFileFilter sbmlFilter = new SBMLFileFilter("SBML files (*.xml)",streamUtilRef);
		SBMLNetworkViewTaskFactory sbmlNetworkViewTaskFactory = new SBMLNetworkViewTaskFactory(sbmlFilter,cyNetworkFactoryServiceRef,cyNetworkViewFactoryServiceRef);
		
		Properties sbmlNetworkViewTaskFactoryProps = new Properties();
		sbmlNetworkViewTaskFactoryProps.setProperty("readerDescription","SBML (Cy3SBML) file reader");
		sbmlNetworkViewTaskFactoryProps.setProperty("readerId","cy3sbmlNetworkViewReader");
		registerService(bc,sbmlNetworkViewTaskFactory,InputStreamTaskFactory.class, sbmlNetworkViewTaskFactoryProps);
		
		
		// make the menu item
		CyNetworkNaming cyNetworkNamingServiceRef = getService(bc,CyNetworkNaming.class);
		CyNetworkManager cyNetworkManagerServiceRef = getService(bc,CyNetworkManager.class);
		CyNetworkViewManager cyNetworkViewManagerServiceRef = getService(bc,CyNetworkViewManager.class);
		
		CreateNetworkViewTaskFactory createNetworkViewTaskFactory = new CreateNetworkViewTaskFactory(cyNetworkNamingServiceRef, cyNetworkFactoryServiceRef,cyNetworkManagerServiceRef, cyNetworkViewFactoryServiceRef,cyNetworkViewManagerServiceRef);
				
		Properties createNetworkViewTaskFactoryProps = new Properties();
		createNetworkViewTaskFactoryProps.setProperty("preferredMenu","Apps.Samples");
		createNetworkViewTaskFactoryProps.setProperty("title","Test CySBML3 new");
		registerService(bc,createNetworkViewTaskFactory,TaskFactory.class, createNetworkViewTaskFactoryProps);
	}
}

