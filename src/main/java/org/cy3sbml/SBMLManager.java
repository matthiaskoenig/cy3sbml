package org.cy3sbml;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import org.cy3sbml.archive.ArchiveImport;
import org.cy3sbml.comp.CompModels;
import org.cy3sbml.comp.CompTargets;
import org.cy3sbml.comp.SBaseRefResolver;
import org.cy3sbml.mapping.Network2SBMLMapper;
import org.cy3sbml.mapping.One2ManyMapping;
import org.cy3sbml.util.AttributeUtil;
import org.cy3sbml.util.NetworkUtil;
import org.cytoscape.application.CyApplicationManager;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNode;
import org.cytoscape.model.events.NetworkAboutToBeDestroyedEvent;
import org.cytoscape.model.events.NetworkAboutToBeDestroyedListener;
import org.cytoscape.model.subnetwork.CyRootNetwork;
import org.cytoscape.model.subnetwork.CySubNetwork;
import org.sbml.jsbml.Model;
import org.sbml.jsbml.SBMLDocument;
import org.sbml.jsbml.SBase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The SBMLManager class manages mappings between SBMLDocuments and CyNetworks.
 * <p>
 * The SBMLManager provides the entry point to interact with SBMLDocuments.
 * All access to SBMLDocuments should go via the SBMLManager.
 * <p>
 * CyActivator creates the single instance and registers it as an OSGi service,
 * so that other apps can look it up.
 */
public class SBMLManager implements NetworkAboutToBeDestroyedListener, CompTargets {
    private static final Logger logger = LoggerFactory.getLogger(SBMLManager.class);
    private final CyApplicationManager cyApplicationManager;

    /*
     * currentSUID and network2sbml are written from Cytoscape event handlers
     * (network/selection listeners, session restore) and read from the WebViewPanel's
     * background render thread (PanelUpdater, run on its LatestTaskExecutor).
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
    // notified when a session is restored, e.g. the info panel, which rendered before
    private final List<Runnable> sessionRestoredListeners = new CopyOnWriteArrayList<>();
    // the COMBINE archives the documents were imported from, by root network SUID
    private final Map<Long, ArchiveImport> archives = new ConcurrentHashMap<>();

    // resolvers of the comp references of the read documents, which know the external
    // documents read; not part of the session
    private final List<SBaseRefResolver> sBaseRefResolvers = new CopyOnWriteArrayList<>();

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
     * Access to the SBML to network mapper.
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
            archives.remove(rootSUID);
            removeUnusedResolvers();
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
     * Adds a listener that is notified when a session is restored: the mapping and the
     * archives of the session are set. Cytoscape can fire the network events of the loaded
     * session before, so a view of the mapping (the info panel) updates on this.
     */
    public void addSessionRestoredListener(Runnable listener) {
        sessionRestoredListeners.add(listener);
    }

    /** Notifies the listeners that a session is restored, called by {@link SessionData}. */
    void sessionRestored() {
        for (Runnable listener : sessionRestoredListeners) {
            try {
                listener.run();
            } catch (RuntimeException e) {
                logger.error("A session restored listener failed", e);
            }
        }
    }

    /** Registers the COMBINE archive the document of the root network was imported from. */
    public void addArchive(Long rootNetworkSUID, ArchiveImport archive) {
        archives.put(rootNetworkSUID, archive);
    }

    /** The COMBINE archive the document of the root network was imported from. */
    public Optional<ArchiveImport> getArchive(Long rootNetworkSUID) {
        return Optional.ofNullable(archives.get(rootNetworkSUID));
    }

    /** The COMBINE archive the document was imported from. */
    // reason: the document instance of a network, SBMLDocument.equals compares the content
    @SuppressWarnings("ReferenceEquality")
    public Optional<ArchiveImport> getArchive(SBMLDocument document) {
        for (Map.Entry<Long, SBMLDocument> entry : network2sbml.getDocumentMap().entrySet()) {
            if (entry.getValue() == document) {
                return getArchive(entry.getKey());
            }
        }
        return Optional.empty();
    }

    /** The archives by root network SUID, for the session. */
    public Map<Long, ArchiveImport> getArchives() {
        return Map.copyOf(archives);
    }

