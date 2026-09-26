package org.cy3sbml.reader;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicInteger;
import org.cy3sbml.SBML;
import org.cy3sbml.SBMLReaderError;
import org.cy3sbml.util.AttributeUtil;
import org.cytoscape.group.GroupTestSupport;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNetworkFactory;
import org.cytoscape.model.CyNode;
import org.cytoscape.model.NetworkTestSupport;
import org.cytoscape.work.TaskMonitor;
import org.junit.jupiter.api.Test;

class SBMLReaderTaskTest {

    @Test
    void readerReportsInvalidSbml() {
        InputStream stream = new ByteArrayInputStream("<sbml>broken".getBytes(StandardCharsets.UTF_8));
        SBMLReaderTask task = new SBMLReaderTask(
                stream,
                "broken.xml",
                new NetworkTestSupport().getNetworkFactory(),
                new GroupTestSupport().getGroupFactory());
        TaskMonitor taskMonitor = mock(TaskMonitor.class);

        assertThrows(SBMLReaderError.class, () -> task.run(taskMonitor));

        assertTrue(task.getError());
        verify(taskMonitor).showMessage(eq(TaskMonitor.Level.ERROR), anyString());
        assertEquals(0, task.getNetworks().length);
    }

    @Test
    void readerHonorsTheXmlEncoding() throws Exception {
        String sbml = """
                <?xml version="1.0" encoding="ISO-8859-1"?>
                <sbml xmlns="http://www.sbml.org/sbml/level3/version1/core" level="3" version="1">
                  <model id="m">
                    <listOfCompartments>
                      <compartment id="c" constant="true"/>
                    </listOfCompartments>
                    <listOfSpecies>
                      <species id="s1" name="M\u00e4hren \u00b5M" compartment="c" hasOnlySubstanceUnits="false"
                          boundaryCondition="false" constant="false"/>
                    </listOfSpecies>
                  </model>
                </sbml>
                """;
        InputStream stream = new ByteArrayInputStream(sbml.strip().getBytes(StandardCharsets.ISO_8859_1));
        SBMLReaderTask task = new SBMLReaderTask(
                stream,
                "latin1.xml",
                new NetworkTestSupport().getNetworkFactory(),
                new GroupTestSupport().getGroupFactory());

        task.run(mock(TaskMonitor.class));

        CyNetwork network = task.getNetworks()[0];
        CyNode species = AttributeUtil.getNodeByAttribute(network, SBML.ATTR_ID, "s1");
        assertEquals("M\u00e4hren \u00b5M", network.getRow(species).get(SBML.ATTR_NAME, String.class));
    }

    @Test
    void readerReadsCompModelWithReplacementsInSubmodels() throws Exception {
        String resource = "/models/comp/Watanabe2014/test_replacement_4.xml";
        TaskMonitor taskMonitor = mock(TaskMonitor.class);
        SBMLReaderTask task;
        try (InputStream stream = getClass().getResourceAsStream(resource)) {
            task = new SBMLReaderTask(
                    stream,
                    "test_replacement_4.xml",
                    new NetworkTestSupport().getNetworkFactory(),
                    new GroupTestSupport().getGroupFactory());
            task.run(taskMonitor);
        }

        assertFalse(task.getError());
        assertTrue(task.getNetworks().length > 0);
    }

    /** Model with a core model and three comp ModelDefinitions, i.e. four models. */
    private static final String MODEL_WITH_DEFINITIONS = "/models/unittests/01134-sbml-l3v1.xml";

    private static SBMLReaderTask readerTask(InputStream stream, CyNetworkFactory networkFactory) {
        return new SBMLReaderTask(
                stream, "01134-sbml-l3v1.xml", networkFactory, new GroupTestSupport().getGroupFactory());
    }

    @Test
    void readerReadsAllModels() throws Exception {
        CyNetworkFactory networkFactory = spy(new NetworkTestSupport().getNetworkFactory());
        try (InputStream stream = getClass().getResourceAsStream(MODEL_WITH_DEFINITIONS)) {
            readerTask(stream, networkFactory).run(mock(TaskMonitor.class));
        }

        verify(networkFactory, times(4)).createNetwork();
    }

    @Test
    void cancelStopsReadingBetweenModels() throws Exception {
        CyNetworkFactory networkFactory = spy(new NetworkTestSupport().getNetworkFactory());
        TaskMonitor taskMonitor = mock(TaskMonitor.class);
        try (InputStream stream = getClass().getResourceAsStream(MODEL_WITH_DEFINITIONS)) {
            SBMLReaderTask task = readerTask(stream, networkFactory);
            // cancel after the first model was read
            doAnswer(invocation -> {
                        task.cancel();
                        return null;
                    })
                    .when(taskMonitor)
                    .setProgress(0.4);

            task.run(taskMonitor);

            verify(networkFactory, times(1)).createNetwork();
            assertFalse(task.getError());
            assertEquals(0, task.getNetworks().length);
        }
    }

    @Test
    void failedReaderReturnsNoNetworks() throws Exception {
        TaskMonitor taskMonitor = mock(TaskMonitor.class);
        // fail while reading the second model, after the networks of the first model were created
        AtomicInteger modelsRead = new AtomicInteger();
        doAnswer(invocation -> {
                    if (modelsRead.incrementAndGet() == 2) {
                        throw new IllegalStateException("failure in second model");
                    }
                    return null;
                })
                .when(taskMonitor)
                .setProgress(0.4);

        try (InputStream stream = getClass().getResourceAsStream(MODEL_WITH_DEFINITIONS)) {
            SBMLReaderTask task = readerTask(stream, new NetworkTestSupport().getNetworkFactory());

            assertThrows(SBMLReaderError.class, () -> task.run(taskMonitor));

            assertTrue(task.getError());
            assertEquals(0, task.getNetworks().length);
        }
    }
}
