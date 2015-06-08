package org.cy3sbml;

import org.cytoscape.application.CyApplicationManager;
import org.cytoscape.model.CyNetworkManager;

/**
 * Main class for managing the SBML files.
 * Replaces the parts in the NavPanel.
 * 
 * Master class for managing the SBML documents
 * @author mkoenig
 *
 */
public class SBMLManager {
	private static SBMLManager uniqueInstance;
	private SBMLDocuments sbmlDocuments;
	private CyNetworkManager cyNetworkManager;
	private CyApplicationManager cyApplicationManager;
	

	public static synchronized SBMLManager getInstance(CyNetworkManager cyNetworkManager, CyApplicationManager cyApplicationManager){
		if (uniqueInstance == null){
			System.out.println("cy3sbml: SBMLManager created");
			uniqueInstance = new SBMLManager(cyNetworkManager, cyApplicationManager);
		}
		return uniqueInstance;
	}
	
	private SBMLManager(CyNetworkManager cyNetworkManager, CyApplicationManager cyApplicationManager){
		/** Construct the Navigation panel for cy3sbml. */
		sbmlDocuments = new SBMLDocuments();
		this.cyNetworkManager = cyNetworkManager;
		this.cyApplicationManager = cyApplicationManager;
	}
	
	public CyNetworkManager getCyNetworkManager(){
		return cyNetworkManager;
	}
	public CyApplicationManager getCyApplicationManager(){
		return cyApplicationManager;
	}
	
	
}
