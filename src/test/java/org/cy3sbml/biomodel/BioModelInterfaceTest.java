package org.cy3sbml.biomodel;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.cy3sbml.TestUtils;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Tests the access to the live BioModels REST API.
 */
@Tag("network")
public class BioModelInterfaceTest {
    static final String VALID_BIOMODEL_ID = "BIOMD0000000070";
    static final String VALID_BIOMODEL_PERSON = "gille";
    static final String VALID_BIOMODEL_NAME = "glycolysis";
    static final String INVALID_BIOMODEL_ID = "BIOMD9999999999";

    private final BiomodelsQuery query = BiomodelsQuery.createDefault();

    @TempDir
    Path tempDir;

    @BeforeAll
    public static void onlyOnce() {
        TestUtils.setSystemProxyForTests();
    }

    @Test
    public void downloadSBML() throws IOException {
        Path file = tempDir.resolve(VALID_BIOMODEL_ID + ".xml");
        query.downloadSBML(VALID_BIOMODEL_ID, file);

        assertTrue(Files.readString(file).contains("<sbml"));
    }

    @Test
    public void downloadSBMLOfUnknownIdFails() {
        Path file = tempDir.resolve(INVALID_BIOMODEL_ID + ".xml");
        IOException e = assertThrows(IOException.class, () -> query.downloadSBML(INVALID_BIOMODEL_ID, file));

        assertTrue(e.getMessage().contains("HTTP status 404"), e.getMessage());
    }

    @Test
    public void biomodelQuery() {
        Biomodel biomodel = query.performBiomodelQuery(VALID_BIOMODEL_ID).join();

        assertEquals(VALID_BIOMODEL_ID, biomodel.id());
        assertFalse(biomodel.name().isEmpty());
    }

    @Test
    public void searchByPerson() {
        BiomodelsQueryResult result = query.performSearchQuery(VALID_BIOMODEL_PERSON);

        assertTrue(result.success());
        assertFalse(result.getBiomodelIdsFromSearch().isEmpty(), "More than 0 models have to exist.");
    }

    @Test
    public void searchByName() {
        BiomodelsQueryResult result = query.performSearchQuery(VALID_BIOMODEL_NAME);

        assertTrue(result.success());
        List<String> modelIds = result.getBiomodelIdsFromSearch();
        assertFalse(modelIds.isEmpty(), "More than 0 models have to exist.");
    }
}
