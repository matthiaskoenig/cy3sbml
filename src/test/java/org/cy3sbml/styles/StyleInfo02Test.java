package org.cy3sbml.styles;

import org.junit.Test;

import java.util.List;

import static org.junit.Assert.*;


public class StyleInfo02Test {
    @Test
    public void createMappings() throws Exception {
        StyleInfo_cy3sbmlDark info = new StyleInfo_cy3sbmlDark();
        List<Mapping> mappings = info.createMappings();
        assertNotNull(mappings);
        assertTrue(mappings.size() > 0);
    }

}