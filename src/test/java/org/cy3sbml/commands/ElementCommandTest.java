package org.cy3sbml.commands;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;
import org.cy3sbml.SBML;
import org.cy3sbml.util.AttributeUtil;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNode;
import org.junit.jupiter.api.Test;

class ElementCommandTest {

    @Test
    void returnsTheElementOfAnSbmlId() throws Exception {
        CommandTestSupport support = new CommandTestSupport();
        List<CyNetwork> networks = support.importModel(CommandTestSupport.CORE_MODEL);
        ElementCommand.ElementTask task = new ElementCommand.ElementTask(support.services());
        task.sbmlId = "BLL";

        JsonNode json = CommandTestSupport.run(task);

        JsonNode element = json.get("elements").get(0);
        assertEquals("Species", element.get("class").asText());
        assertEquals("BLL", element.get("id").asText());
        assertEquals("BasalACh2", element.get("name").asText());
        assertEquals("_000003", element.get("metaId").asText());
        assertEquals("SBO:0000297", element.get("sboTerm").asText());
        JsonNode cvTerm = element.get("cvTerms").get(0);
        assertEquals("BQB_IS_VERSION_OF", cvTerm.get("qualifier").asText());
        assertEquals(
                "http://identifiers.org/interpro/IPR002394",
                cvTerm.get("resources").get(0).asText());
        assertTrue(element.get("notes").asText().contains("<notes>"), element.toPrettyString());
        CyNode node = AttributeUtil.getNodeByAttribute(networks.get(0), SBML.ATTR_ID, "BLL");
        assertEquals(node.getSUID(), element.get("nodes").get(0).asLong());
    }

    @Test
    void returnsTheElementOfAMetaId() throws Exception {
        CommandTestSupport support = new CommandTestSupport();
        support.importModel(CommandTestSupport.CORE_MODEL);
        ElementCommand.ElementTask task = new ElementCommand.ElementTask(support.services());
        task.metaId = "_000003";

        JsonNode json = CommandTestSupport.run(task);

        assertEquals("BLL", json.get("elements").get(0).get("id").asText());
    }

    @Test
    void returnsTheElementsOfNodes() throws Exception {
        CommandTestSupport support = new CommandTestSupport();
        List<CyNetwork> networks = support.importModel(CommandTestSupport.CORE_MODEL);
        CyNetwork network = networks.get(0);
        ElementCommand.ElementTask task = new ElementCommand.ElementTask(support.services());
        task.network = network;
        task.getnodeList()
                .setValue(List.of(
                        AttributeUtil.getNodeByAttribute(network, SBML.ATTR_ID, "BLL"),
                        AttributeUtil.getNodeByAttribute(network, SBML.ATTR_ID, "IL")));

        JsonNode json = CommandTestSupport.run(task);

        assertEquals(2, json.get("elements").size());
        assertEquals("BLL", json.get("elements").get(0).get("id").asText());
        assertEquals("IL", json.get("elements").get(1).get("id").asText());
    }

    @Test
    void failsWithoutOrWithSeveralSelections() throws Exception {
        CommandTestSupport support = new CommandTestSupport();
        support.importModel(CommandTestSupport.CORE_MODEL);
        ElementCommand.ElementTask none = new ElementCommand.ElementTask(support.services());
        ElementCommand.ElementTask two = new ElementCommand.ElementTask(support.services());
        two.sbmlId = "BLL";
        two.metaId = "_000003";

        for (ElementCommand.ElementTask task : List.of(none, two)) {
            IllegalArgumentException error =
                    assertThrows(IllegalArgumentException.class, () -> CommandTestSupport.run(task));
            assertEquals("Give exactly one of the arguments nodeList, sbmlId and metaId.", error.getMessage());
        }
    }

    @Test
    void failsForAnUnknownSbmlId() throws Exception {
        CommandTestSupport support = new CommandTestSupport();
        support.importModel(CommandTestSupport.CORE_MODEL);
        ElementCommand.ElementTask task = new ElementCommand.ElementTask(support.services());
        task.sbmlId = "missing";

        IllegalArgumentException error =
                assertThrows(IllegalArgumentException.class, () -> CommandTestSupport.run(task));

        assertEquals("No element with the SBML id 'missing'.", error.getMessage());
    }
}
