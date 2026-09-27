package org.cy3sbml.styles;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

public class StyleInfo01Test {
    @Test
    public void createMappings() throws Exception {
        StyleInfo_cy3sbml info = new StyleInfo_cy3sbml();
        List<Mapping> mappings = info.createMappings();
        assertNotNull(mappings);
        assertTrue(mappings.size() > 0);
    }
}
