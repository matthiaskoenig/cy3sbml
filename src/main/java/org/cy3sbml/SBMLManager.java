package org.cy3sbml;

import java.util.*;

import org.cytoscape.application.CyApplicationManager;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyTable;
import org.cytoscape.model.subnetwork.CyRootNetwork;
import org.cytoscape.model.subnetwork.CySubNetwork;

import org.sbml.jsbml.SBMLDocument;
import org.sbml.jsbml.SBase;

import org.cy3sbml.mapping.IdObjectMap;
import org.cy3sbml.mapping.One2ManyMapping;
import org.cy3sbml.mapping.SBML2NetworkMapper;

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
public class SBMLManager {
	private static final Logger logger = LoggerFactory.getLogger(SBMLManager.class);
	private static SBMLManager uniqueInstance;
	private CyApplicationManager cyApplicationManager;
	
	private SBML2NetworkMapper sbml2networks;
	private HashMap<Long, IdObjectMap> sbml2objectMap;
	private IdObjectMap objectMap;

    /**
     * Construct the instance.
     * Use the variant without arguments for access.
     */
	public static synchronized SBMLManager getInstance(CyApplicationManager cyApplicationManager){
		if (uniqueInstance == null){
			uniqueInstance = new SBMLManager(cyApplicationManager);
		}
		return uniqueInstance;
	}

    /**
     * Get SBMLManager instance.
     * Use this function to access the SBMLManager.
     */
	public static synchronized SBMLManager getInstance(){
		if (uniqueInstance == null){
			logger.error("Access to SBMLManager before creation");
		}
		return uniqueInstance;
	}

    /** Constructor. */
	private SBMLManager(CyApplicationManager cyApplicationManager){
		logger.debug("SBMLManager created");
		this.cyApplicationManager = cyApplicationManager;
		reset();
	}
	
	/** Reset SBMLManager to empty state. */
	private void reset(){
		sbml2networks = new SBML2NetworkMapper();
		sbml2objectMap = new HashMap<Long, IdObjectMap>();
		objectMap = new IdObjectMap();
	}
	

    /** Access to the SBML <-> network mapper. */
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
		sbml2objectMap = new HashMap<Long, IdObjectMap>();
		objectMap = new IdObjectMap();
		
		// Create all the trees
		Map<Long, SBMLDocument> documentMap = mapper.getDocumentMap();	
		for (Long suid: documentMap.keySet()){
			SBMLDocument doc = documentMap.get(suid);
			
			// create and store navigation tree
			IdObjectMap map = new IdObjectMap(doc);
			sbml2objectMap.put(suid, map);
		}
		
		// Set current network and tree
		CyNetwork current = cyApplicationManager.getCurrentNetwork();
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
	
	/**
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
		IdObjectMap map = new IdObjectMap(doc);
		sbml2objectMap.put(rootNetworkSuid, map);
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
		objectMap = sbml2objectMap.get(rootNetworkSUID);
	}
	
	/**
     * Lookup a SBase object via id.
     */
	public SBase getObjectById(String key){
		SBase sbase = objectMap.getObject(key);
		return sbase;
	}
	
	public List<String> getObjectIds(List<Long> suids){ 
		One2ManyMapping<Long, String> mapping = getCurrentCyNode2NSBMapping();
		return new LinkedList<String>(mapping.getValues(suids));
	}

    /**
     * Get current SBMLDocument.
     * Returns null if no current SBMLDocument exists.
     */
    public SBMLDocument getCurrentSBMLDocument(){
        return sbml2networks.getCurrentDocument();
    }

	/** 
	 * Get SBMLDocument for given network.
	 * Returns null if no SBMLDocument exist for the network.
	 */
	public SBMLDocument getSBMLDocument(CyNetwork network){
		Long suid = getRootNetworkSuid(network);
		return getSBMLDocument(suid);
	}

    /**
     * Get SBMLDocument for given rootNetworkSUID.
     * Returns null if no SBMLDocument exist for the network.
     */
	public SBMLDocument getSBMLDocument(Long rootNetworkSUID){
	    return sbml2networks.getDocumentForSUID(rootNetworkSUID);
	}


	public One2ManyMapping<Long, String> getCurrentCyNode2NSBMapping(){
		return sbml2networks.getCurrentCyNode2NSBMapping();
	}
	public One2ManyMapping<String, Long> getCurrentNSB2CyNodeMapping(){
		return sbml2networks.getCurrentNSBToCyNodeMapping();
	}

	/** String information. */
	public String toString(){
	    return sbml2networks.toString();
	}

    /**
     * Check if network is an SBMLNetwork.
     * This uses a attribute in the network table to check the type of the network.
     */
    public static boolean isSBMLNetwork(CyNetwork cyNetwork) {
        //true if the attribute column exists
        CyTable cyTable = cyNetwork.getDefaultNetworkTable();
        return cyTable.getColumn(SBML.NETWORKTYPE_ATTR) != null;
    }

    /**
     * Remove all mapping entries for networks which are not in the SBML<->network mapping.
     *
     * The current network set can be accessed via
     *      CyNetworkManager.getNetworkSet()
     */
    @Deprecated
    public void synchronizeDocuments(Collection<CyNetwork> networks){
        HashSet<Long> suids = new HashSet<Long>();
        for (CyNetwork network: networks){
            suids.add(getRootNetworkSuid(network));
        }
        for (Long key : sbml2networks.keySet()){
            if (!suids.contains(key)){
                sbml2networks.removeDocument(key);
            }
        }
    }

}
