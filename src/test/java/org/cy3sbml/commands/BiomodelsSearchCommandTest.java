package org.cy3sbml.commands;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.JsonNode;
import java.io.IOException;
import java.util.List;
import org.cy3sbml.biomodel.BiomodelSummary;
import org.cy3sbml.biomodel.BiomodelsSearchResult;
import org.junit.jupiter.api.Test;

class BiomodelsSearchCommandTest {

    @Test
    void returnsTheMatchingModels() throws Exception {
        CommandTestSupport support = new CommandTestSupport();
        when(support.biomodelsQuery.search("repressilator"))
                .thenReturn(new BiomodelsSearchResult(
                        1,
                        List.of(new BiomodelSummary(
                                "BIOMD0000000012", "Elowitz2000 - Repressilator", "2005-02-08", "2012-07-05"))));
        BiomodelsSearchCommand.BiomodelsSearchTask task =
                new BiomodelsSearchCommand.BiomodelsSearchTask(support.services());
        task.query = " repressilator ";

        JsonNode json = CommandTestSupport.run(task);

        assertEquals(1, json.get("matches").asInt());
        JsonNode model = json.get("models").get(0);
        assertEquals("BIOMD0000000012", model.get("id").asText());
        assertEquals("Elowitz2000 - Repressilator", model.get("name").asText());
        assertEquals("2005-02-08", model.get("submissionDate").asText());
        assertEquals("2012-07-05", model.get("lastModified").asText());
    }

    @Test
    void failsWithoutQueryAndIfBiomodelsCannotBeReached() throws Exception {
        CommandTestSupport support = new CommandTestSupport();
        BiomodelsSearchCommand.BiomodelsSearchTask noQuery =
                new BiomodelsSearchCommand.BiomodelsSearchTask(support.services());
        assertEquals(
                "Give the search query.",
                assertThrows(IllegalArgumentException.class, () -> CommandTestSupport.run(noQuery))
                        .getMessage());

        when(support.biomodelsQuery.search("glycolysis")).thenThrow(new IOException("BioModels unreachable"));
        BiomodelsSearchCommand.BiomodelsSearchTask task =
                new BiomodelsSearchCommand.BiomodelsSearchTask(support.services());
        task.query = "glycolysis";
        assertEquals(
                "BioModels unreachable",
                assertThrows(IOException.class, () -> CommandTestSupport.run(task))
                        .getMessage());
    }
}
