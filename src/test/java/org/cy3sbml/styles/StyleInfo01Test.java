package org.cy3sbml.styles;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;


public class StyleInfo01Test {
    @Test
    public void createMappings() throws Exception {
        StyleInfo_cy3sbml info = new StyleInfo_cy3sbml();
        List<Mapping> mappings = info.createMappings();
        assertNotNull(mappings);
        assertTrue(mappings.size() > 0);
    }
}