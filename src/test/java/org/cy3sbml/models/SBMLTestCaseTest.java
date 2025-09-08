package org.cy3sbml.models;

import java.util.HashSet;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.cy3sbml.TestUtils;
import org.cytoscape.work.TaskMonitor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

/**
 * Test all SBML files of the SBML TestCases.
 * <p>
 * sbml-test-suite-v3.3.0 (stochastic and semantic branch)
 * https://github.com/sbmlteam/sbml-test-suite/releases/tag/3.3.0
 * Retrieved on 2017-12-14.
 */
public class SBMLTestCaseTest {

    @Mock
    TaskMonitor taskMonitor;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    static Stream<String> sbmlTestCases() {
        HashSet<String> skip = new HashSet<>();
        String filter = "-sbml-l\\dv\\d.xml";

        return StreamSupport.stream(
                TestUtils.findResources(TestUtils.SBMLTESTCASES_RESOURCE_PATH, ".xml", filter, skip).spliterator(),
                false
        ).map(arr -> arr[0].toString());
    }

    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("sbmlTestCases")
    void testSingle(String resource) throws Exception {
        TestUtils.testNetwork(taskMonitor, getClass().getName(), resource);
    }

    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("sbmlTestCases")
    void testSerialization(String resource) throws Exception {
        TestUtils.testNetworkSerialization(getClass().getName(), resource);
    }
}