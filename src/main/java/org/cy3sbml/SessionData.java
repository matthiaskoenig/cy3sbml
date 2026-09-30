package org.cy3sbml;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputFilter;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import javax.xml.stream.XMLStreamException;
import org.cy3sbml.archive.ArchiveImport;
import org.cy3sbml.cofactors.CofactorManager;
import org.cy3sbml.cofactors.Network2CofactorMapper;
import org.cy3sbml.mapping.Network2SBMLMapper;
import org.cy3sbml.mapping.One2ManyMapping;
import org.cy3sbml.util.IOUtil;
import org.cytoscape.model.CyEdge;
import org.cytoscape.model.CyIdentifiable;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNode;
import org.cytoscape.session.CySession;
import org.cytoscape.session.events.SessionAboutToBeSavedEvent;
import org.cytoscape.session.events.SessionAboutToBeSavedListener;
import org.cytoscape.session.events.SessionLoadedEvent;
import org.cytoscape.session.events.SessionLoadedListener;
import org.sbml.jsbml.Model;
import org.sbml.jsbml.SBMLDocument;
import org.sbml.jsbml.SBMLException;
import org.sbml.jsbml.SBMLWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Saves the cy3sbml data of a Cytoscape session and restores it when the session is loaded.
 * <p>
 * The {@link Network2SBMLMapper} (with the SBML documents) and the {@link Network2CofactorMapper}
 * are written with Java serialization, the COMBINE archives of the documents as JSON. SUIDs are
 * not stable across sessions, so the restored mappings are translated to the SUIDs of the loaded
 * session. The SBML files of the documents are also written into the session, for the user;
 * they are not read on restore.
 * <p>
 * A session file can come from anywhere, so the deserialization only accepts the classes the
 * mappers consist of.
 */
public class SessionData implements SessionAboutToBeSavedListener, SessionLoadedListener {
    private static final Logger logger = LoggerFactory.getLogger(SessionData.class);
    private static final String APP_ID = "cy3sbml";
    private static final String NETWORK2SBMLMAPPER_ID = "Network2SBMLMapper.ser";
    private static final String NETWORK2COFACTOR_ID = "Network2Cofactors.ser";
    // the COMBINE archives of the documents, by root network SUID
    private static final String ARCHIVES_ID = "archives.json";
    private static final ObjectMapper JSON = new ObjectMapper();

    /**
     * The classes the serialized mappers consist of: the cy3sbml mappers, the JSBML documents
     * and the JDK values and collections they hold. Any other class in a session file is
     * rejected before it is instantiated, so a crafted session file cannot run code through
     * the deserialization of a class that is on the classpath of the bundle (a gadget chain).
     */
    static final ObjectInputFilter SESSION_FILTER = ObjectInputFilter.Config.createFilter(String.join(
            ";",
            "java.lang.*",
            "java.util.*",
            "java.math.*",
            "java.net.URI",
            "javax.xml.namespace.QName",
            "org.sbml.jsbml.**",
            "org.cy3sbml.mapping.*",
            "org.cy3sbml.cofactors.Network2CofactorMapper",
            "!*"));

    private final SBMLManager sbmlManager;
    private final CofactorManager cofactorManager;
    // the temporary directory of the last saved session, deleted on the next save and in dispose
    private Path savedDirectory;

    /**
     * Saves the mappers of the SBMLManager and the CofactorManager and
     * restores the loaded mappers in them.
     *
     * @param sbmlManager the manager of the SBML documents and their mappings
     * @param cofactorManager the manager of the cofactor clones
     */
    public SessionData(SBMLManager sbmlManager, CofactorManager cofactorManager) {
        this.sbmlManager = sbmlManager;
        this.cofactorManager = cofactorManager;
    }

    /** Adds the cy3sbml files to the session that is about to be saved. */
    @Override
    public void handleEvent(SessionAboutToBeSavedEvent event) {
        saveSessionData(event);
    }

    /** Restores the cy3sbml data of the loaded session. */
    @Override
    public void handleEvent(SessionLoadedEvent event) {
        loadSessionData(event);
    }

