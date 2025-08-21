
package org.cy3sbml.biomodel;



import java.io.IOException;
import java.util.List;
import java.util.function.Supplier;

import org.cy3sbml.TestUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;


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


   /* public void testGetBioModelIdsByName() throws BioModelsWSException, IOException, InterruptedException {
        List<String> modelIds = bmInterface.getBioModelIdsByName(VALID_BIOMODEL_NAME);
        assertNotNull("Models have to exist.", modelIds);
        assertTrue("More than 0 models have to exist.", modelIds.size() > 0);
    }*/


    /*public void testGetBioModelIdsByName2() throws BioModelsWSException, IOException, InterruptedException {
        List<String> modelIds = bmInterface.getBioModelIdsByName(INVALID_STRING);
        assertNotNull("If invalid name empty list is returned.", modelIds);
        assertTrue("No models in list for invalid name,", modelIds.size() == 0);
    }





    public void testGetBioModelIdsByPerson2() throws BioModelsWSException {
        List<String> modelIds = bmInterface.getBioModelIdsByPerson(INVALID_STRING);
        assertNotNull("If invalid name empty list is returned.", modelIds);
        assertTrue("No models in list for invalid name,", modelIds.size() == 0);
    }*/


    /*public void testGetBioModelNameById() throws BioModelsWSException {
        String name = bmInterface.getBioModelNameById(VALID_BIOMODEL_ID);
        assertNotNull("Name has to exist.", name);
    }

    @Test
    public void testGetBioModelNameById2() throws BioModelsWSException {
        String name = bmInterface.getBioModelNameById(INVALID_STRING);
        assertNull("If invalid id, null is returned.", name);
    }
*/

   /* public void testGetAuthorsByModelId() throws BioModelsWSException {
        List<String> authors = bmInterface.getAuthorsByModelId(VALID_BIOMODEL_ID);
        assertNotNull("Authors have to exist.", authors);
        assertTrue("Authors have to exist.", authors.size() > 0);
    }

    @Test
    public void testGetAuthorsByModelId2() throws BioModelsWSException {
        List<String> authors = bmInterface.getAuthorsByModelId(INVALID_STRING);
        assertNotNull("Empty list for invalid search term.", authors);
        assertTrue("Empty list for invalid search term,", authors.size() == 0);
    }

    @Test
    public void testGetEncodersByModelId() throws BioModelsWSException {
        List<String> encoders = bmInterface.getEncodersByModelId(VALID_BIOMODEL_ID);
        assertNotNull("Encoders have to exist.", encoders);
        assertTrue("More than 0 models have to exist.", encoders.size() > 0);
    }

    @Test
    public void testGetEncodersByModelId2() throws BioModelsWSException {
        List<String> encoders = bmInterface.getEncodersByModelId(INVALID_STRING);
        assertNotNull("Empty list for invalid search term.", encoders);
        assertTrue("Empty list for invalid search term,", encoders.size() == 0);
    }

    @Test
    public void testGetDateLastModifiedByModelId() throws BioModelsWSException {
        String dateString = bmInterface.getDateLastModifiedByModelId(VALID_BIOMODEL_ID);
        assertNotNull("DateString has to exist.", dateString);
    }

    @Test
    public void testGetDateLastModifiedByModelId2() throws BioModelsWSException {
        String dateString = bmInterface.getDateLastModifiedByModelId(INVALID_STRING);
        assertNull("If invalid id, empty String is returned", dateString);
    }*/

    @Test
    public void testGetBioModelSBMLById() throws IOException, InterruptedException {
        String sbml = BiomodelsQuery.getBioModelSBMLById(VALID_BIOMODEL_ID);
        System.out.println(sbml);
        assertNotNull("SBML has to exist.", sbml);
    }

    @Test
    public void testGetBioModelSBMLById2() throws IOException, InterruptedException {
        String sbml = BiomodelsQuery.getBioModelSBMLById(INVALID_STRING);
        System.out.println(sbml);
        assertNotNull("If invalid id, null is returned.", sbml);
    }

    @Test
    public void testGetBioModelIdsByPerson() throws IOException, InterruptedException {
        List<String> modelIds = BiomodelsQuery.performSearchQuery(VALID_BIOMODEL_PERSON).getBiomodelIdsFromSearch();
        assertNotNull(modelIds, () -> "Models have to exist.");
        assertTrue(modelIds.size() > 0, "More than 0 models have to exist.");
        for (String modelId: modelIds){
            System.out.println(modelId);
        }
    }

    @Test
    public void testGetBioModelIdsByName() throws IOException, InterruptedException {
        List<String> modelIds = BiomodelsQuery.performSearchQuery(VALID_BIOMODEL_NAME).getBiomodelIdsFromSearch();
        assertNotNull(modelIds, () -> "Models have to exist.");
        assertTrue(modelIds.size() > 0, "More than 0 models have to exist.");
        for (String modelId: modelIds){
            System.out.println(modelId);
        }
    }
}
