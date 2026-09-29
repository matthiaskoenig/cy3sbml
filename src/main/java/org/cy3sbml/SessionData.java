package org.cy3sbml;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.xml.stream.XMLStreamException;
import org.cy3sbml.archive.ArchiveImport;
import org.cy3sbml.cofactors.CofactorManager;
import org.cy3sbml.cofactors.Network2CofactorMapper;
import org.cy3sbml.mapping.Network2SBMLMapper;
import org.cy3sbml.mapping.One2ManyMapping;
import org.cy3sbml.util.IOUtil;
import org.cytoscape.model.CyIdentifiable;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNode;
import org.cytoscape.session.CySession;
import org.cytoscape.session.events.*;
import org.sbml.jsbml.Model;
import org.sbml.jsbml.SBMLDocument;
import org.sbml.jsbml.SBMLException;
import org.sbml.jsbml.SBMLWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class implements the saving of cy3sbml session data and
 * the restoring of the saved data.
 * The internal data structures are serialized on session saving
 * and deserialized on session loading.
 * <p>
 * In addition to the saved data structures the SBML files are written in
 * the session file. These are not used for deserialization.
 */
public class SessionData implements SessionAboutToBeSavedListener, SessionLoadedListener {
    private static final Logger logger = LoggerFactory.getLogger(SessionData.class);
    private static final String APP_ID = "cy3sbml";
    private static final String NETWORK2SBMLMAPPER_ID = "Network2SBMLMapper.ser";
    private static final String NETWORK2COFACTOR_ID = "Network2Cofactors.ser";
    // the COMBINE archives of the documents, by root network SUID
    private static final String ARCHIVES_ID = "archives.json";
    private static final ObjectMapper JSON = new ObjectMapper();

    private final SBMLManager sbmlManager;
    private final CofactorManager cofactorManager;

    /**
     * Saves the mappers of the SBMLManager and the CofactorManager and
     * restores the loaded mappers in them.
     */
    public SessionData(SBMLManager sbmlManager, CofactorManager cofactorManager) {
        this.sbmlManager = sbmlManager;
        this.cofactorManager = cofactorManager;
    }

    /**
     * Save session.
     */
    @Override
    public void handleEvent(SessionAboutToBeSavedEvent event) {
        saveSessionData(event);
    }

    /**
     * Load Session.
     */
    @Override
    public void handleEvent(SessionLoadedEvent event) {
        loadSessionData(event);
    }

    /**
     * Save the session data from cy3sbml.
     */
    public void saveSessionData(SessionAboutToBeSavedEvent event) {
        logger.info("SessionAboutToBeSaved: save cy3sbml session state");

        File directory;
        try {
            directory = Files.createTempDirectory(APP_ID).toFile();
        } catch (IOException e) {
            logger.error("Could not create temporary directory for session data", e);
            return;
        }

        // Files to save
        List<File> files = new ArrayList<>();

        // SBML mapper for serialization
        Network2SBMLMapper mapper = sbmlManager.getNetwork2SBMLMapper();
        Map<Long, SBMLDocument> documentMap = mapper.getDocumentMap();

        logger.debug("Save SBMLDocuments");
        for (Long rootSUID : documentMap.keySet()) {
            SBMLDocument doc = documentMap.get(rootSUID);
            SBMLWriter writer = new SBMLWriter();

            // use SUID if no model id is set
            String sbmlId = rootSUID.toString();
            Model model = doc.getModel();
            if ((model != null) && model.isSetId()) {
                sbmlId = model.getId();
            }

            // unique file (SBMLDocuments can have identical sbmlId)
            File sbmlFile = IOUtil.createUniqueFile(directory, sbmlId, ".xml");
            try {
                writer.write(doc, sbmlFile);
                files.add(sbmlFile);
            } catch (SBMLException | XMLStreamException | IOException e) {
                logger.error("Saving of SBMLDocument failed", e);
            }
        }

        // Serialize
        logger.debug("Serializing <Network2SBMLMapper>");
        File mapperFile = new File(directory, NETWORK2SBMLMAPPER_ID);
        try (ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(mapperFile))) {
            out.writeObject(mapper);
            files.add(mapperFile);
        } catch (IOException e) {
            logger.error("Serialization of Network2SBMLMapper failed.", e);
        }

