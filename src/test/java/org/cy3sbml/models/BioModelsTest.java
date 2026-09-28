package org.cy3sbml.models;

import java.util.HashSet;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import org.cy3sbml.TestUtils;
import org.cytoscape.work.TaskMonitor;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

/**
 * Test cases for biomodels.
 * <p>
 * Retrieved on 2024-01-16, 1072 curated models
 */
@Tag("models")
@ExtendWith(MockitoExtension.class)
// NetworkTestSupport stubs mocks the reader does not use
@MockitoSettings(strictness = Strictness.LENIENT)
public class BioModelsTest {

    @Mock
    TaskMonitor taskMonitor;

    static Stream<String> biomodelsResources() {
        HashSet<String> skip = null;
        String filter = null;
        return StreamSupport.stream(
                        TestUtils.findResources("corpora", TestUtils.BIOMODELS_RESOURCE_PATH, ".xml", filter, skip)
                                .spliterator(),
                        false)
                .map(arr -> arr[0].toString());
    }

    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("biomodelsResources")
    void testSingle(String resource) throws Exception {
        TestUtils.testNetwork(taskMonitor, getClass().getName(), resource);
    }

    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("biomodelsResources")
    void testSerialization(String resource) throws Exception {
        TestUtils.testNetworkSerialization(getClass().getName(), resource);
    }
}
