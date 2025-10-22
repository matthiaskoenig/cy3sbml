package org.cy3sbml.miriam;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.*;
import java.util.*;
import java.util.stream.Stream;

/**
 * Testing RegistryUtils.
 */
public class RegistryUtilTest {


    @Test
    public void updateMiriamJSON() throws Exception {
        File f = File.createTempFile("test", ".json");
        assertNotNull(f);
        RegistryUtil.updateMiriamJSON(f);
        Map<String, Namespace> namespaceMap = RegistryUtil.loadRegistry(f);
        assertNotNull(namespaceMap);
    }

    private static Stream<Arguments> prefixTestData() {
        return Stream.of(
                Arguments.of("http://identifiers.org/GO:0042752", "go"),
                Arguments.of("http://identifiers.org/go/GO:0042752", "go"),
                Arguments.of("http://identifiers.org/CHEBI:68579", "chebi"),
                Arguments.of("http://identifiers.org/chebi/CHEBI:68579", "chebi"),
                Arguments.of("https://identifiers.org/GO:0042752", "go"),
                Arguments.of("https://identifiers.org/CHEBI:68579", "chebi")
        );
    }

    @ParameterizedTest
    @MethodSource("prefixTestData")
    public void getPrefixFromURI(String resourceURI, String expectedId) {
        String prefix = RegistryUtil.prefixFromResourceURI(resourceURI);
        assertNotNull(prefix);
        assertEquals(expectedId, prefix);
    }


    private static Stream<Arguments> compactIdsTestData() {
        return Stream.of(
                Arguments.of("http://identifiers.org/GO:0042752", "GO:0042752"),
                Arguments.of("http://identifiers.org/go/GO:0042752", "GO:0042752"),
                Arguments.of("http://identifiers.org/CHEBI:68579", "CHEBI:68579"),
                Arguments.of("http://identifiers.org/chebi/CHEBI:68579", "CHEBI:68579"),
                Arguments.of("https://identifiers.org/GO:0042752", "GO:0042752"),
                Arguments.of("https://identifiers.org/CHEBI:68579", "CHEBI:68579")
        );
    }

    @ParameterizedTest
    @MethodSource("compactIdsTestData")
    public void getCompactIdFromURI(String resourceURI, String expectedId) {
        String compactId = RegistryUtil.compactIdFromResourceURI(resourceURI);
        assertNotNull(compactId);
        assertEquals(expectedId, compactId);
    }
}