package org.cy3sbml.layout;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.File;
import java.nio.file.Path;
import org.cy3sbml.SBML;
import org.cy3sbml.util.AttributeUtil;
import org.cytoscape.ding.NetworkViewTestSupport;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNetworkFactory;
import org.cytoscape.model.CyNode;
import org.cytoscape.model.NetworkTestSupport;
import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.view.model.CyNetworkViewFactory;
import org.cytoscape.view.model.View;
import org.cytoscape.view.presentation.property.BasicVisualLexicon;
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
    void savingAndReloadingRestoresTheOriginalNodePositions() {
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

        LayoutTools layoutTools = new LayoutTools(null);
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
}
