
package org.cy3sbml.biomodel;


import java.io.IOException;
import java.util.List;

import org.cy3sbml.TestUtils;
import org.junit.jupiter.api.*;

import static org.junit.jupiter.api.Assertions.*;


/**
 * Test biomodels access.
 */
public class BioModelInterfaceTest {
    static final String VALID_BIOMODEL_ID = "BIOMD0000000070";
    static final String VALID_BIOMODEL_PERSON = "gille";
    static final String VALID_BIOMODEL_NAME = "glycolysis";
    static final String INVALID_STRING = "xcvsfsfasdfa1323452342";

    BiomodelsQuery bmQuery;

    @BeforeAll
    public static void onlyOnce() {
        TestUtils.setSystemProxyForTests();
    }

    @BeforeEach
    public void setUp() {
        bmQuery = new BiomodelsQuery();
    }

    @AfterEach
    public void tearDown() {
        bmQuery = null;
    }


    @Test
    public void testGetBioModelSBMLById() throws IOException, InterruptedException {
        String sbml = BiomodelsQuery.getBioModelSBMLById(VALID_BIOMODEL_ID);
        assertNotNull(sbml);
    }

    @Test
    public void testGetBioModelSBMLById2() throws IOException, InterruptedException {
        String sbml = BiomodelsQuery.getBioModelSBMLById(INVALID_STRING);
        assertNotNull(sbml);
    }

    @Test
    public void testGetBioModelIdsByPerson() throws IOException, InterruptedException {
        List<String> modelIds = BiomodelsQuery.performSearchQuery(VALID_BIOMODEL_PERSON).getBiomodelIdsFromSearch();
        assertNotNull(modelIds, () -> "Models have to exist.");
        assertFalse(modelIds.isEmpty(), "More than 0 models have to exist.");
        for (String modelId : modelIds) {
            System.out.println(modelId);
        }
    }

    @Test
    public void testGetBioModelIdsByName() throws IOException, InterruptedException {
        List<String> modelIds = BiomodelsQuery.performSearchQuery(VALID_BIOMODEL_NAME).getBiomodelIdsFromSearch();
        assertNotNull(modelIds, () -> "Models have to exist.");
        assertFalse(modelIds.isEmpty());
        for (String modelId : modelIds) {
            System.out.println(modelId);
        }
    }
}
