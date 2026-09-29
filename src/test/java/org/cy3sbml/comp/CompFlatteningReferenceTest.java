package org.cy3sbml.comp;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.fasterxml.jackson.databind.JsonNode;
import java.nio.file.Path;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * The JSBML flattening of the comp test models has the elements and references of the
 * libSBML flattening. The comp cases of the SBML test suite are checked by the models
 * suite ({@code CompFlatteningSuiteTest}).
 */
class CompFlatteningReferenceTest {

    static Path referenceFile() throws Exception {
        return Path.of(CompFlatteningReferenceTest.class
                .getResource("/models/comp/comp-flat-reference.json")
                .toURI());
    }

    static Stream<Arguments> models() throws Exception {
        return FlatReference.entries(referenceFile()).map(e -> Arguments.of(e.getKey(), e.getValue()));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("models")
    void flatModelMatchesLibsbml(String model, JsonNode reference) throws Exception {
        Map<String, Object> actual =
                FlatReference.flatten(referenceFile().getParent().resolve(model).toFile());

        assertEquals(FlatReference.expected(reference), actual);
    }
}
