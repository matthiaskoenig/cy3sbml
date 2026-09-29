package org.cy3sbml.commands;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;
import org.cytoscape.model.CyNetwork;
import org.junit.jupiter.api.Test;

class NetworksCommandTest {

    @Test
    void listsTheModelsWithTheirNetworks() throws Exception {
        CommandTestSupport support = new CommandTestSupport();
        List<CyNetwork> networks = support.importModel(CommandTestSupport.CORE_MODEL);
        support.addOtherNetwork("not sbml");

        JsonNode json = CommandTestSupport.run(new NetworksCommand.NetworksTask(support.services()));

        JsonNode models = json.get("models");
        assertEquals(1, models.size(), json.toPrettyString());
        JsonNode model = models.get(0);
        assertEquals("BIOMD0000000001", model.get("modelId").asText());
        assertEquals("Edelstein1996 - EPSP ACh event", model.get("modelName").asText());
        assertEquals(2, model.get("level").asInt());
        assertEquals(4, model.get("version").asInt());
        assertEquals(0, model.get("packages").size());
        JsonNode networksJson = model.get("networks");
        assertEquals(networks.size(), networksJson.size(), json.toPrettyString());
        assertEquals("base", networksJson.get(0).get("type").asText());
        assertEquals(networks.get(0).getSUID(), networksJson.get(0).get("suid").asLong());
        assertEquals(
                List.of("base", "kinetic", "all"),
                List.of(
                        networksJson.get(0).get("type").asText(),
                        networksJson.get(1).get("type").asText(),
                        networksJson.get(2).get("type").asText()));
    }

    @Test
    void listsThePackagesWithTheirVersions() throws Exception {
        CommandTestSupport support = new CommandTestSupport();
        support.importModel("/models/fbc/fbc_v3_example_L3V1_fbcV3.xml");

        JsonNode json = CommandTestSupport.run(new NetworksCommand.NetworksTask(support.services()));

        assertEquals(3, json.get("models").get(0).get("packages").get("fbc").asInt(), json.toPrettyString());
    }

    @Test
    void noModelsWithoutSbmlNetworks() throws Exception {
        CommandTestSupport support = new CommandTestSupport();
        support.addOtherNetwork("not sbml");

        JsonNode json = CommandTestSupport.run(new NetworksCommand.NetworksTask(support.services()));

        assertEquals(0, json.get("models").size());
    }
}
