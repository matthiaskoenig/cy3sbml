package org.cy3sbml.ols;

import org.cy3sbml.miriam.RegistryUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import uk.ac.ebi.pride.utilities.ols.web.service.model.Term;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;


/**
 * Test OLS Access.
 */
public class OLSAccessTest {


    @ParameterizedTest
    @ValueSource(strings = {
            "GO:0042752",
            "CHEBI:68579",
            "PUBMED:68579",
    })
    public void getTermFromIdentifier(String identifier) throws Exception {
        Term term = OLSAccess.getTerm(identifier);
        assertNotNull(term);
    }


    @ParameterizedTest
    @ValueSource(strings = {
            "http://identifiers.org/GO:0042752",
            "http://identifiers.org/CHEBI:68579",
            "https://identifiers.org/GO:0042752",
            "https://identifiers.org/CHEBI:68579",
            "https://identifiers.org/pubmed/68579",
            "https://identifiers.org/PUBMED:68579",
    })
    public void getTermFromURI(String resourceURI) {
        String compactId = RegistryUtil.compactIdFromResourceURI(resourceURI);
        Term term = OLSAccess.getTerm(compactId);
        assertNotNull(term);
    }

    @Test
    public void termToString() {
        Term term = OLSAccess.getTerm("GO:0042752");
        String text = OLSAccess.termToString(term);
        assertNotNull(text);
        assertTrue(text.contains("regulation of circadian rhythm"));
        assertTrue(text.contains("go\n"));
    }

}