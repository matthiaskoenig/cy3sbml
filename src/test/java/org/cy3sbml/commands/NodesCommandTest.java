package org.cy3sbml.commands;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.ArrayList;
import java.util.List;
import org.cy3sbml.SBML;
import org.cy3sbml.util.AttributeUtil;
import org.cytoscape.model.CyNetwork;
import org.junit.jupiter.api.Test;

class NodesCommandTest {

    @Test
    void returnsTheNodesOfTheGivenIdsInTheirOrder() throws Exception {
        CommandTestSupport support = new CommandTestSupport();
        CyNetwork network = support.importModel(CommandTestSupport.CORE_MODEL).get(0);
        NodesCommand.NodesTask task = new NodesCommand.NodesTask(support.services());
        task.sbmlIds = "IL, BLL,missing";

        JsonNode json = CommandTestSupport.run(task);

        JsonNode nodes = json.get("nodes");
        List<String> ids = new ArrayList<>();
        nodes.fieldNames().forEachRemaining(ids::add);
        assertEquals(List.of("IL", "BLL", "missing"), ids);
        assertEquals(
                AttributeUtil.getNodeByAttribute(network, SBML.ATTR_ID, "BLL").getSUID(),
                nodes.get("BLL").get(0).asLong());
        assertEquals(0, nodes.get("missing").size());
    }

    @Test
    void returnsTheNodesOfAllIdsOfTheNetwork() throws Exception {
        CommandTestSupport support = new CommandTestSupport();
        List<CyNetwork> networks = support.importModel(CommandTestSupport.CORE_MODEL);
        NodesCommand.NodesTask task = new NodesCommand.NodesTask(support.services());
        // the all network has the parameters and compartments as well
        task.network = networks.get(2);

        JsonNode json = CommandTestSupport.run(task);

        JsonNode nodes = json.get("nodes");
        assertTrue(nodes.has("BLL"), json.toPrettyString());
        assertTrue(nodes.has("comp1"), json.toPrettyString());
    }
}
