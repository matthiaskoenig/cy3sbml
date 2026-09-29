package org.cy3sbml.gui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.mockito.Mockito.mock;

import java.io.InputStream;
import java.util.Arrays;
import org.cy3sbml.SBML;
import org.cy3sbml.SBMLManager;
import org.cy3sbml.reader.SBMLReaderTask;
import org.cytoscape.application.CyApplicationManager;
import org.cytoscape.ding.NetworkViewTestSupport;
import org.cytoscape.group.GroupTestSupport;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNode;
import org.cytoscape.model.NetworkTestSupport;
import org.cytoscape.work.TaskMonitor;
import org.junit.jupiter.api.Test;
import org.sbml.jsbml.SBase;
import org.sbml.jsbml.Species;
import org.sbml.jsbml.ext.layout.SpeciesGlyph;

/**
 * The info panel of a node of a layout network (#71): an alias shows the element of its
 * glyph, a glyph without element in the model shows the glyph.
 */
class LayoutNetworkPanelTest {

    @Test
    void aliasShowsItsSpecies() throws Exception {
        assertEquals("A", ((Species) selected("sg_A2")).getId());
    }

    @Test
    void glyphOfAnUnknownSpeciesShowsTheGlyph() throws Exception {
        SBase sbase = selected("sg_X");

        assertInstanceOf(SpeciesGlyph.class, sbase);
        assertEquals("sg_X", ((SpeciesGlyph) sbase).getId());
    }

    /** The target of the info panel when the node of the glyph in the first layout is selected. */
    private static SBase selected(String glyph) throws Exception {
        SBMLManager sbmlManager = new SBMLManager(mock(CyApplicationManager.class));
        SBMLReaderTask task;
        try (InputStream stream = LayoutNetworkPanelTest.class.getResourceAsStream("/models/unittests/layout_02.xml")) {
            task = new SBMLReaderTask(
                    stream,
                    "layout_02.xml",
                    null,
                    new NetworkTestSupport().getNetworkFactory(),
                    new GroupTestSupport().getGroupFactory(),
                    new NetworkViewTestSupport().getNetworkViewFactory(),
                    null,
                    null,
                    null,
                    sbmlManager);
            task.run(mock(TaskMonitor.class));
        }
        CyNetwork layout = Arrays.stream(task.getNetworks())
                .filter(n -> "layout_02__layout_layout1".equals(n.getRow(n).get(CyNetwork.NAME, String.class)))
                .findFirst()
                .orElseThrow();
        task.buildCyNetworkView(layout);
        CyNode node = layout.getNodeList().stream()
                .filter(n -> glyph.equals(layout.getRow(n).get(SBML.ATTR_LAYOUT_GLYPH, String.class)))
                .findFirst()
                .orElseThrow();
        layout.getRow(node).set(CyNetwork.SELECTED, true);
        return (SBase) PanelUpdater.resolveTarget(layout, sbmlManager);
    }
}
