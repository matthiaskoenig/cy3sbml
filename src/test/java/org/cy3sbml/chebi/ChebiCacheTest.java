package org.cy3sbml.chebi;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class ChebiCacheTest {
    @Test
    public void getChebiHTML() throws Exception {

        String e1 = ChebiCache.getChebiHTML("CHEBI:15377");
        String e2 = ChebiCache.getChebiHTML("CHEBI:15377");

        // if second term is the cached first one, they are identical.
        assertEquals(e1, e2);
    }

}