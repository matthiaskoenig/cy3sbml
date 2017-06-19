package org.cy3sbml;

import org.junit.Test;
import static org.junit.Assert.*;


public class ResourceExtractorTest {

    @Test
    public void getResource() throws Exception {
        ResourceExtractor.setAppDirectory(null);
        String resource = ResourceExtractor.getResource("/gui/help.html");
        // without appdirectory the resources cannot be resolved
        assertNull(resource);
    }

}