package org.cy3sbml;

import java.util.HashSet;

import org.cy3sbml.mapping.NamedSBase2CyNodeMapping;
import org.cy3sbml.mapping.NavigationTree;
import org.cy3sbml.mapping.One2ManyMapping;
import org.cy3sbml.mapping.SBML2NetworkMapper;
import org.cytoscape.application.events.SetCurrentNetworkEvent;
import org.cytoscape.application.events.SetCurrentNetworkListener;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.events.NetworkAddedEvent;
import org.cytoscape.model.events.NetworkAddedListener;
import org.cytoscape.model.subnetwork.CyRootNetwork;
import org.cytoscape.model.subnetwork.CySubNetwork;
import org.sbml.jsbml.NamedSBase;
import org.sbml.jsbml.SBMLDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The SBMLManager manages the loaded/available SBML files.
 * The sbml2networks manages the mapping between SBMLDocuments and
 * CyNetworks.
 * Interaction with the SBMLDocuments and information should go through the 
 * SBMLManager.
 */
public class SBMLManager implements SetCurrentNetworkListener, NetworkAddedListener {
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
		// stores the root network with the SBMLDocument
		// all subnetworks can be looked up via the root network
		CyRootNetwork rootNetwork = ((CySubNetwork)network).getRootNetwork();	
		Long suid = rootNetwork.getSUID();
		sbml2networks.putDocument(suid, doc, mapping);
	}
	

	public void updateCurrent(CyNetwork network) {	
		logger.info("Update current ...");
		Long suid = null;
		if (network != null){
			CyRootNetwork rootNetwork = ((CySubNetwork)network).getRootNetwork();	
			suid = rootNetwork.getSUID();
		}
		sbml2networks.setCurrentSUID(suid);
		
		// update tree
		updateNavigationTree();
	}
	
	// TODO: only create the trees once for the SBMLDocuments -> lookup afterwards
	private void updateNavigationTree(){
		SBMLDocument document = sbml2networks.getCurrentDocument();
		navigationTree = new NavigationTree(document);
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
		CyRootNetwork rootNetwork = ((CySubNetwork)network).getRootNetwork();	
		return sbml2networks.getDocumentForSUID(rootNetwork.getSUID());
	}
	
	/** Remove all mapping entries were the networks are no longer available. */
	public void synchronizeDocuments(){
		HashSet<Long> suids = new HashSet<Long>();
		for (CyNetwork network : adapter.cyNetworkManager.getNetworkSet()){
			CyRootNetwork rootNetwork = ((CySubNetwork)network).getRootNetwork();
			suids.add(rootNetwork.getSUID());
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
		logger.info("network SUID: " + network.getSUID());
		CyRootNetwork rootNetwork = ((CySubNetwork)network).getRootNetwork();
		logger.info("root SUID: " + rootNetwork.getSUID());
		
		updateCurrent(network);
	}

	/** If networks are added check if they are subnetworks
	 * of SBML networks and add the respective SBMLDocument 
	 * to them in the mapping.
	 */
	@Override
	public void handleEvent(NetworkAddedEvent event) {
		
		logger.info("NetworkAddedEvent");
		CyNetwork network = event.getNetwork();
		logger.info("network SUID: " + network.getSUID());
		CyRootNetwork rootNetwork = ((CySubNetwork)network).getRootNetwork();
		logger.info("root SUID: " + rootNetwork.getSUID());
		// TODO: manage
		
		// if the root network is an SBMLNetwork than add
		// the network to root network mapping
		// not necessary because root network is already in there
		
		
	}
	
}