    /**
     * Writes the cy3sbml data into a temporary directory and adds the files to the session.
     * Cytoscape copies them into the session file after the event, so the directory is kept
     * until the next save or {@link #dispose()}.
     *
     * @param event the event of the session that is about to be saved
     */
    public synchronized void saveSessionData(SessionAboutToBeSavedEvent event) {
        logger.info("SessionAboutToBeSaved: save cy3sbml session state");
        deleteSavedDirectory();

        File directory;
        try {
            savedDirectory = Files.createTempDirectory(APP_ID);
            directory = savedDirectory.toFile();
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
        SBMLWriter writer = new SBMLWriter();
        for (Map.Entry<Long, SBMLDocument> entry : documentMap.entrySet()) {
            SBMLDocument doc = entry.getValue();
            // unique file (SBMLDocuments can have identical model ids)
            File sbmlFile = IOUtil.createUniqueFile(directory, sbmlFileName(entry.getKey(), doc), ".xml");
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
     * The name of the SBML file of a document: the model id, or the root network SUID if the
     * model has no id. Characters other than letters, digits, {@code _}, {@code -} and
     * {@code .} are replaced, so that an invalid model id cannot name a file outside the
     * directory.
     */
    static String sbmlFileName(Long rootSUID, SBMLDocument doc) {
        Model model = doc.getModel();
        String name = model != null && model.isSetId() ? model.getId() : rootSUID.toString();
        name = name.replaceAll("[^A-Za-z0-9_.-]", "_");
        // no hidden file and no "." or ".."
        return name.startsWith(".") ? "_" + name : name;
    }

    /** Deletes the temporary files of the last saved session, called when the app stops. */
    public synchronized void dispose() {
        deleteSavedDirectory();
    }

    private void deleteSavedDirectory() {
        if (savedDirectory == null) {
            return;
        }
        try (Stream<Path> files = Files.list(savedDirectory)) {
            for (Path file : files.toList()) {
                Files.deleteIfExists(file);
            }
            Files.deleteIfExists(savedDirectory);
        } catch (IOException e) {
            logger.warn("The session files in {} could not be deleted: {}", savedDirectory, e.getMessage());
        }
        savedDirectory = null;
    }

    /** Reads the serialized object of the file, accepting only the classes of {@code SESSION_FILTER}. */
    private static Object deserialize(File file) throws IOException, ClassNotFoundException {
        try (ObjectInputStream input = new ObjectInputStream(new BufferedInputStream(new FileInputStream(file)))) {
            input.setObjectInputFilter(SESSION_FILTER);
            return input.readObject();
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
        sbmlManager.sessionRestored();
    }

    private void loadAppFile(CySession session, File f) {
        String name = f.getName();
        if (name.equals(NETWORK2SBMLMAPPER_ID)) {
            logger.debug("Deserialize <Network2SBMLMapper>");
            try {
                Network2SBMLMapper mapper = (Network2SBMLMapper) deserialize(f);
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
            try {
                Network2CofactorMapper mapper = (Network2CofactorMapper) deserialize(f);
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
     * SUIDs of networks, nodes and edges that no longer exist are skipped.
     */
    static Network2CofactorMapper updateSUIDsInCofactorMapper(CySession s, Network2CofactorMapper m) {
        logger.debug("Update SUIDs in Network2CofactorMapper");
        Network2CofactorMapper newM = new Network2CofactorMapper();

        for (Long networkSUID : m.keySet()) {
            Long newNetworkSUID = newSUID(s, networkSUID, CyNetwork.class);
            if (newNetworkSUID == null) {
                continue;
            }
            for (Long cofactorSUID : m.getCofactors(networkSUID)) {
                Long newCofactorSUID = newSUID(s, cofactorSUID, CyNode.class);
                if (newCofactorSUID == null) {
                    continue;
                }
                for (Long cloneSUID : m.getClones(networkSUID, cofactorSUID)) {
                    Long newCloneSUID = newSUID(s, cloneSUID, CyNode.class);
                    if (newCloneSUID != null) {
                        newM.putClone(newNetworkSUID, newCofactorSUID, newCloneSUID);
                    }
                }
                double[] position = m.getPosition(networkSUID, cofactorSUID);
                if (position != null) {
                    newM.putPosition(newNetworkSUID, newCofactorSUID, position[0], position[1]);
                }
            }
            for (Map.Entry<Long, Long> entry : m.getCloneEdges(networkSUID).entrySet()) {
                Long newCloneEdgeSUID = newSUID(s, entry.getKey(), CyEdge.class);
                Long newEdgeSUID = newSUID(s, entry.getValue(), CyEdge.class);
                if (newCloneEdgeSUID != null && newEdgeSUID != null) {
                    newM.putCloneEdge(newNetworkSUID, newCloneEdgeSUID, newEdgeSUID);
                }
            }
        }
        return newM;
    }
}
