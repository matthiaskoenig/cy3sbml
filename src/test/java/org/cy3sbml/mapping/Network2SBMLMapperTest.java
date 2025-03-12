package org.cy3sbml.mapping;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.sbml.jsbml.SBMLDocument;

import java.util.Map;
import java.util.Set;

import static org.junit.Assert.*;

/**
 * Testing Network2SBMLMapper.
 */
public class Network2SBMLMapperTest {

    private Network2SBMLMapper mapper;
    private static final Long SUID = Long.valueOf(123);
    private static final One2ManyMapping<String, Long> MAPPING = new One2ManyMapping<>();
    private static final SBMLDocument DOC = new SBMLDocument();

    @Before
    public void setUp(){
        mapper = new Network2SBMLMapper();
    }

    @After
    public void tearDown(){
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

}