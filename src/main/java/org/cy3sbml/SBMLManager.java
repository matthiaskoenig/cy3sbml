package org.cy3sbml;

import java.util.ArrayList;
import java.util.List;
import org.cy3sbml.mapping.Network2SBMLMapper;
import org.cy3sbml.mapping.One2ManyMapping;
import org.cy3sbml.util.NetworkUtil;
import org.cytoscape.application.CyApplicationManager;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.events.NetworkAboutToBeDestroyedEvent;
import org.cytoscape.model.events.NetworkAboutToBeDestroyedListener;
import org.cytoscape.model.subnetwork.CyRootNetwork;
import org.cytoscape.model.subnetwork.CySubNetwork;
import org.sbml.jsbml.SBMLDocument;
import org.sbml.jsbml.SBase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The SBMLManager class manages mappings between SBMLDocuments & CyNetworks.
 * <p>
 * The SBMLManager provides the entry point to interact with SBMLDocuments.
 * All access to SBMLDocuments should go via the SBMLManager.
 * <p>
 * CyActivator creates the single instance and registers it as an OSGi service,
 * so that other apps can look it up.
 */
public class SBMLManager implements NetworkAboutToBeDestroyedListener {
    private static final Logger logger = LoggerFactory.getLogger(SBMLManager.class);
    private final CyApplicationManager cyApplicationManager;

    /*
     * currentSUID and network2sbml are written from Cytoscape event handlers
     * (network/selection listeners, session restore) and read from the WebViewPanel's
     * background panel-update thread (PanelUpdater, run on its own Thread).
     *
     * currentSUID is only ever replaced wholesale (never mutated in place), so volatile
     * is enough to make a writer's new value visible to the reader thread.
     *
     * network2sbml is also replaced wholesale on session restore, so it needs the same
     * volatile reference; but addSBMLForNetwork/removeSBMLForNetwork mutate the SAME
     * Network2SBMLMapper instance in place while a reader may be querying it concurrently.
     * That mutation safety is provided by Network2SBMLMapper itself (its methods are
     * synchronized, and the One2ManyMapping instances it hands out are synchronized too),
     * not by this field's volatile modifier. volatile here only guarantees visibility of a
     * full mapper replacement, not safety of in-place mutation of the mapper's contents.
     */
    private volatile Long currentSUID;
    private volatile Network2SBMLMapper network2sbml;

    /**
     * Constructor.
     */
    public SBMLManager(CyApplicationManager cyApplicationManager) {
        logger.debug("SBMLManager created");
        this.cyApplicationManager = cyApplicationManager;
        reset();
    }

    /**
     * Reset SBMLManager to empty state.
     */
    private void reset() {
        currentSUID = null;
        network2sbml = new Network2SBMLMapper();
    }

    /**
     * Access to the SBML <-> network mapper.
     * The mapper should not be modified.
     */
    public Network2SBMLMapper getNetwork2SBMLMapper() {
        return network2sbml;
    }

    /**
     * Adds an SBMLDocument - network entry to the SBMLManager.
     * <p>
     * For all networks the root network is associated with the SBMLDocument
     * so that all subnetworks can be looked up via the root network and
     * the mapping.
     */
    public void addSBMLForNetwork(SBMLDocument doc, CyNetwork network, One2ManyMapping<String, Long> mapping) {
        addSBMLForNetwork(doc, NetworkUtil.getRootNetworkSUID(network), mapping);
    }

    /**
     * Adds an SBMLDocument - network entry to the SBMLManager.
     */
    public void addSBMLForNetwork(SBMLDocument doc, Long rootNetworkSUID, One2ManyMapping<String, Long> mapping) {
        // document & mapping
        network2sbml.putDocument(rootNetworkSUID, doc, mapping);
    }

    /**
     * Remove the SBMLDocument for network.
     * The SBMLDocument is only removed if no other subnetworks reference the SBMLDocument.
     */
    public Boolean removeSBMLForNetwork(CyNetwork network) {

        // necessary to check if there are other SubNetworks for the root network.
        // If yes the SBMLDocument is not removed

        Long rootSUID = NetworkUtil.getRootNetworkSUID(network);
        CyRootNetwork rootNetwork = ((CySubNetwork) network).getRootNetwork();
        List<CySubNetwork> subnetworks = rootNetwork.getSubNetworkList();
        if (subnetworks.size() == 1) {
            network2sbml.removeDocument(rootSUID);
            logger.info(String.format("SBMLDocument removed for rootSUID: %s", rootSUID));
            return true;
        } else {
            logger.info(String.format(
                    "SBMLDocument not removed for rootSUID: %s. Number of associated networks: %s",
                    rootSUID, subnetworks.size()));
            return false;
        }
    }

