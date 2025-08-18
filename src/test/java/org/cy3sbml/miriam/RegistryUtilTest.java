package org.cy3sbml.miriam;

import org.junit.Test;

import java.io.File;
import static org.junit.Assert.*;


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