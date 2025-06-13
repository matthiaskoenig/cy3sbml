package org.cy3sbml.miriam;

import org.junit.Test;

import java.io.File;
import static org.junit.Assert.*;


/**
 * Testing RegistryUtils.
 */
public class RegistryUtilTest {

    @Test
    public void updateMiriamJSON() throws Exception {
        File f = File.createTempFile("test", ".json");
        RegistryUtil.updateMiriamJSON(f);
        assertNotNull(f);
        RegistryUtil.loadRegistry(f);
    }

    @Test
    public void updateMiriamJSONWithNewer() throws Exception {
        File f = File.createTempFile("test", ".json");
        RegistryUtil.updateMiriamJSON(f);

        // identical file
        RegistryUtil.updateMiriamJSONWithNewer(f);
    }




}