package org.cy3sbml.commands;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;
import org.cy3sbml.SBML;
import org.cy3sbml.util.AttributeUtil;
import org.cytoscape.model.CyEdge;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNode;
import org.junit.jupiter.api.Test;

class CofactorsCommandTest {

    @Test
    void splitsANodeAndMergesAllClonesBack() throws Exception {
        CommandTestSupport support = new CommandTestSupport();
        CyNetwork network = support.importModel(CommandTestSupport.CORE_MODEL).get(0);
        CyNode node = AttributeUtil.getNodeByAttribute(network, SBML.ATTR_ID, "BLL");
        int edges = network.getAdjacentEdgeList(node, CyEdge.Type.ANY).size();
        assertTrue(edges > 1);
        int nodeCount = network.getNodeCount();

        CofactorsCommand.CofactorsTask split = (CofactorsCommand.CofactorsTask)
                CofactorsCommand.split(support.services()).createTaskIterator().next();
        split.network = network;
        split.getnodeList().setValue(List.of(node));
        JsonNode json = CommandTestSupport.run(split);

        assertEquals(edges, json.get("clones").size(), json.toPrettyString());
        assertFalse(network.containsNode(node));
        assertTrue(support.cofactorManager.hasClones(network));

        // merge without nodes: all split nodes of the network
        CofactorsCommand.CofactorsTask merge = (CofactorsCommand.CofactorsTask)
                CofactorsCommand.merge(support.services()).createTaskIterator().next();
        merge.network = network;
        json = CommandTestSupport.run(merge);

        assertEquals(List.of(node.getSUID()), List.of(json.get("merged").get(0).asLong()));
        assertTrue(network.containsNode(node));
        assertEquals(nodeCount, network.getNodeCount());
    }

    @Test
    void splitFailsWithoutNodes() throws Exception {
        CommandTestSupport support = new CommandTestSupport();
        support.importModel(CommandTestSupport.CORE_MODEL);
        CofactorsCommand.CofactorsTask split = (CofactorsCommand.CofactorsTask)
                CofactorsCommand.split(support.services()).createTaskIterator().next();

        IllegalArgumentException error =
                assertThrows(IllegalArgumentException.class, () -> CommandTestSupport.run(split));

        assertEquals("Give the nodes to split with the argument nodeList.", error.getMessage());
    }
}
