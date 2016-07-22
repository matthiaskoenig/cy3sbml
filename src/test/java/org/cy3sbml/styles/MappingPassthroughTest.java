package org.cy3sbml.styles;

import org.cy3sbml.SBML;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Test MappingPasstrough.
 */
public class MappingPassthroughTest {
    private MappingPassthrough m;
    private static final String DEFAULT_VALUE = "default";

    @Before
    public void setUp(){
        m = new MappingPassthrough(Mapping.DataType.integer,
                VisualPropertyKey.NODE_LABEL,
                SBML.NODETYPE_ATTR,
                DEFAULT_VALUE);
    }

    @After
    public void tearDown(){
        m = null;
    }

    @Test
    public void mappingTest(){
        assertEquals(Mapping.DataType.integer, m.getDataType());
        assertEquals(SBML.NODETYPE_ATTR, m.getAttributeName());
        assertEquals(VisualPropertyKey.NODE_LABEL, m.getVisualProperty());
        assertEquals(DEFAULT_VALUE, m.getDefaultValue());
        assertEquals(Mapping.MappingType.PASSTHROUGH, m.getMappingType());
    }

}