package org.cy3sbml;

import java.util.*;

import org.cy3sbml.mapping.CyIdSBaseMap;
import org.cytoscape.application.CyApplicationManager;
import org.cytoscape.model.CyNetwork;

import org.cytoscape.model.events.NetworkAboutToBeDestroyedEvent;
import org.cytoscape.model.events.NetworkAboutToBeDestroyedListener;
import org.cytoscape.model.subnetwork.CyRootNetwork;
import org.cytoscape.model.subnetwork.CySubNetwork;
import org.sbml.jsbml.SBMLDocument;
import org.sbml.jsbml.SBase;

import org.cy3sbml.mapping.Network2SBMLMapper;
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
public class SBMLManager implements NetworkAboutToBeDestroyedListener {
	private static final Logger logger = LoggerFactory.getLogger(SBMLManager.class);
	private static SBMLManager uniqueInstance;
	private CyApplicationManager cyApplicationManager;

    private Long currentSUID;
    private Network2SBMLMapper network2sbml;
	private HashMap<Long, CyIdSBaseMap> network2objectMap;

    /**
     * Get SBMLManager (creates the instance).
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
		network2sbml = new Network2SBMLMapper();
		network2objectMap = new HashMap<>();
	}

    /**
     * Access to the SBML <-> network mapper.
     * The mapper should not be modified.
     */
	public Network2SBMLMapper getNetwork2SBMLMapper(){
	    return network2sbml;
	}

	/**
	 * Adds an SBMLDocument - network entry to the SBMLManager.
	 * 
	 * For all networks the root network is associated with the SBMLDocument
	 * so that all subnetworks can be looked up via the root network and
	 * the mapping. 
	 */
	public void addSBMLForNetwork(SBMLDocument doc, CyNetwork network, One2ManyMapping<String, Long> mapping){
		addSBMLForNetwork(doc, NetworkUtil.getRootNetworkSUID(network), mapping);
	}

    /** Adds an SBMLDocument - network entry to the SBMLManager. */
     public void addSBMLForNetwork(SBMLDocument doc, Long rootNetworkSUID, One2ManyMapping<String, Long> mapping){
		// document & mapping
		network2sbml.putDocument(rootNetworkSUID, doc, mapping);
		// object map
		network2objectMap.put(rootNetworkSUID, new CyIdSBaseMap(doc));
	}

    /**
     * Remove the SBMLDocument for network.
     * The SBMLDocument is only removed if no other subnetworks reference the SBMLDocument.
     */
    public Boolean removeSBMLForNetwork(CyNetwork network){

        // necessary to check if there are other SubNetworks for the root network.
        // If yes the SBMLDocument is not removed

        Long rootSUID = NetworkUtil.getRootNetworkSUID(network);
        CyRootNetwork rootNetwork = ((CySubNetwork) network).getRootNetwork();
        List<CySubNetwork> subnetworks = rootNetwork.getSubNetworkList();
        if (subnetworks.size() == 1){
            network2sbml.removeDocument(rootSUID);
            logger.info(String.format("SBMLDocument removed for rootSUID: %s", rootSUID));
            return true;
        }else {
            logger.info(String.format("SBMLDocument not removed for rootSUID: %s. Number of associated networks: %s",
                    rootSUID, subnetworks.size()));
            return false;
        }
    }

	/** Returns mapping or null if no mapping exists. */
	public One2ManyMapping<String, Long> getMapping(CyNetwork network){
		Long suid = NetworkUtil.getRootNetworkSUID(network);
		return getMapping(suid);
	}
	
	/** Returns mapping or null if no mapping exists. */
	public One2ManyMapping<String, Long> getMapping(Long rootNetworkSUID){
		return network2sbml.getSBase2CyNodeMapping(rootNetworkSUID);
	}

    /** Update current SBML for network. */
	public void updateCurrent(CyNetwork network) {
		Long suid = NetworkUtil.getRootNetworkSUID(network);
		updateCurrent(suid);
	}
	
	/** Update current SBML via rootNetworkSUID. */
	public void updateCurrent(Long rootNetworkSUID) {
		logger.debug("Set current network to root SUID: " + rootNetworkSUID);
		setCurrentSUID(rootNetworkSUID);
	}

	/** Set the current network SUID. */
    private void setCurrentSUID(Long SUID){
        currentSUID = null;
        if (SUID != null && network2sbml.containsDocument(SUID)){
            currentSUID = SUID;
        }
        logger.debug("Current network set to: " + currentSUID);
    }

    /** Get current network SUID. */
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
     * Get SBMLDocument.
     * @param rootNetworkSUID root network SUID
     * @return SBMLDocument or null
     */
    public SBMLDocument getSBMLDocument(Long rootNetworkSUID){
        return network2sbml.getDocument(rootNetworkSUID);
    }


    public One2ManyMapping<Long, String> getCurrentCyNode2SBaseMapping(){
        return network2sbml.getCyNode2SBaseMapping(currentSUID);
    }

    public One2ManyMapping<String, Long> getCurrentSBase2CyNodeMapping(){
        return network2sbml.getSBase2CyNodeMapping(currentSUID);
    }

	/**
     * Lookup a SBase object via id.
     *
     * The SBases are stored so that their information can be used for display
     * in the results panel. The lookup gets the dictionary for the current network
     * and searches for the key.
     *
     * The object maps are created when the SBMLDocument is stored.
     */
	public SBase getSBaseByCyId(String key){
        return getSBaseByCyId(key, currentSUID);
	}

    public SBase getSBaseByCyId(String key, Long SUID){
        return network2objectMap.get(SUID).getObjectByCyId(key);
    }

    /**
     * Lookup the list of cyIds of SBase objects for the given suids.
     *
     * @param suids list of node suids.
     * @return
     */
	public List<String> getCyIdsFromSUIDs(List<Long> suids){
		One2ManyMapping<Long, String> mapping = getCurrentCyNode2SBaseMapping();
		return new LinkedList<>(mapping.getValues(suids));
	}

	/** String information. */
	public String toString(){
	    return network2sbml.toString();
	}


    ///////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * Set all information in SBMLManager from given Network2SBMLMapper.
     * This function is used to set the Network2SBMLMapper from a stored state.
     * For instance during session reloading.
     */
    public void setSBML2NetworkMapper(Network2SBMLMapper mapper){
        logger.debug("SBMLManager from given mapper");

        network2sbml = mapper;
        network2objectMap = new HashMap<>();

        // Create all the trees
        Map<Long, SBMLDocument> documentMap = mapper.getDocumentMap();
        for (Long suid: documentMap.keySet()){
            SBMLDocument doc = documentMap.get(suid);

            // create id<->object mapping
            CyIdSBaseMap map = new CyIdSBaseMap(doc);
            network2objectMap.put(suid, map);
        }

        // Set current network and tree
        CyNetwork currentNetwork = cyApplicationManager.getCurrentNetwork();
        updateCurrent(currentNetwork);
    }

    ///////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * Remove the mappings if networks are destroyed.
     * This handles also the new Session (all networks are destroyed).
     */
    @Override
    public void handleEvent(NetworkAboutToBeDestroyedEvent e) {
        CyNetwork network = e.getNetwork();
        removeSBMLForNetwork(network);
    }


}
