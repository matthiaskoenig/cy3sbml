package org.cy3sbml.archive;

import org.apache.taverna.robundle.Bundle;
import org.cy3sbml.util.NetworkUtil;
import org.cytoscape.application.CyApplicationManager;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.events.NetworkAboutToBeDestroyedEvent;
import org.cytoscape.model.events.NetworkAboutToBeDestroyedListener;
import org.cytoscape.model.subnetwork.CyRootNetwork;
import org.cytoscape.model.subnetwork.CySubNetwork;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;


/**
 * Manages the mapping between robundles & CyNetworks.
 */
public class BundleManager implements NetworkAboutToBeDestroyedListener {
    private static final Logger logger = LoggerFactory.getLogger(BundleManager.class);

    private static BundleManager uniqueInstance;
    private CyApplicationManager cyApplicationManager;

    private Long currentSUID;
    private Network2BundleMapper network2bundle;
    private HashMap<Bundle, BundleAnnotation> bundle2annotation;

    /**
     * Get instance.
     */
    public static synchronized BundleManager getInstance(CyApplicationManager cyApplicationManager) {
        if (uniqueInstance == null) {
            uniqueInstance = new BundleManager(cyApplicationManager);
        }
        return uniqueInstance;
    }

    /**
     * Get instance.
     */
    public static synchronized BundleManager getInstance() {
        if (uniqueInstance == null) {
            logger.error("Access to BundleManager before creation");
        }
        return uniqueInstance;
    }

    /**
     * Constructor.
     */
    private BundleManager(CyApplicationManager cyApplicationManager) {
        logger.debug("BundleManager created");
        this.cyApplicationManager = cyApplicationManager;
        reset();
    }

    /**
     * Reset to empty state.
     */
    private void reset() {
        currentSUID = null;
        network2bundle = new Network2BundleMapper();
        bundle2annotation = new HashMap<>();
    }

    /**
     * Access bundle to network mapping.
     */
    public Network2BundleMapper getNetwork2BundleMapper() {
        return network2bundle;
    }

    /**
     * Adds a bundle to the manager.
     */
    public void addBundleForNetwork(Bundle bundle, CyNetwork network) {
        addBundleForNetwork(bundle, NetworkUtil.getRootNetworkSUID(network));
    }

    /**
     * Adds a bundle - network entry.
     */
    public void addBundleForNetwork(Bundle bundle, Long rootNetworkSUID) {
        network2bundle.put(rootNetworkSUID, bundle);
        BundleAnnotation annotation = new BundleAnnotation(bundle);
        bundle2annotation.put(bundle, annotation);
    }

    /**
     * Remove bundle for network.
     * The bundle is only removed if no other subnetworks references it.
     */
    public Boolean removeBundleForNetwork(CyNetwork network) {

        // necessary to check if there are other SubNetworks for the root network.
        // If yes the SBMLDocument is not removed

        Long rootSUID = NetworkUtil.getRootNetworkSUID(network);
        CyRootNetwork rootNetwork = ((CySubNetwork) network).getRootNetwork();
        List<CySubNetwork> subnetworks = rootNetwork.getSubNetworkList();
        if (subnetworks.size() == 1) {
            network2bundle.remove(rootSUID);
            logger.info(String.format("Bundle removed for rootSUID: %s", rootSUID));
            return true;
        } else {
            logger.info(String.format("Bundle not removed for rootSUID: %s. Number of associated networks: %s",
                    rootSUID, subnetworks.size()));
            return false;
        }
    }

    /**
     * Update current.
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
        if (SUID != null && network2bundle.contains(SUID)) {
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
     * Get current bundle.
     * Returns null if no current bundle exists.
     */
    public Bundle getCurrentBundle() {
        return getBundle(currentSUID);
    }

    public BundleAnnotation getCurrentBundleAnnotation() {
        Bundle bundle = getBundle(currentSUID);
        if (bundle == null){
            return null;
        }
        return bundle2annotation.get(bundle);
    }


    /**
     * Get bundle for given network.
     * Returns null if no bundle exist for the network.
     */
    public Bundle getBundle(CyNetwork network) {
        Long suid = NetworkUtil.getRootNetworkSUID(network);
        return getBundle(suid);
    }

    /**
     * Get bundle.
     *
     * @param rootNetworkSUID root network SUID
     * @return bundle or null
     */
    public Bundle getBundle(Long rootNetworkSUID) {
        return network2bundle.get(rootNetworkSUID);
    }


    /**
     * String information.
     */
    public String toString() {
        return network2bundle.toString();
    }


    /**
     * Remove bundles if networks are destroyed.
     * This handles also the new Session (all networks are destroyed).
     */
    @Override
    public void handleEvent(NetworkAboutToBeDestroyedEvent e) {
        CyNetwork network = e.getNetwork();
        removeBundleForNetwork(network);
    }

}
