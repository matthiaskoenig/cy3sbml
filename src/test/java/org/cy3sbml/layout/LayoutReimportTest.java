package org.cy3sbml.layout;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.cy3sbml.SBML;
import org.cy3sbml.TestUtils;
import org.cy3sbml.golden.GoldenModelsTest;
import org.cytoscape.ding.NetworkViewTestSupport;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNode;
import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.view.model.CyNetworkViewFactory;
import org.cytoscape.view.model.View;
import org.cytoscape.view.presentation.property.BasicVisualLexicon;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Saves the layout of every network of a model and loads it into the networks of a new
 * import of the same model, as a user does after changing the model (#228): every node has
 * to get the position of its node in the first import.
 */
class LayoutReimportTest {

    private static final String MODELS_ROOT = "/models/";
    private static final String L1_MODEL = "sbml-test-suite/semantic/00032/00032-sbml-l1v2.xml";
    /**
     * The golden models, except the SBML L1 model: L1 has no metaIds, so its nodes have no
     * cyId and only the nodes with SBML id are positioned (see
     * {@link #layoutOfL1ModelIsRestoredForTheNodesWithSbmlId}).
     */
    static List<String> models() {
        return GoldenModelsTest.MODELS.stream()
                .filter(model -> !model.equals(L1_MODEL))
                .toList();
    }

    @TempDir
    Path tempDir;

    @ParameterizedTest(name = "{0}")
    @MethodSource("models")
    void layoutOfAllNodesIsRestoredInANewImport(String model) throws Exception {
        for (Reimport reimport : reimport(model)) {
            assertEquals(reimport.saved.getModel().getNodeCount(), reimport.positioned, reimport.name());
            // the node order differs between imports: every node has to be at the position of
            // a node of the first import with the same type and label
            assertEquals(placedNodes(reimport.saved), placedNodes(reimport.loaded), reimport.name());
        }
    }

    @Test
    void layoutOfL1ModelIsRestoredForTheNodesWithSbmlId() throws Exception {
        for (Reimport reimport : reimport(L1_MODEL)) {
            List<String> saved = placedNodes(reimport.saved).stream()
                    .filter(node -> !node.startsWith(SBML.NODETYPE_RATE_RULE))
                    .toList();
            List<String> loaded = placedNodes(reimport.loaded).stream()
                    .filter(node -> !node.startsWith(SBML.NODETYPE_RATE_RULE))
                    .toList();
            assertEquals(saved.size(), reimport.positioned, reimport.name());
            assertEquals(saved, loaded, reimport.name());
        }
    }

    /** The saved view of a network of the first import and the loaded view of the second. */
    private record Reimport(CyNetworkView saved, CyNetworkView loaded, int positioned) {
        String name() {
            CyNetwork network = saved.getModel();
            return network.getRow(network).get(CyNetwork.NAME, String.class);
        }
    }

    /**
     * Imports the model twice, gives every node of the first import its own position, saves
     * the layout of every network and loads it into the network of the second import.
     */
    private List<Reimport> reimport(String model) throws Exception {
        CyNetwork[] first = TestUtils.readNetwork(MODELS_ROOT + model);
        CyNetwork[] second = TestUtils.readNetwork(MODELS_ROOT + model);
        assertEquals(first.length, second.length);

        CyNetworkViewFactory viewFactory = new NetworkViewTestSupport().getNetworkViewFactory();
        LayoutTools layoutTools = new LayoutTools(null, null);
        List<Reimport> reimports = new ArrayList<>();
        for (int k = 0; k < first.length; k++) {
            CyNetworkView saved = viewFactory.createNetworkView(first[k]);
            List<CyNode> nodes = first[k].getNodeList();
            for (int i = 0; i < nodes.size(); i++) {
                View<CyNode> nodeView = saved.getNodeView(nodes.get(i));
                nodeView.setVisualProperty(BasicVisualLexicon.NODE_X_LOCATION, i + 1.0);
                nodeView.setVisualProperty(BasicVisualLexicon.NODE_Y_LOCATION, -i - 1.0);
            }
            File file = tempDir.resolve("layout" + k + ".xml").toFile();
            layoutTools.saveLayoutOfViewInFile(saved, file);

            CyNetworkView loaded = viewFactory.createNetworkView(second[k]);
            int positioned = layoutTools.loadLayoutForViewFromFile(loaded, file);
            reimports.add(new Reimport(saved, loaded, positioned));
        }
        return reimports;
    }

    /** Sorted list of the nodes with type, label and position. */
    private static List<String> placedNodes(CyNetworkView view) {
        CyNetwork network = view.getModel();
        List<String> nodes = new ArrayList<>();
        for (CyNode node : network.getNodeList()) {
            View<CyNode> nodeView = view.getNodeView(node);
            nodes.add(label(network, node) + " @ "
                    + nodeView.getVisualProperty(BasicVisualLexicon.NODE_X_LOCATION) + ","
                    + nodeView.getVisualProperty(BasicVisualLexicon.NODE_Y_LOCATION));
        }
        Collections.sort(nodes);
        return nodes;
    }

    private static String label(CyNetwork network, CyNode node) {
        return network.getRow(node).get(SBML.NODETYPE_ATTR, String.class) + ":"
                + network.getRow(node).get(SBML.LABEL, String.class);
    }
}
