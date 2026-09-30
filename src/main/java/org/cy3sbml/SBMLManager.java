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
import org.cytoscape.model.CyNetworkManager;
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

    // notified when a session is restored, e.g. the info panel, which rendered before
    private final List<Runnable> sessionRestoredListeners = new CopyOnWriteArrayList<>();
    // the COMBINE archives the documents were imported from, by root network SUID
    private final Map<Long, ArchiveImport> archives = new ConcurrentHashMap<>();

    // resolvers of the comp references of the read documents, which know the external
    // documents read; not part of the session
    private final List<SBaseRefResolver> sBaseRefResolvers = new CopyOnWriteArrayList<>();

    /**
     * Creates an empty manager.
     *
     * @param cyApplicationManager the application manager, for the current network after a
     *     session is restored
     */
    public SBMLManager(CyApplicationManager cyApplicationManager) {
        this.cyApplicationManager = cyApplicationManager;
        this.network2sbml = new Network2SBMLMapper();
    }

    /**
     * The mapper of the root networks to their documents and node mappings.
     * The mapper must not be modified, use the methods of the manager.
     *
     * @return the current mapper
     */
    public Network2SBMLMapper getNetwork2SBMLMapper() {
        return network2sbml;
    }

    /**
     * Adds the document of a network with the mapping of the cyIds of its SBases to the
     * node SUIDs.
     * <p>
     * The document is stored for the root network of the network, so that all subnetworks
     * of the collection find it.
     *
     * @param doc the document
     * @param network a network of the collection
     * @param mapping the mapping of the cyIds (metaIds) of the SBases to the node SUIDs
     */
    public void addSBMLForNetwork(SBMLDocument doc, CyNetwork network, One2ManyMapping<String, Long> mapping) {
        addSBMLForNetwork(doc, NetworkUtil.getRootNetworkSUID(network), mapping);
    }

    /**
     * Adds the document of a root network with the mapping of the cyIds of its SBases to
     * the node SUIDs.
     *
     * @param doc the document
     * @param rootNetworkSUID the root network SUID
     * @param mapping the mapping of the cyIds (metaIds) of the SBases to the node SUIDs
     */
    public void addSBMLForNetwork(SBMLDocument doc, Long rootNetworkSUID, One2ManyMapping<String, Long> mapping) {
        network2sbml.putDocument(rootNetworkSUID, doc, mapping);
    }

    /**
     * Removes the document of the network if it is the last network of its collection:
     * the root network has no other subnetwork.
     *
     * @param network the network that is destroyed
     * @return true if the document was removed, false if other networks of the collection
     *     still use it
     */
    public Boolean removeSBMLForNetwork(CyNetwork network) {
        return removeSBMLForNetwork(network, null);
    }

    /**
     * Removes the document of the network if it is the last registered network of its
     * collection. Other subnetworks of the root network that are not registered, such as the
     * networks of group nodes, do not keep the document.
     *
     * @param network the network that is destroyed
     * @param networkManager the network manager, null to count all subnetworks
     * @return true if the document was removed
     */
    private boolean removeSBMLForNetwork(CyNetwork network, CyNetworkManager networkManager) {
        Long rootSUID = NetworkUtil.getRootNetworkSUID(network);
        CyRootNetwork rootNetwork = ((CySubNetwork) network).getRootNetwork();
        long others = rootNetwork.getSubNetworkList().stream()
                .filter(subnetwork -> !subnetwork.equals(network))
                .filter(subnetwork -> networkManager == null || networkManager.networkExists(subnetwork.getSUID()))
                .count();
        if (others == 0) {
            network2sbml.removeDocument(rootSUID);
            archives.remove(rootSUID);
            removeUnusedResolvers();
            logger.info("SBMLDocument removed for rootSUID: {}", rootSUID);
            return true;
        }
        logger.info("SBMLDocument not removed for rootSUID: {}, {} other networks use it", rootSUID, others);
        return false;
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

    /**
     * Registers the COMBINE archive the document of the root network was imported from.
     *
     * @param rootNetworkSUID the root network SUID
     * @param archive the archive import
     */
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
     * The mapping of the cyIds (metaIds) of the SBases to the node SUIDs of the collection
     * of the network.
     *
     * @param network a network of the collection
     * @return the mapping, null if the network has no document
     */
    public One2ManyMapping<String, Long> getMapping(CyNetwork network) {
        return getMapping(NetworkUtil.getRootNetworkSUID(network));
    }

    /**
     * The mapping of the cyIds (metaIds) of the SBases to the node SUIDs of the root network.
     *
     * @param rootNetworkSUID the root network SUID
     * @return the mapping, null if the root network has no document
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
     * Sets the current document to the one of the collection of the network.
     *
     * @param network the current network, may be null
     */
    public void updateCurrent(CyNetwork network) {
        updateCurrent(NetworkUtil.getRootNetworkSUID(network));
    }

    /**
     * Sets the current document to the one of the root network; no current document if the
     * root network has none.
     *
     * @param rootNetworkSUID the root network SUID, may be null
     */
    public void updateCurrent(Long rootNetworkSUID) {
        currentSUID =
                rootNetworkSUID != null && network2sbml.containsDocument(rootNetworkSUID) ? rootNetworkSUID : null;
        logger.debug("Current network set to: {}", currentSUID);
    }

    /**
     * The root network SUID of the current document.
     *
     * @return the SUID, null if there is no current document
     */
    public Long getCurrentSUID() {
        return currentSUID;
    }

    /**
     * The current document, of the collection of the current network.
     *
     * @return the document, null if there is no current document
     */
    public SBMLDocument getCurrentSBMLDocument() {
        return getSBMLDocument(currentSUID);
    }

    /** The model of the current network collection, see {@link #getModel(Long)}. */
    public Model getCurrentModel() {
        return getModel(currentSUID);
    }

    /**
     * The document of the collection of the network.
     *
     * @param network a network, may be null
     * @return the document, null if the network has none
     */
    public SBMLDocument getSBMLDocument(CyNetwork network) {
        return getSBMLDocument(NetworkUtil.getRootNetworkSUID(network));
    }

    /**
     * The document of the root network.
     *
     * @param rootNetworkSUID root network SUID, may be null
     * @return the document, null if the root network has none
     */
    public SBMLDocument getSBMLDocument(Long rootNetworkSUID) {
        return network2sbml.getDocument(rootNetworkSUID);
    }

    /**
     * The model the network collection of the root network was created from: the main model,
     * a comp model definition, the model of an external model definition or the flat model.
     * The main model of the document for a network collection without stored model (a
     * session of an older version).
     *
     * @param rootNetworkSUID root network SUID, may be null
     * @return the model, null if the root network has no document or its document no model
     */
    public Model getModel(Long rootNetworkSUID) {
        Model model = network2sbml.getModel(rootNetworkSUID);
        if (model != null) {
            return model;
        }
        SBMLDocument document = getSBMLDocument(rootNetworkSUID);
        return document != null && document.isSetModel() ? document.getModel() : null;
    }

    /**
     * The mapping of the node SUIDs to the cyIds (metaIds) of the current document.
     *
     * @return the mapping, null if there is no current document
     */
    public One2ManyMapping<Long, String> getCurrentCyNode2SBaseMapping() {
        return network2sbml.getCyNode2SBaseMapping(currentSUID);
    }

    /**
     * The mapping of the cyIds (metaIds) to the node SUIDs of the current document.
     *
     * @return the mapping, null if there is no current document
     */
    public One2ManyMapping<String, Long> getCurrentSBase2CyNodeMapping() {
        return network2sbml.getSBase2CyNodeMapping(currentSUID);
    }

    /**
     * The SBase of the current document with the cyId (the metaId the reader sets on every
     * SBase).
     *
     * @param cyId the cyId of a node
     * @return the SBase, null if there is no current document or it has no such SBase
     */
    public SBase getSBaseByCyId(String cyId) {
        return getSBaseByCyId(cyId, currentSUID);
    }

    /**
     * The SBase of the document of the root network with the cyId (metaId).
     *
     * @param cyId the cyId of a node
     * @param rootNetworkSUID the root network SUID
     * @return the SBase, null if the root network has no document or it has no such SBase
     */
    public SBase getSBaseByCyId(String cyId, Long rootNetworkSUID) {
        SBMLDocument doc = network2sbml.getDocument(rootNetworkSUID);
        return doc == null ? null : doc.getElementByMetaId(cyId);
    }

    /**
     * The cyIds (metaIds) of the SBases of the nodes in the current document.
     *
     * @param suids the node SUIDs
     * @return the cyIds, empty if there is no current document
     */
    public List<String> getCyIdsFromSUIDs(List<Long> suids) {
        One2ManyMapping<Long, String> mapping = getCurrentCyNode2SBaseMapping();
        return mapping == null ? new ArrayList<>() : new ArrayList<>(mapping.getValues(suids));
    }

    /** The documents by root network SUID. */
    @Override
    public String toString() {
        return network2sbml.toString();
    }

    /**
     * Replaces the documents and mappings with the ones of the mapper, when a session is
     * restored, and updates the current document for the current network.
     *
     * @param mapper the restored mapper, with the SUIDs of the loaded session
     */
    public void setSBML2NetworkMapper(Network2SBMLMapper mapper) {
        network2sbml = mapper;
        // the documents of a session are new documents
        sBaseRefResolvers.clear();

        // Set current network and tree
        CyNetwork currentNetwork = cyApplicationManager.getCurrentNetwork();
        updateCurrent(currentNetwork);
    }

    /**
     * Removes the document when the last network of its collection is destroyed, also when
     * a session is closed (all networks are destroyed).
     */
    @Override
    public void handleEvent(NetworkAboutToBeDestroyedEvent e) {
        removeSBMLForNetwork(e.getNetwork(), e.getSource());
    }
}
