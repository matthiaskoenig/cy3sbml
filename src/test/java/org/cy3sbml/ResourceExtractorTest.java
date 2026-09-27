package org.cy3sbml;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

public class ResourceExtractorTest {

    @Test
    public void getResource() throws Exception {
        ResourceExtractor.setAppDirectory(null);
        String resource = ResourceExtractor.getResource("/gui/help.html");
        // without appdirectory the resources cannot be resolved
        assertNull(resource);
    }
}
