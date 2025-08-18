package org.cy3sbml.miriam;



import java.io.File;
import org.junit.jupiter.api.Test;


import static org.junit.jupiter.api.Assertions.assertNotNull;


/**
 * Testing RegistryUtils.
 */
public class RegistryUtilTest {

    @Test
    public void updateMiriamXML() throws Exception {
        File f = File.createTempFile("test", ".xml");
        RegistryUtil.updateMiriamXML(f);
        assertNotNull(f);
        RegistryUtil.loadRegistry(f);
    }

    @Test
    public void updateMiriamXMLWithNewer() throws Exception {
        File f = File.createTempFile("test", ".xml");
        RegistryUtil.updateMiriamXML(f);

        // identical file
        RegistryUtil.updateMiriamXMLWithNewer(f);
    }

    @Test
    public void loadRegistry() throws Exception {
        RegistryUtil.loadRegistry();
    }

}