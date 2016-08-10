package org.cy3sbml.ols;

import org.identifiers.registry.RegistryUtilities;
import org.junit.Test;
import uk.ac.ebi.pride.utilities.ols.web.service.model.Term;

import static org.junit.Assert.*;

/**
 * Test OLS Access.
 */
public class OLSAccessTest {
    @Test
    public void getTerm() throws Exception {
        String resourceURI = "http://identifiers.org/go/GO:0042752";
        String identifier = RegistryUtilities.getIdentifierFromURI(resourceURI);

        Term term = OLSAccess.getTerm(identifier);
        assertNotNull(term);
    }

    @Test
    public void termToString() throws Exception {
        String resourceURI = "http://identifiers.org/go/GO:0042752";
        String identifier = RegistryUtilities.getIdentifierFromURI(resourceURI);

        Term term = OLSAccess.getTerm(identifier);
        String text = OLSAccess.termToString(term);
        assertNotNull(text);
        assertTrue(text.contains("regulation of circadian rhythm"));
        assertTrue(text.contains("go\n"));
    }

}