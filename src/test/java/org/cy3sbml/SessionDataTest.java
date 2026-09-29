package org.cy3sbml;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.cy3sbml.cofactors.CofactorManager;
import org.cy3sbml.cofactors.Network2CofactorMapper;
import org.cy3sbml.mapping.Network2SBMLMapper;
import org.cy3sbml.mapping.One2ManyMapping;
import org.cy3sbml.reader.SBMLReaderTask;
import org.cy3sbml.util.DistribUtil;
import org.cy3sbml.util.NetworkUtil;
import org.cytoscape.application.CyApplicationManager;
import org.cytoscape.ding.NetworkViewTestSupport;
import org.cytoscape.group.CyGroupFactory;
import org.cytoscape.group.GroupTestSupport;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNetworkFactory;
import org.cytoscape.model.CyNode;
import org.cytoscape.model.NetworkTestSupport;
import org.cytoscape.model.subnetwork.CyRootNetwork;
import org.cytoscape.model.subnetwork.CySubNetwork;
import org.cytoscape.session.CySession;
import org.cytoscape.session.CySessionManager;
import org.cytoscape.session.events.SessionAboutToBeSavedEvent;
import org.cytoscape.session.events.SessionLoadedEvent;
import org.cytoscape.view.model.CyNetworkViewFactory;
import org.cytoscape.work.TaskMonitor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.sbml.jsbml.SBMLDocument;
import org.sbml.jsbml.SBase;

/**
 * Tests restoring the cy3sbml mappings from a loaded session.
 */
class SessionDataTest {
    // SUIDs when the session was saved -> SUIDs in the loaded session
    private static final long NETWORK = 1L;
    private static final long NEW_NETWORK = 101L;
    private static final long NODE = 2L;
    private static final long NEW_NODE = 102L;
    private static final long CLONE = 3L;
    private static final long NEW_CLONE = 103L;
    // node deleted after the import, not part of the loaded session
    private static final long STALE_NODE = 4L;

    @TempDir
    Path tempDir;

    private CySession session;

    @BeforeEach
    void setUp() {
        session = mock(CySession.class);
        CyNetwork network = mock(CyNetwork.class);
        when(network.getSUID()).thenReturn(NEW_NETWORK);
        when(session.getObject(NETWORK, CyNetwork.class)).thenReturn(network);
        CyNode node = node(NEW_NODE);
        CyNode clone = node(NEW_CLONE);
        when(session.getObject(NODE, CyNode.class)).thenReturn(node);
        when(session.getObject(CLONE, CyNode.class)).thenReturn(clone);
    }

    private static CyNode node(long suid) {
        CyNode node = mock(CyNode.class);
        when(node.getSUID()).thenReturn(suid);
        return node;
    }

