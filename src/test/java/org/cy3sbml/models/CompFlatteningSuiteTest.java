package org.cy3sbml.models;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.fasterxml.jackson.databind.JsonNode;
import java.nio.file.Path;
import java.util.Map;
import java.util.stream.Stream;
import org.cy3sbml.comp.FlatReference;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * The JSBML flattening of the comp cases of the SBML test suite has the elements and
 * references of the libSBML flattening
 * ({@code src/test/corpora/models/sbml-test-suite/comp-flat-reference.json}).
 */
@Tag("models")
class CompFlatteningSuiteTest {

    static Path referenceFile() throws Exception {
        return Path.of(CompFlatteningSuiteTest.class
                .getResource("/models/sbml-test-suite/comp-flat-reference.json")
                .toURI());
    }

    static Stream<Arguments> testCases() throws Exception {
        return FlatReference.entries(referenceFile()).map(e -> Arguments.of(e.getKey(), e.getValue()));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("testCases")
    void flatModelMatchesLibsbml(String testCase, JsonNode reference) throws Exception {
        Map<String, Object> actual = FlatReference.flatten(
                referenceFile().getParent().resolve(testCase).toFile());

        assertEquals(FlatReference.expected(reference), actual);
    }
}
