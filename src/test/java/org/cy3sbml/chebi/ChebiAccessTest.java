package org.cy3sbml.chebi;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;


/**
 * Test access to chebi via rest queries.
 */
public class ChebiAccessTest {

    @Test
    public void getChebiHTML() {
        String accession = "CHEBI:15377";
        String html = ChebiAccess.getChebiHTML(accession);
        assertNotNull(html);
        assertTrue(html.contains("CHEBI:15377"));
        assertTrue(html.contains("<svg"));
    }

}