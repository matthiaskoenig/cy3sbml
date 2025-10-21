package org.cy3sbml;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class ResourceExtractorTest {

    @Test
    public void getResource() throws Exception {
        ResourceExtractor.setAppDirectory(null);
        String resource = ResourceExtractor.getResource("/gui/help.html");
        // without appdirectory the resources cannot be resolved
        assertNull(resource);
    }

}