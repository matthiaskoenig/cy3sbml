package org.cy3sbml.styles;

import org.junit.Test;

import java.util.List;

import static org.junit.Assert.*;


public class StyleInfo02Test {
    @Test
    public void createMappings() throws Exception {
        StyleInfo02 info = new StyleInfo02();
        List<Mapping> mappings = info.createMappings();
        assertNotNull(mappings);
        assertTrue(mappings.size() > 0);
    }

}