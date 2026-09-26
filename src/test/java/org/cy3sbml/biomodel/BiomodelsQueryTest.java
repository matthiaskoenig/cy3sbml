package org.cy3sbml.biomodel;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests parsing of the BioModels search response, recorded from
 * https://www.ebi.ac.uk/biomodels/search?query=glucose&format=json&numResults=2
 * (trimmed to the "matches", "models" and "queryParameters" fields actually used).
 */
public class BiomodelsQueryTest {

    private static String fixture(String resource) throws IOException {
        try (InputStream in = BiomodelsQueryTest.class.getResourceAsStream(resource)) {
            if (in == null) {
                throw new IOException("Missing test resource: " + resource);
            }
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    @Test
    public void parsesSearchResult() throws IOException {
        String json = fixture("/biomodel/search_glucose.json");
        BiomodelsQueryResult result = new BiomodelsQueryResult("glucose", 200, json);

        assertEquals(true, result.success());

        List<String> biomodelIds = result.getBiomodelIdsFromSearch();
        assertEquals(List.of("MODEL1204270001", "MODEL1209260000"), biomodelIds);
    }

    @Test
    public void getBiomodelIdsFromSearch_skipsModelsWithoutId() {
        String json = """
                {"models": [
                    {"id": "MODEL1204270001", "name": "with id"},
                    {"name": "missing id"},
                    {"id": "MODEL1209260000", "name": "with id"}
                ]}
                """;
        BiomodelsQueryResult result = new BiomodelsQueryResult("glucose", 200, json);

        List<String> biomodelIds = result.getBiomodelIdsFromSearch();
        assertEquals(List.of("MODEL1204270001", "MODEL1209260000"), biomodelIds);
    }
}
