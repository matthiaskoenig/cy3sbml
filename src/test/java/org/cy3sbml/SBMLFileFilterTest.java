package org.cy3sbml;

import org.cytoscape.io.DataCategory;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.InputStream;
import java.util.Set;

import static org.junit.Assert.*;

/**
 * Testing the SBML file filter.
 */
public class SBMLFileFilterTest {
    private SBMLFileFilter filter;
    private static final String DESCRIPTION = "Test description";

    @Before
    public void setUp() {
        filter = new SBMLFileFilter(DESCRIPTION, null);
    }

    @After
    public void tearDown() {
        filter = null;
    }

    @Test
    public void accept() throws Exception {
        InputStream instream = getClass().getResourceAsStream(SBMLCoreTest.TEST_MODEL_01);
        boolean accepted = filter.accepts(instream, DataCategory.NETWORK);
        assertTrue(accepted);
    }

    @Test
    public void getExtensions() throws Exception {
        Set<String> extensions = filter.getExtensions();
        assertTrue(extensions.contains("sbml"));
        assertTrue(extensions.contains("xml"));
        assertTrue(extensions.contains(""));
    }

    @Test
    public void getContentTypes() throws Exception {
        Set<String> contentTypes = filter.getContentTypes();
        assertTrue(contentTypes.contains("text/xml"));
        assertTrue(contentTypes.contains("application/xml"));
    }

    @Test
    public void getDescription() throws Exception {
        String description = filter.getDescription();
        assertEquals(DESCRIPTION, description);
    }

    @Test
    public void getDataCategory() throws Exception {
        DataCategory category = filter.getDataCategory();
        assertEquals(DataCategory.NETWORK, category);
    }

}