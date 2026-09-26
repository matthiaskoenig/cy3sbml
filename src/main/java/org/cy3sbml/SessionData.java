package org.cy3sbml;

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
import java.util.List;
import java.util.Map;
import javax.xml.stream.XMLStreamException;
import org.cy3sbml.cofactors.CofactorManager;
import org.cy3sbml.cofactors.Network2CofactorMapper;
import org.cy3sbml.mapping.Network2SBMLMapper;
import org.cy3sbml.mapping.One2ManyMapping;
import org.cy3sbml.util.IOUtil;
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

    /**
     * Session data is locally saved in given directory.
     * Normally this is the CytoscapeConfiguration/cy3sbml directory.
     */
    public SessionData() {}

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
    public static void saveSessionData(SessionAboutToBeSavedEvent event) {
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

        // get SBMLManager for serialization
        SBMLManager sbmlManager = SBMLManager.getInstance();
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
        CofactorManager cofactorManager = CofactorManager.getInstance();
        Network2CofactorMapper network2cofactorMapper = cofactorManager.getNetwork2CofactorMapper();
        File cofactorFile = new File(directory, NETWORK2COFACTOR_ID);
        try (ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(cofactorFile))) {
            out.writeObject(network2cofactorMapper);
            files.add(cofactorFile);
        } catch (IOException e) {
            logger.error("Serialization of Network2CofactorMapper failed.", e);
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
     */
    private static void loadSessionData(SessionLoadedEvent event) {
        CySession session = event.getLoadedSession();
        // check if app files exist
        if (session.getAppFileListMap() == null || session.getAppFileListMap().size() == 0) {
            return;
        }

        // iterate
        List<File> files = event.getLoadedSession().getAppFileListMap().get(APP_ID);
        for (File f : files) {
            String name = f.getName();
            logger.debug("cy3sbml file in session: " + f.getName());
            // deserialize documentMap
            if (name.equals(NETWORK2SBMLMAPPER_ID)) {
                logger.debug("Deserialize <Network2SBMLMapper>");

                try (ObjectInput input = new ObjectInputStream(new BufferedInputStream(new FileInputStream(f)))) {
                    // read mapper
                    Network2SBMLMapper mapper = (Network2SBMLMapper) input.readObject();
                    // update suids in mapper & set in manager
                    Network2SBMLMapper updatedMapper = updateSUIDsInMapper(session, mapper);
                    SBMLManager sbmlManager = SBMLManager.getInstance();
                    // set updated mapper
                    sbmlManager.setSBML2NetworkMapper(updatedMapper);

                } catch (IOException | ClassNotFoundException | ClassCastException e) {
                    logger.error("Deserialization of Network2SBMLMapper failed.", e);
                }
            }

            // deserialize
            else if (name.equals(NETWORK2COFACTOR_ID)) {
                logger.debug("Deserialize <Network2CofactorMapper>");

                try (ObjectInput input = new ObjectInputStream(new BufferedInputStream(new FileInputStream(f)))) {
                    // read mapper
                    Network2CofactorMapper m = (Network2CofactorMapper) input.readObject();
                    CofactorManager cofactorManager = CofactorManager.getInstance();

                    // update suids in mapper & set in manager
                    Network2CofactorMapper updatedMapper = updateSUIDsInCofactorMapper(session, m);
                    // set updated mapper
                    cofactorManager.setNetwork2CofactorMapper(updatedMapper);

                } catch (IOException | ClassNotFoundException | ClassCastException e) {
                    logger.error("Deserialization of Network2CofactorMapper failed.", e);
                }
            }
        }
    }

    /**
     * Updates the changed SUIDs in the data structure.
     * <p>
     * SUIDs are not persistent across sessions.
     * Consequently the mappings have to be updated.
     * <p>
     * The network, node and edge SUIDs can be updated via:
     * Long newSUID = s.getObjectByCyId(oldSUID, CyIdentifiable.class).getSUID();
     */
    private static Network2SBMLMapper updateSUIDsInMapper(CySession s, Network2SBMLMapper m) {
        // mapper with updated SUIDS
        Network2SBMLMapper newM = new Network2SBMLMapper();

        // documentMap
        Map<Long, SBMLDocument> documentMap = m.getDocumentMap();
        for (Long networkSuid : documentMap.keySet()) {
            Long newNetworkSuid = s.getObject(networkSuid, CyNetwork.class).getSUID();
            SBMLDocument doc = documentMap.get(networkSuid);
            One2ManyMapping<String, Long> nsb2node = m.getSBase2CyNodeMapping(networkSuid);

            // replace keys in nsb2node mapping
            One2ManyMapping<String, Long> newNsb2node = new One2ManyMapping<String, Long>();
            for (String key : nsb2node.keySet()) {
                for (Long suid : nsb2node.getValues(key)) {
                    Long newSuid = s.getObject(suid, CyNode.class).getSUID();
                    newNsb2node.put(key, newSuid);
                }
            }
            newM.putDocument(newNetworkSuid, doc, newNsb2node);
        }
        return newM;
    }

    /**
     * Updates the changed SUIDs in the data structure.
     * <p>
     * SUIDs are not persistent across sessions.
     * Consequently the mappings have to be updated.
     * <p>
     * The network, node and edge SUIDs can be updated via:
     * Long newSUID = s.getObjectByCyId(oldSUID, CyIdentifiable.class).getSUID();
     */
    private static Network2CofactorMapper updateSUIDsInCofactorMapper(CySession s, Network2CofactorMapper m) {
        logger.debug("Update SUIDs in Network2CofactorMapper");
        // mapper with updated SUIDS
        Network2CofactorMapper newM = new Network2CofactorMapper();

        // update all network & node SUIDs
        for (Long networkSUID : m.keySet()) {
            Long newNetworkSUID = s.getObject(networkSUID, CyNetwork.class).getSUID();
            One2ManyMapping<Long, Long> cofactor2clones = m.getCofactor2CloneMapping(networkSUID);
            for (Long cofactorSUID : cofactor2clones.keySet()) {
                // update cofactor SUID
                Long newCofactorSUID = s.getObject(cofactorSUID, CyNode.class).getSUID();
                for (Long cloneSUID : cofactor2clones.getValues(cofactorSUID)) {
                    // update clone SUID
                    Long newCloneSUID = s.getObject(cloneSUID, CyNode.class).getSUID();
                    newM.put(newNetworkSUID, newCofactorSUID, newCloneSUID);
                }
            }
        }
        return newM;
    }
}