    /** Replaces the archives, on session restore. */
    public void setArchives(Map<Long, ArchiveImport> archives) {
        this.archives.clear();
        this.archives.putAll(archives);
    }

    /**
     * Adds the resolver the reader resolved the comp references of a document with, so
     * that the info panel finds the same targets, with the same metaids.
     */
    public void addSBaseRefResolver(SBaseRefResolver resolver) {
        if (sBaseRefResolvers.stream().noneMatch(r -> r == resolver)) {
            sBaseRefResolvers.add(resolver);
        }
    }

    /**
     * The resolver of the comp references of the document of the element: the one of the
     * reader that read the document, else a new one (e.g. for a document of a session).
     *
     * @return the resolver, null if the element is not part of a document
     */
    public SBaseRefResolver getSBaseRefResolver(SBase sbase) {
        SBMLDocument document = sbase.getSBMLDocument();
        if (document == null) {
            return null;
        }
        for (SBaseRefResolver resolver : sBaseRefResolvers) {
            if (resolver.models().owns(document)) {
                return resolver;
            }
        }
        // a document of a session: a resolver that uses the open documents, created once
        SBaseRefResolver resolver = new SBaseRefResolver(new CompModels(document, openDocuments()));
        sBaseRefResolvers.add(resolver);
        return resolver;
    }

    /** The open documents with a location, by location. */
    private Map<URI, SBMLDocument> openDocuments() {
        Map<URI, SBMLDocument> documents = new HashMap<>();
        for (SBMLDocument document : network2sbml.getDocumentMap().values()) {
            if (document.isSetLocationURI()) {
                try {
                    documents.putIfAbsent(new URI(document.getLocationURI()), document);
                } catch (URISyntaxException e) {
                    logger.debug("Location is no URI: {}", document.getLocationURI());
                }
            }
        }
        return documents;
    }

    @Override
    public SBaseRefResolver resolver(SBase sbase) {
        return getSBaseRefResolver(sbase);
    }

    /** Stores the model the network collection of the network was created from. */
    public void addModelForNetwork(CyNetwork network, Model model) {
        network2sbml.putModel(NetworkUtil.getRootNetworkSUID(network), model);
    }

    @Override
    public Optional<Long> rootNetwork(Model model) {
        return network2sbml.findRootNetwork(model);
    }

    /** Removes the resolvers whose document has no network anymore. */
    private void removeUnusedResolvers() {
        // by identity, JSBML's equals compares the content
        Set<SBMLDocument> documents = Collections.newSetFromMap(new IdentityHashMap<>());
        documents.addAll(network2sbml.getDocumentMap().values());
        sBaseRefResolvers.removeIf(
                resolver -> !documents.contains(resolver.models().document()));
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
     * Maps a node added to the network after the import (a cofactor clone) to the SBase
     * of its cyId, so that the node shows the information of the SBase.
     */
    public void addNodeMapping(CyNetwork network, CyNode node) {
        String cyId = AttributeUtil.get(network, node, SBML.ATTR_CYID, String.class);
        network2sbml.putNode(NetworkUtil.getRootNetworkSUID(network), cyId, node.getSUID());
    }

    /**
     * Removes the mapping of a node added with {@link #addNodeMapping}.
     */
    public void removeNodeMapping(CyNetwork network, CyNode node) {
        network2sbml.removeNode(NetworkUtil.getRootNetworkSUID(network), node.getSUID());
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
        logger.debug("Set current network to root SUID: {}", rootNetworkSUID);
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
        logger.debug("Current network set to: {}", currentSUID);
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

    // ------------------------------------------------------------

    /**
     * Set all information in SBMLManager from given Network2SBMLMapper.
     * This function is used to set the Network2SBMLMapper from a stored state.
     * For instance during session reloading.
     */
    public void setSBML2NetworkMapper(Network2SBMLMapper mapper) {
        logger.debug("SBMLManager from given mapper");

        network2sbml = mapper;
        // the documents of a session are new documents
        sBaseRefResolvers.clear();

        // Set current network and tree
        CyNetwork currentNetwork = cyApplicationManager.getCurrentNetwork();
        updateCurrent(currentNetwork);
    }

    // ------------------------------------------------------------

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
