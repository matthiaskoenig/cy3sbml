package org.cy3sbml.miriam;


import org.junit.jupiter.api.Test;

import java.io.File;

import static org.junit.jupiter.api.Assertions.assertNotNull;


/**
 * Testing RegistryUtils.
 */
public class RegistryUtilTest {

    @Test
    public void updateMiriamXML() throws Exception {
        File f = File.createTempFile("test", ".xml");
        assertNotNull(f);
        RegistryUtil.updateMiriamJSON(f);
        assertNotNull(RegistryUtil.getMiriamContent());
    }



    @Test
    public void loadRegistry() throws Exception {
        assertNotNull(RegistryUtil.getMiriamContent());
    }

}