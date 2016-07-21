package org.cy3sbml;

import java.util.*;

import org.cy3sbml.mapping.Network2SBMLMapper;
import org.cytoscape.application.CyApplicationManager;
import org.cytoscape.model.CyNetwork;

import org.sbml.jsbml.SBMLDocument;
import org.sbml.jsbml.SBase;

import org.cy3sbml.mapping.IdObjectMap;
import org.cy3sbml.mapping.One2ManyMapping;
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
 */
public class SBMLManager {
	private static final Logger logger = LoggerFactory.getLogger(SBMLManager.class);
	private static SBMLManager uniqueInstance;
	private CyApplicationManager cyApplicationManager;

    private Long currentSUID;
    private IdObjectMap objectMap;
    private Network2SBMLMapper sbml2networks;
	private HashMap<Long, IdObjectMap> sbml2objectMap;

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
        currentSUID = null;
		sbml2networks = new Network2SBMLMapper();
		sbml2objectMap = new HashMap<Long, IdObjectMap>();
		objectMap = new IdObjectMap();
	}

    /**
     * Access to the SBML <-> network mapper.
     * The mapper should not be modified.
     */
	public Network2SBMLMapper getSBML2NetworkMapper(){
	    return sbml2networks;
	}

	/**
	 * Adds an SBMLDocument - network entry to the SBMLManager.
	 * 
	 * For all networks the root network is associated with the SBMLDocument
	 * so that all subnetworks can be looked up via the root network and
	 * the mapping. 
	 */
	public void addSBMLForNetwork(SBMLDocument doc, CyNetwork network, One2ManyMapping<String, Long> mapping){
		Long suid = NetworkUtil.getRootNetworkSUID(network);
		addSBMLForNetwork(doc, suid, mapping);
	}

    /**
     * Adds an SBMLDocument - network entry to the SBMLManager.
     */
     private void addSBMLForNetwork(SBMLDocument doc, Long rootNetworkSUID, One2ManyMapping<String, Long> mapping){
		// document & mapping
		sbml2networks.putDocument(rootNetworkSUID, doc, mapping);
		// object map
		sbml2objectMap.put(rootNetworkSUID, new IdObjectMap(doc));
	}


	/** Returns mapping or null if no mapping exists. */
	public One2ManyMapping<String, Long> getMapping(CyNetwork network){
		Long suid = NetworkUtil.getRootNetworkSUID(network);
		return getMapping(suid);
	}
	
	/** Returns mapping or null if no mapping exists. */
	private One2ManyMapping<String, Long> getMapping(Long rootNetworkSUID){
		return sbml2networks.getNSB2CyNodeMapping(rootNetworkSUID);
	}

	////------------------------------------------------

    /** Update current SBML for network. */
	public void updateCurrent(CyNetwork network) {
		Long suid = NetworkUtil.getRootNetworkSUID(network);
		updateCurrent(suid);
	}
	
	/** Update current SBML via rootNetworkSUID. */
	private void updateCurrent(Long rootNetworkSUID) {
		logger.debug("Set current network to root SUID: " + rootNetworkSUID);

		setCurrentSUID(rootNetworkSUID);
		objectMap = sbml2objectMap.get(rootNetworkSUID);
	}

    private void setCurrentSUID(Long SUID){
        currentSUID = null;
        if (SUID != null && sbml2networks.containsNetwork(SUID)){
            currentSUID = SUID;
        }
        logger.debug("Current network set to: " + currentSUID);
    }

    public Long getCurrentSUID(){
        return currentSUID;
    }

    /**
     * Get current SBMLDocument.
     * Returns null if no current SBMLDocument exists.
     */
    public SBMLDocument getCurrentSBMLDocument(){
        return getSBMLDocument(currentSUID);
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
    private SBMLDocument getSBMLDocument(Long rootNetworkSUID){
        return sbml2networks.getDocumentForSUID(rootNetworkSUID);
    }


    public One2ManyMapping<Long, String> getCurrentCyNode2NSBMapping(){
        return sbml2networks.getCyNode2NSBMapping(currentSUID);
    }

    public One2ManyMapping<String, Long> getCurrentNSB2CyNodeMapping(){
        return sbml2networks.getNSB2CyNodeMapping(currentSUID);
    }

	
	/** Lookup a SBase object via id. */
	public SBase getObjectById(String key){
		SBase sbase = objectMap.getObject(key);
		return sbase;
	}

    /**
     * FIXME: document, what is this function doing?
     */
	public List<String> getObjectIds(List<Long> suids){ 
		One2ManyMapping<Long, String> mapping = getCurrentCyNode2NSBMapping();
		return new LinkedList<String>(mapping.getValues(suids));
	}

	/** String information. */
	public String toString(){
	    return sbml2networks.toString();
	}

    ///////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * Set all information in SBMLManager from given Network2SBMLMapper.
     * This function is used to set the Network2SBMLMapper from a stored state.
     * For instance during session reloading.
     */
    public void setSBML2NetworkMapper(Network2SBMLMapper mapper){
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
        CyNetwork currentNetwork = cyApplicationManager.getCurrentNetwork();
        updateCurrent(currentNetwork);
    }

    ///////////////////////////////////////////////////////////////////////////////////////////////

    /** The network is a network with a mapping to an SBMLDocument. */
    @Deprecated
    public boolean networkIsSBML(CyNetwork network){
        Long suid = NetworkUtil.getRootNetworkSUID(network);
        return sbml2networks.containsNetwork(suid);
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
