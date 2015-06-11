package org.cy3sbml;

import java.awt.print.Printable;
import java.util.HashSet;

import org.cy3sbml.mapping.NamedSBase2CyNodeMapping;
import org.cy3sbml.mapping.NavigationTree;
import org.cy3sbml.mapping.One2ManyMapping;
import org.cy3sbml.mapping.SBML2NetworkMapper;
import org.cytoscape.model.CyNetwork;
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
	private ServiceAdapter adapter;
	
	private SBML2NetworkMapper sbml2networks;
	
	public static synchronized SBMLManager getInstance(ServiceAdapter adapter){
		if (uniqueInstance == null){
			uniqueInstance = new SBMLManager(adapter);
		}
		return uniqueInstance;
	}
	
	public static synchronized SBMLManager getInstance(){
		if (uniqueInstance == null){
			logger.error("Access to SBMLManager before creation");
		}
		return uniqueInstance;
	}
	
	private SBMLManager(ServiceAdapter adapter){
		logger.info("SBMLManager created");
		sbml2networks = new SBML2NetworkMapper();
		this.adapter = adapter;
	}
	
	public void addSBML2NetworkEntry(SBMLDocument doc, CyNetwork network, NamedSBase2CyNodeMapping mapping){
		// add the entry
		Long suid = network.getSUID();
		sbml2networks.putDocument(suid, doc, mapping);
	}
	
	public void updateCurrent(CyNetwork network) {	
		logger.info("Update current ...");
		Long suid = null;
		if (network != null){
			suid = network.getSUID();
		}
		sbml2networks.setCurrentSUID(suid);
		
		// TODO: update the visualization in control Panel
		// Create some event -> programming pattern
		// this.activate();
		// TODO:
		// updateNavigationTree();
	}
	
	public SBMLDocument getCurrentSBMLDocument(){
		return sbml2networks.getCurrentDocument();
	}
	public One2ManyMapping<Long, String> getCurrentCyNode2NSBMapping(){
		return sbml2networks.getCurrentCyNode2NSBMapping();
	}
	public One2ManyMapping<String, Long> getCurrentNSB2CyNodeMapping(){
		return sbml2networks.getCurrentNSBToCyNodeMapping();
	}
	
	public SBMLDocument getSBMLDocumentForCyNetwork(CyNetwork network){
		return sbml2networks.getDocumentForSUID(network.getSUID());
	}
	
	
	/** Remove all mapping entries were the networks are no longer available. */
	public void synchronizeDocuments(){
		HashSet<Long> suids = new HashSet<Long>();
		for (CyNetwork network : adapter.cyNetworkManager.getNetworkSet()){
			suids.add(network.getSUID());
		}
		for (Long key : sbml2networks.keySet()){
			if (!suids.contains(key)){
				sbml2networks.removeDocument(key);
			}
		}
	}
	
	public String info(){
		return sbml2networks.toString();
	}

}
