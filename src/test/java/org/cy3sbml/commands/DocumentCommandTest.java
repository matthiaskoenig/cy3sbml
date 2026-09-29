package org.cy3sbml.commands;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.JsonNode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.cytoscape.model.CyNetwork;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.sbml.jsbml.SBMLDocument;
import org.sbml.jsbml.SBMLReader;

class DocumentCommandTest {
    @TempDir
    Path tempDir;

    @Test
    void returnsTheSbmlOfTheCurrentNetwork() throws Exception {
        CommandTestSupport support = new CommandTestSupport();
        support.importModel(CommandTestSupport.CORE_MODEL);

        JsonNode json = CommandTestSupport.run(new DocumentCommand.DocumentTask(support.services()));

        SBMLDocument document = SBMLReader.read(json.get("sbml").asText());
        assertEquals("BIOMD0000000001", document.getModel().getId());
    }

    @Test
    void writesTheSbmlOfAGivenNetworkToAFile() throws Exception {
        CommandTestSupport support = new CommandTestSupport();
        List<CyNetwork> networks = support.importModel(CommandTestSupport.CORE_MODEL);
        DocumentCommand.DocumentTask task = new DocumentCommand.DocumentTask(support.services());
        // the kinetic network has the document of the model as well
        task.network = networks.get(1);
        Path file = tempDir.resolve("model.xml");
        task.file = file.toString();

        JsonNode json = CommandTestSupport.run(task);

        assertEquals(file.toAbsolutePath().toString(), json.get("file").asText());
        assertTrue(Files.readString(file).contains("BIOMD0000000001"));
    }

    @Test
    void failsForANetworkWhichIsNoSbmlNetwork() {
        CommandTestSupport support = new CommandTestSupport();
        DocumentCommand.DocumentTask task = new DocumentCommand.DocumentTask(support.services());
        task.network = support.addOtherNetwork("other");

        IllegalArgumentException error =
                assertThrows(IllegalArgumentException.class, () -> CommandTestSupport.run(task));

        assertTrue(error.getMessage().contains("is not an SBML network of cy3sbml"), error.getMessage());
    }

    @Test
    void failsWithoutNetwork() {
        CommandTestSupport support = new CommandTestSupport();
        when(support.applicationManager.getCurrentNetwork()).thenReturn(null);

        IllegalArgumentException error = assertThrows(
                IllegalArgumentException.class,
                () -> CommandTestSupport.run(new DocumentCommand.DocumentTask(support.services())));

        assertTrue(error.getMessage().startsWith("No network"), error.getMessage());
    }
}
