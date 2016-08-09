package org.cy3sbml;

import org.cytoscape.property.CyProperty;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Testing the property reader.
 */
public class PropsReaderTest {
    private static final String NAME = "name";
    private static final String FILENAME = "filename";


    @Test
    public void test(){
        PropsReader reader = new PropsReader(NAME, FILENAME);
        assertNotNull(reader);
        assertEquals(reader.getName(), NAME);
        assertEquals(reader.getSavePolicy(), CyProperty.SavePolicy.CONFIG_DIR);
    }

}