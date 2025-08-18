package org.cy3sbml.biomodel;


import java.util.List;
import java.util.function.Supplier;

import org.cy3sbml.TestUtils;

import org.junit.jupiter.api.Test;

import uk.ac.ebi.biomodels.ws.BioModelsWSException;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test biomodels access.
 */
public class BioModelInterfaceTest {
    static final String VALID_BIOMODEL_ID = "BIOMD0000000070";
    static final String VALID_BIOMODEL_PERSON = "gille";
    static final String VALID_BIOMODEL_NAME = "glycolysis";
    static final String INVALID_STRING = "xcvsfsfasdfa1323452342";

    BioModelWSInterface bmInterface;


    public static void onlyOnce() {
        TestUtils.setSystemProxyForTests();
    }


    public void setUp() {
        bmInterface = new BioModelWSInterface(null, null);
    }


    public void tearDown() {
        bmInterface = null;
    }

    @Test
    public void testGetBioModelIdsByName() throws BioModelsWSException {
        List<String> modelIds = bmInterface.getBioModelIdsByName(VALID_BIOMODEL_NAME);
        assertTrue(modelIds.size() > 0, "More than 0 models have to exist.");
    }

    @Test
    public void testGetBioModelIdsByName2() throws BioModelsWSException {
        List<String> modelIds = bmInterface.getBioModelIdsByName(INVALID_STRING);
        assertNotNull((Object) "If invalid name empty list is returned.", (Supplier<String>) modelIds);
        assertTrue(modelIds.size() == 0, "No models in list for invalid name,");
    }

    @Test
    public void testGetBioModelIdsByPerson() throws BioModelsWSException {
        List<String> modelIds = bmInterface.getBioModelIdsByPerson(VALID_BIOMODEL_PERSON);
        assertNotNull((Object) "Models have to exist.", (Supplier<String>) modelIds);
        assertTrue(modelIds.size() > 0, "More than 0 models have to exist.");
    }

    @Test
    public void testGetBioModelIdsByPerson2() throws BioModelsWSException {
        List<String> modelIds = bmInterface.getBioModelIdsByPerson(INVALID_STRING);
        assertNotNull((Object) "If invalid name empty list is returned.", (Supplier<String>) modelIds);
        assertTrue(modelIds.size() == 0, "No models in list for invalid name,");
    }

    @Test
    public void testGetBioModelNameById() throws BioModelsWSException {
        String name = bmInterface.getBioModelNameById(VALID_BIOMODEL_ID);
        assertNotNull("Name has to exist.", name);
    }

    @Test
    public void testGetBioModelNameById2() throws BioModelsWSException {
        String name = bmInterface.getBioModelNameById(INVALID_STRING);
        assertNull("If invalid id, null is returned.", name);
    }

    @Test
    public void testGetAuthorsByModelId() throws BioModelsWSException {
        List<String> authors = bmInterface.getAuthorsByModelId(VALID_BIOMODEL_ID);
        assertNotNull((Object) "Authors have to exist.", (Supplier<String>) authors);
        assertTrue(authors.size() > 0, "Authors have to exist.");
    }

    @Test
    public void testGetAuthorsByModelId2() throws BioModelsWSException {
        List<String> authors = bmInterface.getAuthorsByModelId(INVALID_STRING);
        assertNotNull((Object) "Empty list for invalid search term.", (Supplier<String>) authors);
        assertTrue(authors.size() == 0, "Empty list for invalid search term,");
    }

    @Test
    public void testGetEncodersByModelId() throws BioModelsWSException {
        List<String> encoders = bmInterface.getEncodersByModelId(VALID_BIOMODEL_ID);
        assertNotNull((Object) "Encoders have to exist.", (Supplier<String>) encoders);
        assertTrue(encoders.size() > 0, "More than 0 models have to exist.");
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
    }

    @Test
    public void testGetBioModelSBMLById() throws BioModelsWSException {
        String sbml = bmInterface.getBioModelSBMLById(VALID_BIOMODEL_ID);
        assertNotNull("SBML has to exist.", sbml);
    }

    @Test
    public void testGetBioModelSBMLById2() throws BioModelsWSException {
        String sbml = bmInterface.getBioModelSBMLById(INVALID_STRING);
        assertNotNull("If invalid id, null is returned.", sbml);
    }

}
