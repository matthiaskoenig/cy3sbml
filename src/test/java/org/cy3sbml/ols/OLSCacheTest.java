package org.cy3sbml.ols;


import uk.ac.ebi.pride.utilities.ols.web.service.model.Term;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;


public class OLSCacheTest {
    @Test
    public void getTerm() throws Exception {
        Term t1 = OLSCache.getTerm("GO:0042752");
        Term t2 = OLSCache.getTerm("GO:0042752");
        // if second term is the cached first one, they are identical.
        assertEquals(t1, t2);
    }

}