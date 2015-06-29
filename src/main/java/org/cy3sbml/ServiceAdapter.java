package org.cy3sbml;

import java.io.File;
import java.util.Properties;

import org.cytoscape.application.CyApplicationManager;
import org.cytoscape.application.swing.CySwingApplication;
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

/** Adapter for working with services.
 * Avoids to have to pass around the services to everywhere, but provides
 * a one-stop shop for getting the necessary things.
 *
 */
public class ServiceAdapter {
	private static final Logger logger = LoggerFactory.getLogger(ServiceAdapter.class);
	
	private static ServiceAdapter uniqueInstance;
		
	public CySwingApplication cySwingApplication;
	public CyApplicationManager cyApplicationManager;
	public CyNetworkManager cyNetworkManager;
	public CyNetworkViewManager cyNetworkViewManager;
	public VisualMappingManager visualMappingManager;
	public CyLayoutAlgorithmManager cyLayoutAlgorithmManager;
	public DialogTaskManager dialogTaskManager;
	@SuppressWarnings("rawtypes")
	public SynchronousTaskManager synchronousTaskManager;
	@SuppressWarnings("rawtypes")
	public TaskManager taskManager;
	public CyNetworkFactory cyNetworkFactory;
	public CyNetworkViewFactory cyNetworkViewFactory;
	public CyProperty<Properties> cy3sbmlProperties;
	public File cy3sbmlDirectory;
	public StreamUtil streamUtil;
	public OpenBrowser openBrowser;
	public ConnectionProxy connectionProxy;
	public LoadNetworkFileTaskFactory loadNetworkFileTaskFactory;
	public FileUtil fileUtil;
	
	@SuppressWarnings("rawtypes")
	public static synchronized ServiceAdapter getInstance(
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
			CyNetworkViewFactory cyNetworkViewFactory,
			CyProperty<Properties> cy3sbmlProperties,
			File cy3sbmlDirectory,
			StreamUtil streamUtil,
			OpenBrowser openBrowser,
			ConnectionProxy connectionProxy,
			LoadNetworkFileTaskFactory loadNetworkFileTaskFactory,
			FileUtil fileUtil
			){
		if (uniqueInstance == null){
			uniqueInstance = new ServiceAdapter(
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
					cy3sbmlDirectory,
					streamUtil,
					openBrowser,
					connectionProxy,
					loadNetworkFileTaskFactory,
					fileUtil);
		}
		return uniqueInstance;
	}
	
	public static ServiceAdapter getInstance(){
		return uniqueInstance;
	}
	
	@SuppressWarnings("rawtypes")
	private ServiceAdapter(
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
			CyNetworkViewFactory cyNetworkViewFactory,
			CyProperty<Properties> cy3sbmlProperties,
			File cy3sbmlDirectory,
			StreamUtil streamUtil,
			OpenBrowser openBrowser,
			ConnectionProxy connectionProxy,
			LoadNetworkFileTaskFactory loadNetworkFileTaskFactory,
			FileUtil fileUtil
			){
		logger.info("ServiceAdapter created");
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
		this.cyNetworkViewFactory = cyNetworkViewFactory;
		this.cy3sbmlProperties = cy3sbmlProperties;
		this.cy3sbmlDirectory = cy3sbmlDirectory;
		this.streamUtil = streamUtil;
		this.openBrowser = openBrowser;
		this.connectionProxy = connectionProxy;
		this.loadNetworkFileTaskFactory = loadNetworkFileTaskFactory;
		this.fileUtil = fileUtil;
	}

	public Object cy3sbmlProperty(String s){
		return cy3sbmlProperties.getProperties().get(s);
	}
}
