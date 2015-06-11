package org.cy3sbml;

import java.util.HashSet;

import org.cy3sbml.mapping.NamedSBase2CyNodeMapping;
import org.cy3sbml.mapping.SBML2NetworkMapper;
import org.cytoscape.application.CyApplicationManager;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNetworkManager;
import org.sbml.jsbml.SBMLDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The SBMLManager manages the loaded/available SBML files.
 * 
 * TODO: create a service adaptor class (one stop shop to get all the necessary services)
 */
public class SBMLManager {
	private static final Logger logger = LoggerFactory.getLogger(SBMLManager.class);
	private static SBMLManager uniqueInstance;
	private SBML2NetworkMapper sbmlDocuments;
	private CyNetworkManager cyNetworkManager;
	private CyApplicationManager cyApplicationManager;
	
	public static synchronized SBMLManager getInstance(CyNetworkManager cyNetworkManager, CyApplicationManager cyApplicationManager){
		if (uniqueInstance == null){
			uniqueInstance = new SBMLManager(cyNetworkManager, cyApplicationManager);
		}
		return uniqueInstance;
	}
	
	public static synchronized SBMLManager getInstance(){
		if (uniqueInstance == null){
			logger.error("Access to SBMLManager before creation");
		}
		return uniqueInstance;
	}
	
	private SBMLManager(CyNetworkManager cyNetworkManager, CyApplicationManager cyApplicationManager){
		logger.info("SBMLManager created");
		sbmlDocuments = new SBML2NetworkMapper();
		this.cyNetworkManager = cyNetworkManager;
		this.cyApplicationManager = cyApplicationManager;
	}
	
	public CyNetworkManager getCyNetworkManager(){
		return cyNetworkManager;
	}
	public CyApplicationManager getCyApplicationManager(){
		return cyApplicationManager;
	}
	
	//---------- Manage SBML documents ----------------------
	
	public SBMLDocument getCurrentSBMLDocument(){
		return sbmlDocuments.getCurrentDocument();
	}
	
	public SBMLDocument getSBMLDocumentForCyNetwork(CyNetwork network){
		return sbmlDocuments.getDocumentForSUID(network.getSUID());
	}
	
	public void updateCurrent() {
		CyNetwork network = cyApplicationManager.getCurrentNetwork();
	
		Long suid = null;
		if (network != null){
			suid = network.getSUID();
		}
		sbmlDocuments.setCurrent(suid);
	}
	
	/** Remove all mapping entries were the networks are no longer available. */
	public void synchronizeDocuments(){
		HashSet<Long> suids = new HashSet<Long>();
		for (CyNetwork network : cyNetworkManager.getNetworkSet()){
			suids.add(network.getSUID());
		}
		for (Long key : sbmlDocuments.keySet()){
			if (!suids.contains(key)){
				sbmlDocuments.removeDocument(key);
			}
		}
	}
	

	
	public void addSBML2NetworkEntry(SBMLDocument doc, CyNetwork network, NamedSBase2CyNodeMapping mapping){
		// add the entry
		Long suid = network.getSUID();
		sbmlDocuments.putDocument(suid, doc, mapping);
		sbmlDocuments.setCurrent(suid);
		
		// TODO: update the visualization in control Panel
		// Create some event -> programming pattern
		// this.activate();
		// updateNavigationTree();
	}
	
	


}