        // Serialize
        logger.debug("Serializing <Network2CofactorMapper>");
        Network2CofactorMapper network2cofactorMapper = cofactorManager.getNetwork2CofactorMapper();
        File cofactorFile = new File(directory, NETWORK2COFACTOR_ID);
        try (ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(cofactorFile))) {
            out.writeObject(network2cofactorMapper);
            files.add(cofactorFile);
        } catch (IOException e) {
            logger.error("Serialization of Network2CofactorMapper failed.", e);
        }

        logger.debug("Writing the archives");
        File archivesFile = new File(directory, ARCHIVES_ID);
        try {
            JSON.writeValue(archivesFile, sbmlManager.getArchives());
            files.add(archivesFile);
        } catch (IOException e) {
            logger.error("Writing the archives failed.", e);
        }

        // Write files in session file
        try {
            event.addAppFiles(APP_ID, files);
        } catch (Exception e) { // SessionAboutToBeSavedEvent.addAppFiles declares Exception
            logger.error("File could not be added to app files.", e);
        }
    }

    /**
     * Load Session data for cy3sbml.
     * <p>
     * The listener is a boundary: a failure in one app file is logged and the
     * remaining files are still restored.
     */
    void loadSessionData(SessionLoadedEvent event) {
        CySession session = event.getLoadedSession();
        Map<String, List<File>> appFiles = session.getAppFileListMap();
        List<File> files = appFiles == null ? null : appFiles.get(APP_ID);
        if (files == null) {
            return;
        }
        for (File f : files) {
            logger.debug("cy3sbml file in session: {}", f.getName());
            try {
                loadAppFile(session, f);
            } catch (RuntimeException e) {
                logger.error("Could not restore the session file: {}", f.getName(), e);
            }
        }
    }

    private void loadAppFile(CySession session, File f) {
        String name = f.getName();
        if (name.equals(NETWORK2SBMLMAPPER_ID)) {
            logger.debug("Deserialize <Network2SBMLMapper>");
            try (ObjectInput input = new ObjectInputStream(new BufferedInputStream(new FileInputStream(f)))) {
                Network2SBMLMapper mapper = (Network2SBMLMapper) input.readObject();
                sbmlManager.setSBML2NetworkMapper(updateSUIDsInMapper(session, mapper));
            } catch (IOException | ClassNotFoundException | ClassCastException e) {
                logger.error("Deserialization of Network2SBMLMapper failed.", e);
            }
        } else if (name.equals(ARCHIVES_ID)) {
            logger.debug("Reading the archives");
            try {
                Map<Long, ArchiveImport> archives = JSON.readValue(f, new TypeReference<Map<Long, ArchiveImport>>() {});
                Map<Long, ArchiveImport> restored = new HashMap<>();
                for (Map.Entry<Long, ArchiveImport> entry : archives.entrySet()) {
                    Long rootSUID = newSUID(session, entry.getKey(), CyNetwork.class);
                    if (rootSUID != null) {
                        restored.put(rootSUID, entry.getValue());
                    }
                }
                sbmlManager.setArchives(restored);
            } catch (IOException e) {
                logger.error("Reading the archives failed.", e);
            }
        } else if (name.equals(NETWORK2COFACTOR_ID)) {
            logger.debug("Deserialize <Network2CofactorMapper>");
            try (ObjectInput input = new ObjectInputStream(new BufferedInputStream(new FileInputStream(f)))) {
                Network2CofactorMapper mapper = (Network2CofactorMapper) input.readObject();
                cofactorManager.setNetwork2CofactorMapper(updateSUIDsInCofactorMapper(session, mapper));
            } catch (IOException | ClassNotFoundException | ClassCastException e) {
                logger.error("Deserialization of Network2CofactorMapper failed.", e);
            }
        }
    }

    /**
     * Returns the SUID of the object in the loaded session for the SUID it had
     * when the session was saved, or null if the object no longer exists
     * (e.g. a node deleted after the import).
     */
    private static <T extends CyIdentifiable> Long newSUID(CySession s, Long oldSUID, Class<T> type) {
        T object = s.getObject(oldSUID, type);
        if (object == null) {
            logger.warn(
                    "{} with SUID {} not found in the loaded session, mapping skipped", type.getSimpleName(), oldSUID);
            return null;
        }
        return object.getSUID();
    }

    /**
     * Updates the changed SUIDs in the data structure.
     * <p>
     * SUIDs are not persistent across sessions.
     * Consequently the mappings have to be updated.
     * SUIDs of networks and nodes that no longer exist are skipped.
     */
    static Network2SBMLMapper updateSUIDsInMapper(CySession s, Network2SBMLMapper m) {
        Network2SBMLMapper newM = new Network2SBMLMapper();

        Map<Long, SBMLDocument> documentMap = m.getDocumentMap();
        for (Long networkSuid : documentMap.keySet()) {
            Long newNetworkSuid = newSUID(s, networkSuid, CyNetwork.class);
            if (newNetworkSuid == null) {
                continue;
            }
            SBMLDocument doc = documentMap.get(networkSuid);
            One2ManyMapping<String, Long> nsb2node = m.getSBase2CyNodeMapping(networkSuid);

            One2ManyMapping<String, Long> newNsb2node = new One2ManyMapping<>();
            for (String key : nsb2node.keySet()) {
                for (Long suid : nsb2node.getValues(key)) {
                    Long newSuid = newSUID(s, suid, CyNode.class);
                    if (newSuid != null) {
                        newNsb2node.put(key, newSuid);
                    }
                }
            }
            newM.putDocument(newNetworkSuid, doc, newNsb2node);
            Model model = m.getModelMap().get(networkSuid);
            if (model != null) {
                newM.putModel(newNetworkSuid, model);
            }
        }
        return newM;
    }

    /**
     * Updates the changed SUIDs in the cofactor data structure.
     * <p>
     * SUIDs of networks and nodes that no longer exist are skipped.
     */
    static Network2CofactorMapper updateSUIDsInCofactorMapper(CySession s, Network2CofactorMapper m) {
        logger.debug("Update SUIDs in Network2CofactorMapper");
        Network2CofactorMapper newM = new Network2CofactorMapper();

        for (Long networkSUID : m.keySet()) {
            Long newNetworkSUID = newSUID(s, networkSUID, CyNetwork.class);
            if (newNetworkSUID == null) {
                continue;
            }
            One2ManyMapping<Long, Long> cofactor2clones = m.getCofactor2CloneMapping(networkSUID);
            for (Long cofactorSUID : cofactor2clones.keySet()) {
                Long newCofactorSUID = newSUID(s, cofactorSUID, CyNode.class);
                if (newCofactorSUID == null) {
                    continue;
                }
                for (Long cloneSUID : cofactor2clones.getValues(cofactorSUID)) {
                    Long newCloneSUID = newSUID(s, cloneSUID, CyNode.class);
                    if (newCloneSUID != null) {
                        newM.put(newNetworkSUID, newCofactorSUID, newCloneSUID);
                    }
                }
            }
        }
        return newM;
    }
}