    private File serialize(String name, Serializable object) throws IOException {
        File file = tempDir.resolve(name).toFile();
        try (ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(file))) {
            out.writeObject(object);
        }
        return file;
    }

    private SessionLoadedEvent event(File... files) {
        when(session.getAppFileListMap()).thenReturn(Map.of("cy3sbml", List.of(files)));
        return new SessionLoadedEvent(mock(CySessionManager.class), session, "test.cys");
    }

    private static Network2SBMLMapper sbmlMapperWithStaleNode() {
        One2ManyMapping<String, Long> mapping = new One2ManyMapping<>();
        mapping.put("s1", NODE);
        mapping.put("s2", STALE_NODE);
        Network2SBMLMapper mapper = new Network2SBMLMapper();
        mapper.putDocument(NETWORK, new SBMLDocument(3, 1), mapping);
        return mapper;
    }

    private static Network2CofactorMapper cofactorMapperWithStaleClone() {
        Network2CofactorMapper mapper = new Network2CofactorMapper();
        mapper.put(NETWORK, NODE, CLONE);
        mapper.put(NETWORK, NODE, STALE_NODE);
        return mapper;
    }

    @Test
    void restoresMappingsAndSkipsStaleSUIDs() throws Exception {
        SBMLManager sbmlManager = new SBMLManager(mock(CyApplicationManager.class));
        CofactorManager cofactorManager = new CofactorManager();
        SessionData sessionData = new SessionData(sbmlManager, cofactorManager);

        sessionData.handleEvent(event(
                serialize("Network2SBMLMapper.ser", sbmlMapperWithStaleNode()),
                serialize("Network2Cofactors.ser", cofactorMapperWithStaleClone())));

        Network2SBMLMapper sbmlMapper = sbmlManager.getNetwork2SBMLMapper();
        assertNotNull(sbmlMapper.getDocument(NEW_NETWORK));
        One2ManyMapping<String, Long> nodes = sbmlMapper.getSBase2CyNodeMapping(NEW_NETWORK);
        assertEquals(Set.of("s1"), nodes.keySet());
        assertEquals(Set.of(NEW_NODE), nodes.getValues("s1"));

        One2ManyMapping<Long, Long> clones =
                cofactorManager.getNetwork2CofactorMapper().getCofactor2CloneMapping(NEW_NETWORK);
        assertEquals(Set.of(NEW_CLONE), clones.getValues(NEW_NODE));
    }

    /** Imports the model resource with the SBMLReaderTask into the manager, returns the base network. */
    private static CyNetwork importNetwork(String resource, SBMLManager manager) throws Exception {
        CyNetworkFactory networkFactory = new NetworkTestSupport().getNetworkFactory();
        CyGroupFactory groupFactory = new GroupTestSupport().getGroupFactory();
        CyNetworkViewFactory viewFactory = new NetworkViewTestSupport().getNetworkViewFactory();
        String fileName = resource.substring(resource.lastIndexOf('/') + 1);
        try (InputStream instream = TestUtils.class.getResourceAsStream(resource)) {
            SBMLReaderTask readerTask = new SBMLReaderTask(
                    instream, fileName, null, networkFactory, groupFactory, viewFactory, null, null, null, manager);
            readerTask.run(mock(TaskMonitor.class));
            CyNetwork network = readerTask.getNetworks()[0];
            // registers the SBMLDocument and mapping in the SBMLManager
            readerTask.buildCyNetworkView(network);
            return network;
        }
    }

    /**
     * Saves the session with the real SessionData API and restores it into a fresh
     * SBMLManager, as a loaded session in which the SUIDs are unchanged.
     */
    private static SBMLManager saveAndLoad(SBMLManager manager, CyNetwork network) throws Exception {
        SessionData savingSessionData = new SessionData(manager, new CofactorManager());
        SessionAboutToBeSavedEvent saveEvent = new SessionAboutToBeSavedEvent(mock(CySessionManager.class));
        savingSessionData.saveSessionData(saveEvent);
        List<File> savedFiles = saveEvent.getAppFileListMap().get("cy3sbml");
        assertNotNull(savedFiles);

        // simulate the loaded session: SUIDs are unchanged, objects are looked up by SUID
        CyRootNetwork rootNetwork = ((CySubNetwork) network).getRootNetwork();
        CySession loadedSession = mock(CySession.class);
        when(loadedSession.getObject(rootNetwork.getSUID(), CyNetwork.class)).thenReturn(rootNetwork);
        for (CyNode node : rootNetwork.getNodeList()) {
            when(loadedSession.getObject(node.getSUID(), CyNode.class)).thenReturn(node);
        }
        when(loadedSession.getAppFileListMap()).thenReturn(Map.of("cy3sbml", savedFiles));
        SessionLoadedEvent loadEvent = new SessionLoadedEvent(mock(CySessionManager.class), loadedSession, "test.cys");

        SBMLManager restoredManager = new SBMLManager(mock(CyApplicationManager.class));
        new SessionData(restoredManager, new CofactorManager()).handleEvent(loadEvent);
        return restoredManager;
    }

    /**
     * Reads a real SBML model with the SBMLReaderTask into an SBMLManager, saves the
     * session with the real SessionData API, and restores it into a fresh SBMLManager.
     */
    @Test
    void sessionRoundTripRestoresMapping() throws Exception {
        SBMLManager originalManager = new SBMLManager(mock(CyApplicationManager.class));
        CyNetwork network = importNetwork(SBMLFbcTest.TEST_MODEL_FBC, originalManager);

        Long rootSUID = NetworkUtil.getRootNetworkSUID(network);
        SBMLDocument originalDocument = originalManager.getSBMLDocument(rootSUID);
        assertNotNull(originalDocument);
        String modelId = originalDocument.getModel().getId();
        One2ManyMapping<String, Long> originalMapping = originalManager.getMapping(rootSUID);
        int originalKeyCount = originalMapping.keySet().size();
        int originalValueCount =
                originalMapping.getValues(originalMapping.keySet()).size();
        assertTrue(originalKeyCount > 0);

        SBMLManager restoredManager = saveAndLoad(originalManager, network);

        SBMLDocument restoredDocument = restoredManager.getSBMLDocument(rootSUID);
        assertNotNull(restoredDocument);
        assertEquals(modelId, restoredDocument.getModel().getId());
        One2ManyMapping<String, Long> restoredMapping = restoredManager.getMapping(rootSUID);
        assertEquals(originalKeyCount, restoredMapping.keySet().size());
        assertEquals(
                originalValueCount,
                restoredMapping.getValues(restoredMapping.keySet()).size());
        // the model of the network collection, for the links to comp targets
        assertEquals(Optional.of(rootSUID), restoredManager.rootNetwork(restoredDocument.getModel()));
    }

    /** The summaries of the uncertainties of the elements of the model, in document order. */
    private static List<String> uncertainties(SBMLDocument document) {
        return document.getModel().filter(o -> o instanceof SBase).stream()
                .map(node -> DistribUtil.summary(DistribUtil.uncertainties((SBase) node)))
                .filter(summary -> !summary.isEmpty())
                .toList();
    }

    @Test
    void sessionRoundTripKeepsUncertainties() throws Exception {
        SBMLManager originalManager = new SBMLManager(mock(CyApplicationManager.class));
        CyNetwork network = importNetwork(SBMLDistribTest.TEST_MODEL_DISTRIB, originalManager);
        Long rootSUID = NetworkUtil.getRootNetworkSUID(network);
        List<String> original = uncertainties(originalManager.getSBMLDocument(rootSUID));

        SBMLManager restoredManager = saveAndLoad(originalManager, network);

        assertEquals(10, original.size());
        assertEquals(original, uncertainties(restoredManager.getSBMLDocument(rootSUID)));
    }

    @Test
    void failureInOneFileStillRestoresTheOthers() throws Exception {
        SBMLManager sbmlManager = mock(SBMLManager.class);
        doThrow(new IllegalStateException("SBMLManager failure"))
                .when(sbmlManager)
                .setSBML2NetworkMapper(any());
        CofactorManager cofactorManager = new CofactorManager();
        SessionData sessionData = new SessionData(sbmlManager, cofactorManager);

        sessionData.handleEvent(event(
                serialize("Network2SBMLMapper.ser", sbmlMapperWithStaleNode()),
                serialize("Network2Cofactors.ser", cofactorMapperWithStaleClone())));

        One2ManyMapping<Long, Long> clones =
                cofactorManager.getNetwork2CofactorMapper().getCofactor2CloneMapping(NEW_NETWORK);
        assertEquals(Set.of(NEW_CLONE), clones.getValues(NEW_NODE));
    }
}
