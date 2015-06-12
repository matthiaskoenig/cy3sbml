package org.cy3sbml;

import java.util.HashSet;

import org.cy3sbml.mapping.NamedSBase2CyNodeMapping;
import org.cy3sbml.mapping.NavigationTree;
import org.cy3sbml.mapping.One2ManyMapping;
import org.cy3sbml.mapping.SBML2NetworkMapper;
import org.cytoscape.application.events.SetCurrentNetworkEvent;
import org.cytoscape.application.events.SetCurrentNetworkListener;
import org.cytoscape.model.CyNetwork;
import org.sbml.jsbml.NamedSBase;
import org.sbml.jsbml.SBMLDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The SBMLManager manages the loaded/available SBML files.
 * 
 * TODO: create a service adaptor class (one stop shop to get all the necessary services)
 */
public class SBMLManager implements SetCurrentNetworkListener {
	private static final Logger logger = LoggerFactory.getLogger(SBMLManager.class);
	private static SBMLManager uniqueInstance;
	private ServiceAdapter adapter;
	
	private SBML2NetworkMapper sbml2networks;
	private NavigationTree navigationTree;
	
	
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
		navigationTree = new NavigationTree();
		this.adapter = adapter;
	}
	
	public void addSBML2NetworkEntry(SBMLDocument doc, CyNetwork network, NamedSBase2CyNodeMapping mapping){
		// add the entry
		Long suid = network.getSUID();
		sbml2networks.putDocument(suid, doc, mapping);
	}
	
	// TODO: !!! This has to be done when current network changes
	public void updateCurrent(CyNetwork network) {	
		logger.info("Update current ...");
		Long suid = null;
		if (network != null){
			suid = network.getSUID();
		}
		sbml2networks.setCurrentSUID(suid);
		
		// update tree
		updateNavigationTree();
	}
	
	// TODO: only create the trees once for the SBMLDocuments -> lookup afterwards
	private void updateNavigationTree(){
		SBMLDocument doc = sbml2networks.getCurrentDocument();
		navigationTree = new NavigationTree(doc);
	}
	
	
	// TODO: handle navigation tree better
	public NamedSBase getNamedSBaseById(String nsbId){
		NamedSBase nsb = navigationTree.getNamedSBaseById(nsbId);
		return nsb;
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
	
	/**
	 * Listening to changes in Networks and NetworkViews.
	 * When must the SBMLDocument store be updated.
	 * - NetworkViewAddedEvent
	 * - NetworkViewDestroyedEvent
	 * 
	 * An event indicating that a network view has been set to current.
	 *  SetCurrentNetworkViewEvent
	 * 
	 * An event signaling that the a network has been set to current.
	 *  SetCurrentNetworkEvent
	 * 
	 * @param e
	 */
	@Override
	public void handleEvent(SetCurrentNetworkEvent event) {
		logger.info("SetCurrentNetworkEvent");
		CyNetwork network = event.getNetwork();
		updateCurrent(network);
	}
	
	/** If networks are added check if they are subnetworks
	 * of SBML networks and add the respective SBMLDocument 
	 * to them in the mapping.
	 */
	
	

}
