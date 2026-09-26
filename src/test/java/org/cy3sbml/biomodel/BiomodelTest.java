package org.cy3sbml.biomodel;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

public class BiomodelTest {
    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Test
    public void fromJson() throws Exception {
        JsonNode json = MAPPER.readTree("""
                {"submissionId": "MODEL1234", "publicationId": "BIOMD0000000012",
                 "name": "Elowitz2000 - Repressilator", "description": "A repressilator.",
                 "publication": {"accession": "10659856",
                                 "authors": [{"name": "Elowitz MB"}, {"name": "Leibler S"}, {}]}}
                """);

        Biomodel biomodel = Biomodel.fromJson(json);

        assertEquals(
                new Biomodel(
                        "BIOMD0000000012",
                        "MODEL1234",
                        "10659856",
                        "Elowitz2000 - Repressilator",
                        "A repressilator.",
                        "Elowitz MB, Leibler S"),
                biomodel);
    }

    @Test
    public void fromJsonWithoutOptionalFields() throws Exception {
        JsonNode json = MAPPER.readTree("{\"submissionId\": \"MODEL1234\", \"publication\": {}}");

        assertEquals(new Biomodel("", "MODEL1234", "", "", "", ""), Biomodel.fromJson(json));
    }

    @Test
    public void fromJsonWithoutSubmissionId() throws Exception {
        JsonNode json = MAPPER.readTree("{\"publication\": {}}");

        assertThrows(IllegalArgumentException.class, () -> Biomodel.fromJson(json));
    }
}
