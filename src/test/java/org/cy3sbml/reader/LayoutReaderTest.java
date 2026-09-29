package org.cy3sbml.reader;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.cytoscape.model.CyNetwork;
import org.junit.jupiter.api.Test;
import org.sbml.jsbml.ext.layout.Layout;

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

    @Test
    void layoutsAreRegisteredInTheContext() throws Exception {
        ConversionContext context = ReaderTestSupport.read("layout_02.xml", new CoreReader(), new LayoutReader());

        assertEquals(
                List.of("layout1", "layout2"),
                context.layouts().stream().map(Layout::getId).toList());
    }

    @Test
    void modelWithoutLayoutRegistersNone() throws Exception {
        ConversionContext context = ReaderTestSupport.read("core_01.xml", new CoreReader(), new LayoutReader());

        assertTrue(context.layouts().isEmpty());
    }
}
