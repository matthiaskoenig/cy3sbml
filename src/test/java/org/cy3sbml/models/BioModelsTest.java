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
 * Test cases for biomodels.
 * <p>
 * 31th BioModels Release, 2017-06-26
 * ftp://ftp.ebi.ac.uk/pub/databases/biomodels/releases/2017-06-26/
 * https://www.ebi.ac.uk/biomodels-main/static-pages.do?page=release_20170626
 * Retrieved on 2017-10-03, 640 curated models
 */
public class BioModelsTest {

    @Mock
    TaskMonitor taskMonitor;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    static Stream<String> biomodelsResources() {
        HashSet<String> skip = null;
        String filter = null;
        return StreamSupport.stream(
                        TestUtils.findResources(TestUtils.BIOMODELS_RESOURCE_PATH, ".xml", filter, skip).spliterator(),
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
