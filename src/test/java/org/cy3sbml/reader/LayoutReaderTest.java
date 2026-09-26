package org.cy3sbml.reader;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.cytoscape.model.CyNetwork;
import org.junit.jupiter.api.Test;

class LayoutReaderTest {

    @Test
    void layoutsDoNotChangeTheNetwork() throws Exception {
        CyNetwork withoutLayout = ReaderTestSupport.read("layout_01.xml", new CoreReader(), new QualReader())
                .network();
        CyNetwork withLayout = ReaderTestSupport.read(
                        "layout_01.xml", new CoreReader(), new QualReader(), new LayoutReader())
                .network();

        assertEquals(withoutLayout.getNodeCount(), withLayout.getNodeCount());
        assertEquals(withoutLayout.getEdgeCount(), withLayout.getEdgeCount());
    }
}
