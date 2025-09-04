package org.cy3sbml.uniprot;


import uk.ac.ebi.kraken.interfaces.uniprot.UniProtEntry;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
public class UniprotCacheTest {
    @Test
    public void getUniProtEntry() throws Exception {

        UniProtEntry e1 = UniprotCache.getUniProtEntry("P10415");
        UniProtEntry e2 = UniprotCache.getUniProtEntry("P10415");

        // if second term is the cached first one, they are identical.
        assertEquals(e1, e2);
    }

}