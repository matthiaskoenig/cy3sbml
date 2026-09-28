package org.cy3sbml.biomodel;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Tests parsing of the BioModels search response, recorded from
 * https://www.biomodels.org/search?query=glucose&format=json&numResults=2
 * (trimmed to the "matches", "models" and "queryParameters" fields).
 */
public class BiomodelsQueryTest {
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private static JsonNode fixture(String resource) throws IOException {
        try (InputStream in = BiomodelsQueryTest.class.getResourceAsStream(resource)) {
            if (in == null) {
                throw new IOException("Missing test resource: " + resource);
            }
            return MAPPER.readTree(in);
        }
    }

    @Test
    public void parsesSearchResult() throws IOException {
        JsonNode json = fixture("/biomodel/search_glucose.json");

        List<BiomodelSummary> models =
                json.path("models").valueStream().map(BiomodelSummary::fromJson).toList();

        assertEquals(
                List.of(
                        new BiomodelSummary(
                                "MODEL1204270001",
                                "Koenig2012 - Hepatic Glucose Metabolism",
                                "2012-04-27T00:00:00Z",
                                ""),
                        new BiomodelSummary(
                                "MODEL1209260000",
                                json.path("models").get(1).path("name").asText(),
                                json.path("models")
                                        .get(1)
                                        .path("submissionDate")
                                        .asText(),
                                json.path("models").get(1).path("lastModified").asText(""))),
                models);
    }

    @Test
    public void modelWithoutIdHasNoSummary() throws IOException {
        assertNull(BiomodelSummary.fromJson(MAPPER.readTree("{\"name\": \"missing id\"}")));
        assertNull(BiomodelSummary.fromJson(MAPPER.readTree("{\"id\": null}")));
    }

    @Test
    public void searchResultHasAtLeastTheModelsReadAsMatches() {
        BiomodelSummary model = new BiomodelSummary("BIOMD0000000001", "", "", "");

        assertEquals(1, new BiomodelsSearchResult(0, List.of(model)).matches());
        assertEquals(false, new BiomodelsSearchResult(2, List.of(model)).isComplete());
    }
}
