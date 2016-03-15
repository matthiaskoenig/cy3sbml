package org.cy3sbml;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;

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
 * The SBMLManager is a singleton class.
 * 
 * TODO: implement proper to string information
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
		logger.info("SBMLManager created");
		sbml2networks = new SBML2NetworkMapper();
		sbml2trees = new HashMap<Long, NavigationTree>();
		navigationTree = new NavigationTree();
		this.adapter = adapter;
	}
	
	public void addSBML2NetworkEntry(SBMLDocument doc, CyNetwork network, One2ManyMapping<String, Long> mapping){
		// stores the root network with the SBMLDocument
		// all subnetworks can be looked up via the root network
		CyRootNetwork rootNetwork = ((CySubNetwork)network).getRootNetwork();	
		Long suid = rootNetwork.getSUID();
		sbml2networks.putDocument(suid, doc, mapping);
		// create and store navigation tree
		NavigationTree tree = new NavigationTree(doc);
		sbml2trees.put(suid, tree);
	}
	
	/** Required for storing the session state. */
	public SBML2NetworkMapper getSBML2NetworkMapper(){
		return sbml2networks;
	}
	
	/** Returns mapping or null if no mapping exists. */
	public One2ManyMapping<String, Long> getMapping(CyNetwork network){
		CyRootNetwork rootNetwork = ((CySubNetwork)network).getRootNetwork();	
		Long suid = rootNetwork.getSUID();
		return sbml2networks.getNSB2CyNodeMapping(suid);
	}
	
	public boolean networkIsSBML(CyNetwork network){
		CyRootNetwork rootNetwork = ((CySubNetwork)network).getRootNetwork();	
		Long suid = rootNetwork.getSUID();
		return sbml2networks.containsSUID(suid);
	}
	
	public void updateCurrent(CyNetwork network) {	
		logger.debug("Update current ...");
		Long suid = null;
		if (network != null){
			CyRootNetwork rootNetwork = ((CySubNetwork)network).getRootNetwork();	
			suid = rootNetwork.getSUID();
		}
		sbml2networks.setCurrentSUID(suid);
		navigationTree = sbml2trees.get(suid);
	}
	
	
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
	
	public List<String> getNSBIds(List<Long> suids){ 
		One2ManyMapping<Long, String> mapping = getCurrentCyNode2NSBMapping();
		return new LinkedList<String>(mapping.getValues(suids));
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
		CyNetwork network = event.getNetwork();
		CyRootNetwork rootNetwork = ((CySubNetwork)network).getRootNetwork();
		logger.info("SetCurrentNetworkEvent to network/root SUID: "+ network.getSUID() + "/" + rootNetwork.getSUID());
		
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
		logger.info("NetworkViewAboutToBeDestroyedEvent");
		ResultsPanel.getInstance().getTextPane().setHelp();
	}
}
