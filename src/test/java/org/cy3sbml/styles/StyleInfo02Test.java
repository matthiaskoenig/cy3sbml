package org.cy3sbml.styles;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;


public class StyleInfo02Test {
    @Test
    public void createMappings() throws Exception {
        StyleInfo_cy3sbmlDark info = new StyleInfo_cy3sbmlDark();
        List<Mapping> mappings = info.createMappings();
        assertNotNull(mappings);
        assertTrue(mappings.size() > 0);
    }

}