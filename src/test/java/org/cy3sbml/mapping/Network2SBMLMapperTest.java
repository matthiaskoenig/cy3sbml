package org.cy3sbml.mapping;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.sbml.jsbml.SBMLDocument;

/**
 * Testing Network2SBMLMapper.
 */
public class Network2SBMLMapperTest {

    private Network2SBMLMapper mapper;
    private static final Long SUID = Long.valueOf(123);
    private static final One2ManyMapping<String, Long> MAPPING = new One2ManyMapping<>();
    private static final SBMLDocument DOC = new SBMLDocument();

    @BeforeEach
    public void setUp() {
        mapper = new Network2SBMLMapper();
    }

    @AfterEach
    public void tearDown() {
        mapper = null;
    }

    @Test
    public void getDocumentForSUID() throws Exception {
        mapper.putDocument(SUID, DOC, MAPPING);
        assertTrue(mapper.containsDocument(SUID));
        SBMLDocument doc = mapper.getDocument(SUID);
        assertEquals(DOC, doc);
    }

    @Test
    public void containsNetwork() throws Exception {
        mapper.putDocument(SUID, DOC, MAPPING);
        assertTrue(mapper.containsDocument(SUID));
    }

    @Test
    public void keySet() throws Exception {
        mapper.putDocument(SUID, DOC, MAPPING);
        assertTrue(mapper.containsDocument(SUID));
        Set<Long> keySet = mapper.keySet();
        assertNotNull(keySet);
        assertEquals(1, keySet.size());
        assertTrue(keySet.contains(SUID));
    }

    @Test
    public void getDocumentMap() throws Exception {
        mapper.putDocument(SUID, DOC, MAPPING);
        Map<Long, SBMLDocument> docMap = mapper.getDocumentMap();
        assertNotNull(docMap);
        assertEquals(1, docMap.size());
        assertTrue(docMap.containsKey(SUID));
        assertTrue(docMap.containsValue(DOC));
    }

    @Test
    public void putDocument() throws Exception {
        mapper.putDocument(SUID, DOC, MAPPING);
        assertTrue(mapper.containsDocument(SUID));
    }

    @Test
    public void removeDocument() throws Exception {
        mapper.putDocument(SUID, DOC, MAPPING);
        assertTrue(mapper.containsDocument(SUID));
        mapper.removeDocument(SUID);
        assertFalse(mapper.containsDocument(SUID));
        assertEquals(0, mapper.keySet().size());
    }

    @Test
    public void getSBase2CyNodeMapping() throws Exception {
        mapper.putDocument(SUID, DOC, MAPPING);
        assertNotNull(mapper.getSBase2CyNodeMapping(SUID));
    }

    @Test
    public void getCyNode2SBaseMapping() throws Exception {
        mapper.putDocument(SUID, DOC, MAPPING);
        assertNotNull(mapper.getCyNode2SBaseMapping(SUID));
    }

    /**
     * Network2SBMLMapper is reached from both the Cytoscape event/EDT thread
     * (put/removeDocument, via SBMLManager) and the WebViewPanel's background
     * panel-update thread (getDocument/keySet/getDocumentMap, also via SBMLManager).
     * Many threads each put, read and remove their own root SUID concurrently; without
     * synchronized methods and defensive copies from keySet()/getDocumentMap() this can
     * throw (e.g. ConcurrentModificationException while another thread mutates the map) or
     * leave stale/lost entries behind.
     */
    @Test
    public void concurrentPutGetRemoveFromManyThreadsDoesNotLoseEntriesOrThrow() throws Exception {
        int threadCount = 16;
        int opsPerThread = 100;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        try {
            List<Future<?>> futures = new ArrayList<>();
            for (int t = 0; t < threadCount; t++) {
                long base = (long) t * opsPerThread;
                futures.add(executor.submit(() -> {
                    for (int i = 0; i < opsPerThread; i++) {
                        Long suid = base + i;
                        One2ManyMapping<String, Long> mapping = new One2ManyMapping<>();
                        mapping.put("s" + suid, suid);
                        mapper.putDocument(suid, new SBMLDocument(), mapping);
                        assertNotNull(mapper.getDocument(suid));
                        // exercise the defensive-copy reads concurrently with other threads' writes
                        mapper.keySet();
                        mapper.getDocumentMap();
                        assertNotNull(mapper.getSBase2CyNodeMapping(suid));
                        mapper.removeDocument(suid);
                        assertFalse(mapper.containsDocument(suid));
                    }
                }));
            }
            for (Future<?> future : futures) {
                // propagates any exception raised inside the task
                future.get();
            }
        } finally {
            executor.shutdown();
        }
        // every put was matched by a remove for the same (unique) SUID
        assertEquals(0, mapper.keySet().size());
    }

    /** A session is saved with a consistent snapshot: the serialization holds the mapper lock. */
    @Test
    public void serializationWaitsForAConcurrentWriter() throws Exception {
        final Network2SBMLMapper lockedMapper = mapper;
        lockedMapper.putDocument(SUID, DOC, new One2ManyMapping<>());
        ExecutorService executor = Executors.newSingleThreadExecutor();
        try {
            Future<?> serialization;
            synchronized (lockedMapper) {
                serialization = executor.submit(() -> {
                    try (java.io.ObjectOutputStream out =
                            new java.io.ObjectOutputStream(new java.io.ByteArrayOutputStream())) {
                        out.writeObject(lockedMapper);
                    }
                    return null;
                });
                Thread.sleep(300);
                assertFalse(serialization.isDone());
            }
            serialization.get();
        } finally {
            executor.shutdownNow();
        }
    }
}
