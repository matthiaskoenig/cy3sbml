package org.cy3sbml.models;

import java.util.HashSet;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import org.cy3sbml.TestUtils;
import org.cytoscape.work.TaskMonitor;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.parallel.ResourceLock;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

/**
 * Test cases for the BIGG models.
 * bigg_models v1.6 (https://github.com/SBRG/bigg_models/releases)
 * <p>
 * Models were retrieved on 2025-09-13 via the webservice.
 */
@Tag("models")
@ExtendWith(MockitoExtension.class)
// NetworkTestSupport stubs mocks the reader does not use
@MockitoSettings(strictness = Strictness.LENIENT)
// the model suites need a lot of memory, so they do not run at the same time
@ResourceLock("model-suites")
public class BiGGTest {

    @Mock
    TaskMonitor taskMonitor;

    static Stream<String> biggModelResources() {
        HashSet<String> skip = null;
        String filter = null;
        return StreamSupport.stream(
                        TestUtils.findResources("corpora", TestUtils.BIGGMODELS_RESOURCE_PATH, ".xml", filter, skip)
                                .spliterator(),
                        false)
                .map(arr -> arr[0].toString());
    }

    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("biggModelResources")
    void testSingle(String resource) throws Exception {
        TestUtils.testNetwork(taskMonitor, getClass().getName(), resource);
    }

    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("biggModelResources")
    void testSerialization(String resource) throws Exception {
        TestUtils.testNetworkSerialization(getClass().getName(), resource);
    }
}
