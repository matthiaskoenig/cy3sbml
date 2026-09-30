package org.cy3sbml.layout;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import org.cy3sbml.SBML;
import org.cy3sbml.reader.SBMLReaderTask;
import org.cy3sbml.util.AttributeUtil;
import org.cytoscape.ding.NetworkViewTestSupport;
import org.cytoscape.group.GroupTestSupport;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNetworkFactory;
import org.cytoscape.model.CyNode;
import org.cytoscape.model.NetworkTestSupport;
import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.view.model.CyNetworkViewFactory;
import org.cytoscape.view.model.View;
import org.cytoscape.view.presentation.property.BasicVisualLexicon;
import org.cytoscape.work.TaskMonitor;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Round trips node positions through {@link LayoutTools}: saves the layout of a small
 * view to an XML file, moves the nodes, reloads the file, and checks the original
 * positions are restored.
 */
class LayoutRoundTripTest {

    @TempDir
    Path tempDir;

    @Test
    void savingAndReloadingRestoresTheOriginalNodePositions() throws Exception {
        CyNetworkFactory networkFactory = new NetworkTestSupport().getNetworkFactory();
        CyNetworkViewFactory viewFactory = new NetworkViewTestSupport().getNetworkViewFactory();

        CyNetwork network = networkFactory.createNetwork();
        CyNode n1 = network.addNode();
        CyNode n2 = network.addNode();
        AttributeUtil.set(network, n1, SBML.ATTR_ID, "n1", String.class);
        AttributeUtil.set(network, n2, SBML.ATTR_ID, "n2", String.class);

        CyNetworkView view = viewFactory.createNetworkView(network);
        View<CyNode> view1 = view.getNodeView(n1);
        View<CyNode> view2 = view.getNodeView(n2);
        view1.setVisualProperty(BasicVisualLexicon.NODE_X_LOCATION, 10.0);
        view1.setVisualProperty(BasicVisualLexicon.NODE_Y_LOCATION, 20.0);
        view2.setVisualProperty(BasicVisualLexicon.NODE_X_LOCATION, 30.0);
        view2.setVisualProperty(BasicVisualLexicon.NODE_Y_LOCATION, 40.0);

        LayoutTools layoutTools = new LayoutTools(null, null);
        File file = tempDir.resolve("layout.xml").toFile();
        layoutTools.saveLayoutOfViewInFile(view, file);

        // move the nodes away from their saved positions
        view1.setVisualProperty(BasicVisualLexicon.NODE_X_LOCATION, 999.0);
        view1.setVisualProperty(BasicVisualLexicon.NODE_Y_LOCATION, 999.0);
        view2.setVisualProperty(BasicVisualLexicon.NODE_X_LOCATION, 888.0);
        view2.setVisualProperty(BasicVisualLexicon.NODE_Y_LOCATION, 888.0);

        layoutTools.loadLayoutForViewFromFile(view, file);

        assertEquals(10.0, view1.getVisualProperty(BasicVisualLexicon.NODE_X_LOCATION));
        assertEquals(20.0, view1.getVisualProperty(BasicVisualLexicon.NODE_Y_LOCATION));
        assertEquals(30.0, view2.getVisualProperty(BasicVisualLexicon.NODE_X_LOCATION));
        assertEquals(40.0, view2.getVisualProperty(BasicVisualLexicon.NODE_Y_LOCATION));
    }

    /**
     * The aliases of a layout network (#71), glyph nodes with the same cyId, get their own
     * positions back.
     */
    @Test
    void aliasesOfALayoutNetworkGetTheirOwnPositions() throws Exception {
        SBMLReaderTask task;
        try (InputStream stream = getClass().getResourceAsStream("/models/unittests/layout_02.xml")) {
            task = new SBMLReaderTask(
                    stream,
                    "layout_02.xml",
                    new NetworkTestSupport().getNetworkFactory(),
                    new GroupTestSupport().getGroupFactory());
            task.run(mock(TaskMonitor.class));
        }
        CyNetwork layout = Arrays.stream(task.getNetworks())
                .filter(n -> "layout_02__layout_layout1".equals(n.getRow(n).get(CyNetwork.NAME, String.class)))
                .findFirst()
                .orElseThrow();
        CyNetworkView view =
                new NetworkViewTestSupport().getNetworkViewFactory().createNetworkView(layout);
        Map<CyNode, Double> positions = new HashMap<>();
        double x = 0;
        for (CyNode node : layout.getNodeList()) {
            x += 10;
            view.getNodeView(node).setVisualProperty(BasicVisualLexicon.NODE_X_LOCATION, x);
            positions.put(node, x);
        }
        LayoutTools layoutTools = new LayoutTools(null, null);
        File file = tempDir.resolve("layout.xml").toFile();
        layoutTools.saveLayoutOfViewInFile(view, file);
        for (CyNode node : layout.getNodeList()) {
            view.getNodeView(node).setVisualProperty(BasicVisualLexicon.NODE_X_LOCATION, -1.0);
        }

        int positioned = layoutTools.loadLayoutForViewFromFile(view, file);

        assertEquals(layout.getNodeCount(), positioned);
        for (CyNode node : layout.getNodeList()) {
            assertEquals(
                    positions.get(node), view.getNodeView(node).getVisualProperty(BasicVisualLexicon.NODE_X_LOCATION));
        }
    }
}
