package org.cy3sbml.ols;

import org.apache.commons.lang.StringUtils;
import org.cy3sbml.miriam.Namespace;
import org.identifiers.registry.RegistryDatabase;
import org.identifiers.registry.RegistryUtilities;
import org.junit.jupiter.api.Test;
import uk.ac.ebi.pride.utilities.ols.web.service.model.Term;

import java.util.ArrayList;
import java.util.List;

import static org.cy3sbml.gui.SBaseHTMLFactory.result;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;


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
    public void getTermCompact() {
        List<String> resourceURIs = new ArrayList<>();
        resourceURIs.add("http://identifiers.org/GO:0042752");
        resourceURIs.add("http://identifiers.org/CHEBI:68579");
        for (String resourceURI : resourceURIs) {
            String identifier = StringUtils.substringAfter(resourceURI, "http://identifiers.org/");
            Term term = OLSAccess.getTerm(identifier);
            assertNotNull(term);
        }
    }

    @Test
    public void getTermHttps() throws Exception {
        String resourceURI = "https://identifiers.org/GO:0042752";
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