    /**
     * Returns mapping or null if no mapping exists.
     */
    public One2ManyMapping<String, Long> getMapping(CyNetwork network) {
        Long suid = NetworkUtil.getRootNetworkSUID(network);
        return getMapping(suid);
    }

    /**
     * Returns mapping or null if no mapping exists.
     */
    public One2ManyMapping<String, Long> getMapping(Long rootNetworkSUID) {
        return network2sbml.getSBase2CyNodeMapping(rootNetworkSUID);
    }

    /**
     * Update current SBML for network.
     */
    public void updateCurrent(CyNetwork network) {
        Long suid = NetworkUtil.getRootNetworkSUID(network);
        updateCurrent(suid);
    }

    /**
     * Update current SBML via rootNetworkSUID.
     */
    public void updateCurrent(Long rootNetworkSUID) {
        logger.debug("Set current network to root SUID: " + rootNetworkSUID);
        setCurrentSUID(rootNetworkSUID);
    }

    /**
     * Set the current network SUID.
     */
    private void setCurrentSUID(Long SUID) {
        currentSUID = null;
        if (SUID != null && network2sbml.containsDocument(SUID)) {
            currentSUID = SUID;
        }
        logger.debug("Current network set to: " + currentSUID);
    }

    /**
     * Get current network SUID.
     */
    public Long getCurrentSUID() {
        return currentSUID;
    }

    /**
     * Get current SBMLDocument.
     * Returns null if no current SBMLDocument exists.
     */
    public SBMLDocument getCurrentSBMLDocument() {
        return getSBMLDocument(currentSUID);
    }

    /**
     * Get SBMLDocument for given network.
     * Returns null if no SBMLDocument exist for the network.
     */
    public SBMLDocument getSBMLDocument(CyNetwork network) {
        Long suid = NetworkUtil.getRootNetworkSUID(network);
        return getSBMLDocument(suid);
    }

    /**
     * Get SBMLDocument.
     *
     * @param rootNetworkSUID root network SUID
     * @return SBMLDocument or null
     */
    public SBMLDocument getSBMLDocument(Long rootNetworkSUID) {
        return network2sbml.getDocument(rootNetworkSUID);
    }

    public One2ManyMapping<Long, String> getCurrentCyNode2SBaseMapping() {
        return network2sbml.getCyNode2SBaseMapping(currentSUID);
    }

    public One2ManyMapping<String, Long> getCurrentSBase2CyNodeMapping() {
        return network2sbml.getSBase2CyNodeMapping(currentSUID);
    }

    /**
     * Lookup a SBase object via id.
     * <p>
     * The SBases are stored so that their information can be used for display
     * in the results panel. The lookup gets the dictionary for the current network
     * and searches for the key.
     * <p>
     * The object maps are created when the SBMLDocument is stored.
     */
    public SBase getSBaseByCyId(String cyId) {
        return getSBaseByCyId(cyId, currentSUID);
    }

    public SBase getSBaseByCyId(String cyId, Long SUID) {
        SBMLDocument doc = network2sbml.getDocument(SUID);
        return doc.getElementByMetaId(cyId);
    }

    /**
     * Lookup the list of cyIds of SBase objects for the given suids.
     *
     * @param suids list of node suids.
     */
    public List<String> getCyIdsFromSUIDs(List<Long> suids) {
        One2ManyMapping<Long, String> mapping = getCurrentCyNode2SBaseMapping();
        return new ArrayList<>(mapping.getValues(suids));
    }

    /**
     * String information.
     */
    @Override
    public String toString() {
        return network2sbml.toString();
    }

    ///////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * Set all information in SBMLManager from given Network2SBMLMapper.
     * This function is used to set the Network2SBMLMapper from a stored state.
     * For instance during session reloading.
     */
    public void setSBML2NetworkMapper(Network2SBMLMapper mapper) {
        logger.debug("SBMLManager from given mapper");

        network2sbml = mapper;

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
