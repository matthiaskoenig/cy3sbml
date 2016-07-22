package org.cy3sbml.styles;

import org.cy3sbml.SBML;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;

/**
 * Test MappingDiscrete.
 */
public class MappingDiscreteTest {
    private MappingDiscrete m;
    private static final String DEFAULT_VALUE = "default";
    private static final String KEY = "key1";
    private static final String VALUE = "value1";

    @Before
    public void setUp(){
        Map<String, String> map = new HashMap<>();
        map.put(KEY, VALUE);
        m = new MappingDiscrete(Mapping.DataType.integer,
                VisualPropertyKey.NODE_LABEL,
                SBML.NODETYPE_ATTR,
                DEFAULT_VALUE, map);
    }

    @After
    public void tearDown(){
        m = null;
    }

    @Test
    public void mappingTest(){
        Map<String, String> map = m.getMap();
        assertNotNull(map);
        assertEquals(Mapping.DataType.integer, m.getDataType());
        assertEquals(SBML.NODETYPE_ATTR, m.getAttributeName());
        assertEquals(VisualPropertyKey.NODE_LABEL, m.getVisualProperty());
        assertEquals(DEFAULT_VALUE, m.getDefaultValue());
        assertEquals(Mapping.MappingType.DISCRETE, m.getMappingType());
    }

    @Test
    public void getMap() throws Exception {
        Map<String, String> map = m.getMap();
        assertNotNull(map);
        assertTrue(map.containsKey(KEY));
        assertTrue(map.containsValue(VALUE));
        assertEquals(1, map.size());
    }

}