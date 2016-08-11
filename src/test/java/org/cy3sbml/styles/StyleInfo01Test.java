package org.cy3sbml.styles;

import org.junit.Test;

import java.util.List;

import static org.junit.Assert.*;

public class StyleInfo01Test {
    @Test
    public void createMappings() throws Exception {
        StyleInfo_cy3sbml info = new StyleInfo_cy3sbml();
        List<Mapping> mappings = info.createMappings();
        assertNotNull(mappings);
        assertTrue(mappings.size() > 0);
    }
}