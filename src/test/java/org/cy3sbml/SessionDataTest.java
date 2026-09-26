package org.cy3sbml;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.cy3sbml.cofactors.Network2CofactorMapper;
import org.cy3sbml.mapping.Network2SBMLMapper;
import org.cy3sbml.mapping.One2ManyMapping;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNode;
import org.cytoscape.session.CySession;
import org.cytoscape.session.CySessionManager;
import org.cytoscape.session.events.SessionLoadedEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.sbml.jsbml.SBMLDocument;

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
        List<Network2SBMLMapper> sbmlMappers = new ArrayList<>();
        List<Network2CofactorMapper> cofactorMappers = new ArrayList<>();
        SessionData sessionData = new SessionData(sbmlMappers::add, cofactorMappers::add);

        sessionData.handleEvent(event(
                serialize("Network2SBMLMapper.ser", sbmlMapperWithStaleNode()),
                serialize("Network2Cofactors.ser", cofactorMapperWithStaleClone())));

        assertEquals(1, sbmlMappers.size());
        Network2SBMLMapper sbmlMapper = sbmlMappers.get(0);
        assertNotNull(sbmlMapper.getDocument(NEW_NETWORK));
        One2ManyMapping<String, Long> nodes = sbmlMapper.getSBase2CyNodeMapping(NEW_NETWORK);
        assertEquals(Set.of("s1"), nodes.keySet());
        assertEquals(Set.of(NEW_NODE), nodes.getValues("s1"));

        assertEquals(1, cofactorMappers.size());
        One2ManyMapping<Long, Long> clones = cofactorMappers.get(0).getCofactor2CloneMapping(NEW_NETWORK);
        assertEquals(Set.of(NEW_CLONE), clones.getValues(NEW_NODE));
    }

    @Test
    void failureInOneFileStillRestoresTheOthers() throws Exception {
        List<Network2CofactorMapper> cofactorMappers = new ArrayList<>();
        SessionData sessionData = new SessionData(
                mapper -> {
                    throw new IllegalStateException("SBMLManager failure");
                },
                cofactorMappers::add);

        sessionData.handleEvent(event(
                serialize("Network2SBMLMapper.ser", sbmlMapperWithStaleNode()),
                serialize("Network2Cofactors.ser", cofactorMapperWithStaleClone())));

        assertEquals(1, cofactorMappers.size());
    }
}
