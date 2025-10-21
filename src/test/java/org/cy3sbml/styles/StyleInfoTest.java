package org.cy3sbml.styles;


import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;


/**
 * Test StyleInfo.
 */
public class StyleInfoTest {
    private StyleInfo s;

    @BeforeEach
    public void setUp() {
        s = new StyleInfo_cy3sbml();
    }

    @AfterEach
    public void tearDown() {
        s = null;
    }

    @Test
    public void getTemplate() throws Exception {
        String template = s.getTemplate();
        assertNotNull(template);
        assertTrue(template.contains("cy3sbml"));
    }

    @Test
    public void getMappings() throws Exception {
        List<Mapping> mappings = s.getMappings();
        assertNotNull(mappings);
        assertTrue(mappings.size() > 0);
    }

    @Test
    public void getName() throws Exception {
        String name = s.getName();
        assertNotNull(name);
        assertEquals("cy3sbml", name);
    }

    @Test
    public void setMappings() throws Exception {
        s.setMappings(null);
        assertNull(s.getMappings());
    }

    @Test
    public void createMappings() throws Exception {
        List<Mapping> mappings = s.createMappings();
        assertNotNull(mappings);
        assertTrue(mappings.size() > 0);
    }

}