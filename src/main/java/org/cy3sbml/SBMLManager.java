package org.cy3sbml;

import java.util.*;

import org.cytoscape.application.CyApplicationManager;
import org.cytoscape.model.CyNetwork;

import org.sbml.jsbml.SBMLDocument;
import org.sbml.jsbml.SBase;

import org.cy3sbml.mapping.IdObjectMap;
import org.cy3sbml.mapping.One2ManyMapping;
import org.cy3sbml.mapping.SBML2NetworkMapper;
import org.cy3sbml.util.NetworkUtil;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The SBMLManager class manages mappings between SBMLDocuments & CyNetworks.
 * 
 * The SBMLManager provides the entry point to interact with SBMLDocuments.
 * All access to SBMLDocuments should go via the SBMLManager.
 * 
 * The SBMLManager is a singleton class.
 *
 * FIXME: The current SUID should be handled here in the manager & not in the mapping.
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

    /**
     * Access to the SBML <-> network mapper.
     * The mapper should not be modified.
     */
	public SBML2NetworkMapper getSBML2NetworkMapper(){
	    return sbml2networks;
	}
	
	/** 
	 * Set all information in SBMLManager from given SBML2NetworkMapper.
	 * This function is used to set the SBML2NetworkMapper from a stored state.
     * For instance during session reloading.
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
	 * Adds an SBMLDocument - network entry to the SBMLManager.
	 * 
	 * For all networks the root network is associated with the SBMLDocument
	 * so that all subnetworks can be looked up via the root network and
	 * the mapping. 
	 */
	public void addSBML2NetworkEntry(SBMLDocument doc, CyNetwork network, One2ManyMapping<String, Long> mapping){	
		Long suid = NetworkUtil.getRootNetworkSUID(network);
		addSBML2NetworkEntry(doc, suid, mapping);
	}

    /**
     * Adds an SBMLDocument - network entry to the SBMLManager.
     */
     public void addSBML2NetworkEntry(SBMLDocument doc, Long rootNetworkSuid, One2ManyMapping<String, Long> mapping){
		// store document and mapping
		sbml2networks.putDocument(rootNetworkSuid, doc, mapping);
		// create and store navigation tree
		IdObjectMap map = new IdObjectMap(doc);
		sbml2objectMap.put(rootNetworkSuid, map);
	}
	
	/** Returns mapping or null if no mapping exists. */
	public One2ManyMapping<String, Long> getMapping(CyNetwork network){
		Long suid = NetworkUtil.getRootNetworkSUID(network);
		return getMapping(suid);
	}
	
	/** Returns mapping or null if no mapping exists. */
	public One2ManyMapping<String, Long> getMapping(Long rootNetworkSUID){
		return sbml2networks.getNSB2CyNodeMapping(rootNetworkSUID);
	}

    /** Update current SBML for network. */
	public void updateCurrent(CyNetwork network) {
		Long suid = NetworkUtil.getRootNetworkSUID(network);
		updateCurrent(suid);
	}
	
	/** Update current SBML via rootNetworkSUID. */
	public void updateCurrent(Long rootNetworkSUID) {
		logger.debug("Set current network to root SUID: " + rootNetworkSUID);

        // FIXME
		sbml2networks.setCurrentSUID(rootNetworkSUID);
		objectMap = sbml2objectMap.get(rootNetworkSUID);
	}

    /**
     * Get current SBMLDocument.
     * Returns null if no current SBMLDocument exists.
     */
    public SBMLDocument getCurrentSBMLDocument(){
        return sbml2networks.getCurrentDocument();
    }

    public One2ManyMapping<Long, String> getCurrentCyNode2NSBMapping(){
        return sbml2networks.getCurrentCyNode2NSBMapping();
    }
    public One2ManyMapping<String, Long> getCurrentNSB2CyNodeMapping(){
        return sbml2networks.getCurrentNSBToCyNodeMapping();
    }

	
	/** Lookup a SBase object via id. */
	public SBase getObjectById(String key){
		SBase sbase = objectMap.getObject(key);
		return sbase;
	}
	
	public List<String> getObjectIds(List<Long> suids){ 
		One2ManyMapping<Long, String> mapping = getCurrentCyNode2NSBMapping();
		return new LinkedList<String>(mapping.getValues(suids));
	}

	/** 
	 * Get SBMLDocument for given network.
	 * Returns null if no SBMLDocument exist for the network.
	 */
	public SBMLDocument getSBMLDocument(CyNetwork network){
		Long suid = NetworkUtil.getRootNetworkSUID(network);
		return getSBMLDocument(suid);
	}

    /**
     * Get SBMLDocument for given rootNetworkSUID.
     * Returns null if no SBMLDocument exist for the network.
     */
	public SBMLDocument getSBMLDocument(Long rootNetworkSUID){
	    return sbml2networks.getDocumentForSUID(rootNetworkSUID);
	}

	/** String information. */
	public String toString(){
	    return sbml2networks.toString();
	}


    ///////////////////////////////////////////////////////////////////////////////////////////////

    /** The network is a network with a mapping to an SBMLDocument. */
    @Deprecated
    public boolean networkIsSBML(CyNetwork network){
        Long suid = NetworkUtil.getRootNetworkSUID(network);
        return sbml2networks.containsSUID(suid);
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
            suids.add(NetworkUtil.getRootNetworkSUID(network));
        }
        for (Long key : sbml2networks.keySet()){
            if (!suids.contains(key)){
                sbml2networks.removeDocument(key);
            }
        }
    }

}
