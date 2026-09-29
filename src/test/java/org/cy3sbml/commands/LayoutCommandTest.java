package org.cy3sbml.commands;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import java.nio.file.Path;
import org.cy3sbml.SBML;
import org.cy3sbml.util.AttributeUtil;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNode;
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
