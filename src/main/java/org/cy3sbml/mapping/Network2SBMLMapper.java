package org.cy3sbml.mapping;

import java.io.Serializable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.sbml.jsbml.Model;
import org.sbml.jsbml.SBMLDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Mapping between CyNetworks and SBMLDocuments.
 * The mapping is defined for the CyRootNetwork. All subnetworks use the same
 * mapping and can access it via the rootNetwork SUID.
 * <p>
 * The rootNetwork SUID is accessible via
 * NetworkUtil.getRootNetworkSUID(CyNetwork network);
 * <p>
 * The SBMLReaderTaskFactory creates multiple networks with the mapping between networks
 * and SBMLDocuments managed by the SBMLManager which updates this mapper.
 * The mappings should only be changed via the SBMLManager and not directly.
 * <p>
 * Reached from both the Cytoscape event/EDT thread (put/removeDocument, called through
 * SBMLManager) and the WebViewPanel's background panel-update thread (reads, also through
 * SBMLManager), so every method is {@code synchronized} on the instance. Methods that expose
 * the outer map or its keys ({@link #getDocumentMap()}, {@link #keySet()}) return defensive
 * copies so a caller can iterate the result without racing a concurrent writer; the contained
 * {@link One2ManyMapping} instances are thread-safe on their own and are handed out live.
 * The serialized fields are unchanged, so session files written by an older version still
 * deserialize.
 */
public class Network2SBMLMapper implements Serializable {
    private static final Logger logger = LoggerFactory.getLogger(Network2SBMLMapper.class);
    private static final long serialVersionUID = 1L;

    private Map<Long, SBMLDocument> documentMap;
    private Map<Long, One2ManyMapping<String, Long>> sbase2nodeMappingMap;
    private Map<Long, One2ManyMapping<Long, String>> node2sbaseMappingMap;
    // the model of each root network; null in sessions of older versions
    private Map<Long, Model> modelMap;

    public Network2SBMLMapper() {
        logger.debug("Network2SBMLMapper created");
        initMaps();
    }

    private void initMaps() {
        documentMap = new HashMap<>();
        sbase2nodeMappingMap = new HashMap<>();
        node2sbaseMappingMap = new HashMap<>();
        modelMap = new HashMap<>();
    }

    /** The model map, created for a mapper of a session of an older version. */
    private Map<Long, Model> modelMap() {
        if (modelMap == null) {
            modelMap = new HashMap<>();
        }
        return modelMap;
    }

    /** Stores the model the root network was created from. */
    public synchronized void putModel(Long rootSUID, Model model) {
        modelMap().put(rootSUID, model);
    }

    /** The models of the root networks; a defensive copy. */
    public synchronized Map<Long, Model> getModelMap() {
        return new HashMap<>(modelMap());
    }

    /**
     * The root network created from the model object; by identity, as JSBML's equals
     * compares the content.
     */
    @SuppressWarnings("ReferenceEquality")
    public synchronized Optional<Long> findRootNetwork(Model model) {
        for (Map.Entry<Long, Model> entry : modelMap().entrySet()) {
            if (entry.getValue() == model) {
                return Optional.of(entry.getKey());
            }
        }
        return Optional.empty();
    }

    /**
     * The SBMLDocument is stored with the SBML id to SUID (CyNode) mapping.
     * A reverse mapping from SUIDs to SBML ids is created in the process.
     */
    public synchronized void putDocument(Long rootSUID, SBMLDocument doc, One2ManyMapping<String, Long> mapping) {
        logger.debug("Network put: {}", rootSUID);
        documentMap.put(rootSUID, doc);
        sbase2nodeMappingMap.put(rootSUID, mapping);
        node2sbaseMappingMap.put(rootSUID, mapping.createReverseMapping());
    }

    /**
     * Maps a node created after the import (a cofactor clone) to the SBase of its cyId.
     */
    public synchronized void putNode(Long rootSUID, String cyId, Long nodeSUID) {
        One2ManyMapping<String, Long> sbase2node = sbase2nodeMappingMap.get(rootSUID);
        if (sbase2node == null || cyId == null) {
            return;
        }
        sbase2node.put(cyId, nodeSUID);
        node2sbaseMappingMap.get(rootSUID).put(nodeSUID, cyId);
    }

    /**
     * Removes the mapping of a node, e.g. of a cofactor clone that is merged.
     */
    public synchronized void removeNode(Long rootSUID, Long nodeSUID) {
        One2ManyMapping<Long, String> node2sbase = node2sbaseMappingMap.get(rootSUID);
        if (node2sbase == null) {
            return;
        }
        for (String cyId : node2sbase.getValues(nodeSUID)) {
            sbase2nodeMappingMap.get(rootSUID).removeValue(cyId, nodeSUID);
        }
        node2sbase.remove(nodeSUID);
    }

    /**
     * Removes the document for a given root network SUID.
     * This removes the mapping for all subnetworks of the given root network SUID.
     *
     * @param rootSUID root network SUID
     */
    public synchronized void removeDocument(Long rootSUID) {
        logger.debug("Network remove:{}", rootSUID);
        documentMap.remove(rootSUID);
        sbase2nodeMappingMap.remove(rootSUID);
        node2sbaseMappingMap.remove(rootSUID);
        modelMap().remove(rootSUID);
    }

    /**
     * Get SBMLDocument for given rootNetworkSUID.
     *
     * @param rootSUID root network SUID
     * @return SBMLDocument or null
     */
    public synchronized SBMLDocument getDocument(Long rootSUID) {
        SBMLDocument doc = null;
        if (rootSUID == null) {
            logger.debug("No SUID set. No SBMLDocument can be retrieved !");
            return null;
        }
        if (documentMap.containsKey(rootSUID)) {
            doc = documentMap.get(rootSUID);
        }
        return doc;
    }

    /**
     * Exists a SBMLDocument for the given rootNetwork.
     *
     * @param rootSUID root network SUID
     */
    public synchronized boolean containsDocument(Long rootSUID) {
        return documentMap.containsKey(rootSUID);
    }

    /**
     * Get all rootNetwork SUIDs which have an association SBMLDocument.
     * <p>
     * Returns a defensive copy so the caller can iterate it without racing a concurrent writer.
     *
     * @return Set of root network SUIDs
     */
    public synchronized Set<Long> keySet() {
        return new HashSet<>(documentMap.keySet());
    }

    /**
     * Get DocumentMap.
     * <p>
     * Returns a defensive copy so the caller can iterate it without racing a concurrent writer.
     */
    public synchronized Map<Long, SBMLDocument> getDocumentMap() {
        return new HashMap<>(documentMap);
    }

    /**
     * Mapping
     *
     * @param rootSUID root network SUID
     */
    public synchronized One2ManyMapping<Long, String> getCyNode2SBaseMapping(Long rootSUID) {
        if (rootSUID == null) {
            logger.warn("No current SUID set. Mapping can not be retrieved !");
            return null;
        }
        return node2sbaseMappingMap.get(rootSUID);
    }

    /**
     * Mapping
     *
     * @param rootSUID root network SUID
     */
    public synchronized One2ManyMapping<String, Long> getSBase2CyNodeMapping(Long rootSUID) {
        if (rootSUID == null) {
            logger.warn("No current SUID set. Mapping can not be retrieved !");
            return null;
        }
        return sbase2nodeMappingMap.get(rootSUID);
    }

    /**
     * Creates information string.
     */
    @Override
    public synchronized String toString() {
        String info = "\n--- SBML2NetworkMapping ---\n";
        for (Long key : documentMap.keySet()) {
            info += String.format(
                    "%s -> %s\n", key.toString(), documentMap.get(key).toString());
        }
        info += "-------------------------------";
        return info;
    }
}
