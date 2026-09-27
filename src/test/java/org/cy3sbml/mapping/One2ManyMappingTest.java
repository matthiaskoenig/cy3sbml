package org.cy3sbml.mapping;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Testing One2ManyMapping.
 */
public class One2ManyMappingTest {

    One2ManyMapping<String, Long> map;

    @BeforeEach
    public void setUp() {
        map = new One2ManyMapping<String, Long>();
    }

    @AfterEach
    public void tearDown() {
        map = null;
    }

    @Test
    public void testContainsKey() {
        map.put("id1", Long.valueOf(10));
        assertTrue(map.containsKey("id1"));
        assertFalse(map.containsKey("id2"));
    }

    @Test
    public void testKeySet() {
        map.put("id1", Long.valueOf(10));
        map.put("id2", Long.valueOf(20));
        Set<String> keys = map.keySet();
        assertEquals(keys.size(), 2);
        assertTrue(keys.contains("id1"));
        assertTrue(keys.contains("id2"));
    }

    @Test
    public void testPut() {
        map.put("id1", Long.valueOf(10));
        assertTrue(map.containsKey("id1"));
        assertEquals(map.keySet().size(), 1);
    }

    @Test
    public void testRemove() {
        map.put("id1", Long.valueOf(10));
        assertTrue(map.containsKey("id1"));
        map.remove("id1");
        assertFalse(map.containsKey("id1"));
        assertEquals(map.keySet().size(), 0);
    }

    @Test
    public void testGetValues() {
        map.put("id1", Long.valueOf(10));
        map.put("id1", Long.valueOf(20));
        map.put("id1", Long.valueOf(30));
        Set<Long> values = map.getValues("id1");
        assertEquals(values.size(), 3);
        values = map.getValues("id2");
        assertEquals(values.size(), 0);
    }

    @Test
    public void testGetValuesListOf() {
        map.put("id1", Long.valueOf(10));
        map.put("id1", Long.valueOf(20));
        map.put("id1", Long.valueOf(30));
        map.put("id2", Long.valueOf(-10));
        map.put("id2", Long.valueOf(-20));
        map.put("id2", Long.valueOf(-30));

        List<String> keys = new ArrayList<String>();
        keys.add("id1");
        keys.add("id2");

        Set<Long> values = map.getValues(keys);
        assertEquals(values.size(), 6);
    }

    @Test
    public void testCreateReverseMapping() {
        map.put("id1", Long.valueOf(10));
        map.put("id1", Long.valueOf(20));
        One2ManyMapping<Long, String> revMap = map.createReverseMapping();
        assertTrue(revMap.containsKey(Long.valueOf(10)));
        assertTrue(revMap.containsKey(Long.valueOf(20)));
    }

    /**
     * One2ManyMapping is reached from both the Cytoscape event/EDT thread (writers) and the
     * WebViewPanel's background panel-update thread (readers). Many threads put values under
     * the same key concurrently; without the instance-level synchronization on put/getValues
     * this can throw (structural modification of the backing HashSet raced with an iteration
     * inside computeIfAbsent) or lose entries.
     */
    @Test
    public void testConcurrentPutDoesNotLoseValuesOrThrow() throws Exception {
        int threadCount = 16;
        int valuesPerThread = 200;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        try {
            List<Future<?>> futures = new ArrayList<>();
            for (int t = 0; t < threadCount; t++) {
                long base = (long) t * valuesPerThread;
                futures.add(executor.submit(() -> {
                    for (int i = 0; i < valuesPerThread; i++) {
                        map.put("shared", base + i);
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
        assertEquals(threadCount * valuesPerThread, map.getValues("shared").size());
    }
}
