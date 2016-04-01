package org.cy3sbml;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.cy3sbml.gui.ResultsPanel;
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
import org.cytoscape.view.model.events.NetworkViewAboutToBeDestroyedEvent;
import org.cytoscape.view.model.events.NetworkViewAboutToBeDestroyedListener;
import org.sbml.jsbml.NamedSBase;
import org.sbml.jsbml.SBMLDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The SBMLManager class manages mappings between SBMLDocuments & CyNetworks.
 * 
 * The SBMLManager provides the entry point to interact with SBMLDocuments.
 * All access to SBMLDocuments should go via the SBMLManager.
 * 
 * The SBMLManager is a singleton class.
 */
public class SBMLManager implements SetCurrentNetworkListener, NetworkAddedListener, NetworkViewAboutToBeDestroyedListener {
	private static final Logger logger = LoggerFactory.getLogger(SBMLManager.class);
	private static SBMLManager uniqueInstance;
	private ServiceAdapter adapter;
	
	private SBML2NetworkMapper sbml2networks;
	private HashMap<Long, NavigationTree> sbml2trees;
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
		logger.debug("SBMLManager created");
		sbml2networks = new SBML2NetworkMapper();
		sbml2trees = new HashMap<Long, NavigationTree>();
		navigationTree = new NavigationTree();
		this.adapter = adapter;
	}
	

	public SBML2NetworkMapper getSBML2NetworkMapper(){
		return sbml2networks;
	}
	
	/** 
	 * Set all information in SBMLManager from given SBML2NetworkMapper.
	 * Used to restore the SBMLManager state from a session file. 
	 */
	public void setSBML2NetworkMapper(SBML2NetworkMapper mapper){
		logger.debug("SBMLManager from given mapper");
		
		sbml2networks = mapper;
		sbml2trees = new HashMap<Long, NavigationTree>();
		navigationTree = new NavigationTree();
		
		// Create all the trees
		Map<Long, SBMLDocument> documentMap = mapper.getDocumentMap();	
		for (Long suid: documentMap.keySet()){
			SBMLDocument doc = documentMap.get(suid);
			
			// create and store navigation tree
			NavigationTree tree = new NavigationTree(doc);
			sbml2trees.put(suid, tree);
		}
		
		// Set current network and tree
		CyNetwork current = adapter.cyApplicationManager.getCurrentNetwork();
		updateCurrent(current);
	}

	/** 
	 * Get the SUID of the root network.
	 * Returns null if the network is null.
	 */
	public static Long getRootNetworkSuid(CyNetwork network){
		Long suid = null;
		if (network != null){
			CyRootNetwork rootNetwork = ((CySubNetwork)network).getRootNetwork();	
			suid = rootNetwork.getSUID();
		}
		return suid;
	}
	
	/** Get rootNetwork. */
	public static CyNetwork getRootNetwork(CyNetwork network){
		CyNetwork rootNetwork = null;
		if (network != null){
			rootNetwork = ((CySubNetwork)network).getRootNetwork();	
		}
		return rootNetwork;
	}
	
	/*
	 * Adds an SBMLDocument - network entry to the SBMLManager.
	 * 
	 * For all networks the root network is associated with the SBMLDocument
	 * so that all subnetworks can be looked up via the root network and
	 * the mapping. 
	 */
	public void addSBML2NetworkEntry(SBMLDocument doc, CyNetwork network, One2ManyMapping<String, Long> mapping){	
		Long suid = getRootNetworkSuid(network);
		addSBML2NetworkEntry(doc, suid, mapping);
	}
	
	public void addSBML2NetworkEntry(SBMLDocument doc, Long rootNetworkSuid, One2ManyMapping<String, Long> mapping){
		// store document and mapping
		sbml2networks.putDocument(rootNetworkSuid, doc, mapping);
		// create and store navigation tree
		NavigationTree tree = new NavigationTree(doc);
		sbml2trees.put(rootNetworkSuid, tree);
	}
	
	/** Returns mapping or null if no mapping exists. */
	public One2ManyMapping<String, Long> getMapping(CyNetwork network){
		Long suid = getRootNetworkSuid(network);
		return getMapping(suid);
	}
	
	/** Returns mapping or null if no mapping exists. */
	public One2ManyMapping<String, Long> getMapping(Long rootNetworkSUID){
		return sbml2networks.getNSB2CyNodeMapping(rootNetworkSUID);
	}
	
	
	/** The network is a network with a mapping to an SBMLDocument. */
	public boolean networkIsSBML(CyNetwork network){
		Long suid = getRootNetworkSuid(network);
		return sbml2networks.containsSUID(suid);
	}
	
	public void updateCurrent(CyNetwork network) {
		Long suid = getRootNetworkSuid(network);
		updateCurrent(suid);
	}
	
	/** Update the current SBML based the SUID of the root network. */
	public void updateCurrent(Long rootNetworkSUID) {
		logger.debug("Set current network to root SUID: " + rootNetworkSUID);
		sbml2networks.setCurrentSUID(rootNetworkSUID);
		navigationTree = sbml2trees.get(rootNetworkSUID);
	}
	
	/** 
	 * Lookup NamedSBased via id in the NavigationTree.
	 * Key method to get SBML information for nodes in the network.
	 */
	public NamedSBase getNamedSBaseById(String nsbId){
		NamedSBase nsb = navigationTree.getNamedSBaseById(nsbId);
		return nsb;
	}
	
	/** 
	 * Get SBMLDocument for given network.
	 * Returns null if no SBMLDocument exist for the network.
	 */
	public SBMLDocument getSBMLDocument(CyNetwork network){
		Long suid = getRootNetworkSuid(network);
		return getSBMLDocument(suid);
	}
	
	public SBMLDocument getSBMLDocument(Long rootNetworkSUID){
		return sbml2networks.getDocumentForSUID(rootNetworkSUID);
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
	
	public List<String> getNSBIds(List<Long> suids){ 
		One2ManyMapping<Long, String> mapping = getCurrentCyNode2NSBMapping();
		return new LinkedList<String>(mapping.getValues(suids));
	}
	
	public String toString(){
		return sbml2networks.toString();
	}
	
	
	///////////////////////////////////////////////////////////////////
	// This are all events which should be handled by the ResultsPanel
	
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
	 */
	@Override
	public void handleEvent(SetCurrentNetworkEvent event) {
		CyNetwork network = event.getNetwork();
		updateCurrent(network);
		// update selection
		ResultsPanel.getInstance().updateInformation();
	}

	/** If networks are added check if they are subnetworks
	 * of SBML networks and add the respective SBMLDocument 
	 * to them in the mapping.
	 * Due to the mapping based on the RootNetworks sub-networks
	 * automatically can use the mappings of the parent networks.
	 */
	@Override
	public void handleEvent(NetworkAddedEvent event) {
		
	}

	@Override
	public void handleEvent(NetworkViewAboutToBeDestroyedEvent event) {
		ResultsPanel.getInstance().setHelp();
	}
}
