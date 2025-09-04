package org.cy3sbml.styles;

import org.cy3sbml.SBML;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test MappingPasstrough.
 */
public class MappingPassthroughTest {
    private MappingPassthrough m;
    private static final String DEFAULT_VALUE = "default";

    @BeforeEach
    public void setUp(){
        m = new MappingPassthrough(Mapping.DataType.integer,
                VisualPropertyKey.NODE_LABEL,
                SBML.NODETYPE_ATTR,
                DEFAULT_VALUE);
    }

    @AfterEach
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