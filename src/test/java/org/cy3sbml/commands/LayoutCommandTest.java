package org.cy3sbml.commands;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.cy3sbml.SBML;
import org.cy3sbml.cofactors.CofactorManager;
import org.cy3sbml.util.AttributeUtil;
import org.cytoscape.model.CyEdge;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNode;
import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.view.model.View;
import org.cytoscape.view.presentation.property.BasicVisualLexicon;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class LayoutCommandTest {
    @TempDir
    Path tempDir;

    private static LayoutCommand.LayoutTask task(LayoutCommand command, CyNetwork network, Path file) {
        LayoutCommand.LayoutTask task =
                (LayoutCommand.LayoutTask) command.createTaskIterator().next();
        task.network = network;
        task.file = file.toString();
        return task;
    }

    @Test
    void savesAndLoadsTheNodePositions() throws Exception {
        CommandTestSupport support = new CommandTestSupport();
        CyNetwork network = support.importModel(CommandTestSupport.CORE_MODEL).get(0);
        CyNode node = AttributeUtil.getNodeByAttribute(network, SBML.ATTR_ID, "BLL");
        View<CyNode> nodeView = support.view(network).getNodeView(node);
        nodeView.setVisualProperty(BasicVisualLexicon.NODE_X_LOCATION, 12.0);
        Path file = tempDir.resolve("layout.xml");

        JsonNode saved = CommandTestSupport.run(task(LayoutCommand.save(support.services()), network, file));

        assertEquals(file.toAbsolutePath().toString(), saved.get("file").asText());
        assertEquals(network.getNodeCount(), saved.get("nodes").asInt());

        nodeView.setVisualProperty(BasicVisualLexicon.NODE_X_LOCATION, 500.0);
        JsonNode loaded = CommandTestSupport.run(task(LayoutCommand.load(support.services()), network, file));

        assertEquals(network.getNodeCount(), loaded.get("nodes").asInt());
        assertEquals(12.0, nodeView.getVisualProperty(BasicVisualLexicon.NODE_X_LOCATION));
    }

    /**
     * The clones of a split cofactor node have the cyId of their node: they are not saved,
     * and placed next to their neighbors after loading instead of on top of each other.
     */
    @Test
    void savesAndLoadsTheLayoutOfASplitNetwork() throws Exception {
        CommandTestSupport support = new CommandTestSupport();
        CyNetwork network = support.importModel(CommandTestSupport.CORE_MODEL).get(0);
        CyNode node = AttributeUtil.getNodeByAttribute(network, SBML.ATTR_ID, "BLL");
        List<CyNode> clones = support.cofactorManager.split(network, support.view(network), List.of(node));
        assertTrue(clones.size() > 1);
        CyNetworkView view = support.newView(network);
        int k = 0;
        for (View<CyNode> nodeView : view.getNodeViews()) {
            nodeView.setVisualProperty(BasicVisualLexicon.NODE_X_LOCATION, 100.0 * k);
            nodeView.setVisualProperty(BasicVisualLexicon.NODE_Y_LOCATION, 30.0 * k++);
        }
        Path file = tempDir.resolve("layout.xml");

        JsonNode saved = CommandTestSupport.run(task(LayoutCommand.save(support.services()), network, file));
        assertEquals(network.getNodeCount() - clones.size(), saved.get("nodes").asInt());

        for (View<CyNode> nodeView : view.getNodeViews()) {
            nodeView.setVisualProperty(BasicVisualLexicon.NODE_X_LOCATION, 500.0);
            nodeView.setVisualProperty(BasicVisualLexicon.NODE_Y_LOCATION, 500.0);
        }
        CommandTestSupport.run(task(LayoutCommand.load(support.services()), network, file));

        Set<String> positions = new HashSet<>();
        for (CyNode clone : clones) {
            View<CyNode> cloneView = view.getNodeView(clone);
            View<CyNode> neighborView = view.getNodeView(
                    network.getNeighborList(clone, CyEdge.Type.ANY).get(0));
            double dx = cloneView.getVisualProperty(BasicVisualLexicon.NODE_X_LOCATION)
                    - neighborView.getVisualProperty(BasicVisualLexicon.NODE_X_LOCATION);
            double dy = cloneView.getVisualProperty(BasicVisualLexicon.NODE_Y_LOCATION)
                    - neighborView.getVisualProperty(BasicVisualLexicon.NODE_Y_LOCATION);
            assertEquals(CofactorManager.CLONE_DISTANCE, Math.hypot(dx, dy), 1e-6);
            positions.add(cloneView.getVisualProperty(BasicVisualLexicon.NODE_X_LOCATION) + ","
                    + cloneView.getVisualProperty(BasicVisualLexicon.NODE_Y_LOCATION));
        }
        assertEquals(clones.size(), positions.size());
    }

    @Test
    void failsWithoutFileOrView() throws Exception {
        CommandTestSupport support = new CommandTestSupport();
        CyNetwork network = support.importModel(CommandTestSupport.CORE_MODEL).get(0);
        LayoutCommand.LayoutTask noFile = task(LayoutCommand.save(support.services()), network, tempDir);
        noFile.file = null;
        assertEquals(
                "Give the layout file with the argument file.",
                assertThrows(IllegalArgumentException.class, () -> CommandTestSupport.run(noFile))
                        .getMessage());

        LayoutCommand.LayoutTask missing =
                task(LayoutCommand.load(support.services()), network, tempDir.resolve("missing.xml"));
        assertTrue(assertThrows(IllegalArgumentException.class, () -> CommandTestSupport.run(missing))
                .getMessage()
                .endsWith("does not exist."));
    }
}
