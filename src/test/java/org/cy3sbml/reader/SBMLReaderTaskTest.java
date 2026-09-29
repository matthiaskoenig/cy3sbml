package org.cy3sbml.reader;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.cy3sbml.SBML;
import org.cy3sbml.SBMLReaderError;
import org.cy3sbml.util.AttributeUtil;
import org.cytoscape.group.GroupTestSupport;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNetworkFactory;
import org.cytoscape.model.CyNode;
import org.cytoscape.model.NetworkTestSupport;
import org.cytoscape.model.subnetwork.CyRootNetwork;
import org.cytoscape.model.subnetwork.CySubNetwork;
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

        SBMLReaderError error = assertThrows(SBMLReaderError.class, () -> task.run(taskMonitor));

        assertTrue(task.getError());
        assertEquals(0, task.getNetworks().length);
        // Cytoscape reports the thrown error, a task monitor message would show it twice
        verify(taskMonitor, never()).showMessage(eq(TaskMonitor.Level.ERROR), anyString());
        String message = error.getMessage();
        assertTrue(message.startsWith("cy3sbml could not read the SBML file 'broken.xml': "), message);
        assertTrue(message.contains("line 1"), message);
        assertFalse(message.contains("com.ctc.wstx"), message);
        assertFalse(message.contains("\n"), message);
        assertTrue(message.contains("https://sbml.org/facilities/validator/"), message);
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
    void nodesHaveTheSbmlNameAsNameInEverySubnetwork() throws Exception {
        String sbml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <sbml xmlns="http://www.sbml.org/sbml/level3/version1/core" level="3" version="1">
                  <model id="m">
                    <listOfCompartments>
                      <compartment id="c" name="cytosol" constant="true"/>
                    </listOfCompartments>
                    <listOfSpecies>
                      <species id="s1" name="glucose" compartment="c" hasOnlySubstanceUnits="false"
                          boundaryCondition="false" constant="false"/>
                    </listOfSpecies>
                  </model>
                </sbml>
                """;
        InputStream stream = new ByteArrayInputStream(sbml.strip().getBytes(StandardCharsets.UTF_8));
        SBMLReaderTask task = new SBMLReaderTask(
                stream,
                "names.xml",
                new NetworkTestSupport().getNetworkFactory(),
                new GroupTestSupport().getGroupFactory());

        task.run(mock(TaskMonitor.class));

        assertEquals(3, task.getNetworks().length);
        for (CyNetwork network : task.getNetworks()) {
            CyNode species = AttributeUtil.getNodeByAttribute(network, SBML.ATTR_ID, "s1");
            String networkName = network.getRow(network).get(CyNetwork.NAME, String.class);
            assertEquals("glucose", network.getRow(species).get(CyNetwork.NAME, String.class), networkName);
        }
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

    /** Reads the given model without id, from an input with the given name, and returns the network names. */
    private static List<String> networkNamesOfModelWithoutId(String inputName) throws Exception {
        String sbml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <sbml xmlns="http://www.sbml.org/sbml/level3/version1/core" level="3" version="1">
                  <model>
                    <listOfCompartments>
                      <compartment id="c" constant="true"/>
                    </listOfCompartments>
                  </model>
                </sbml>
                """;
        SBMLReaderTask task = new SBMLReaderTask(
                new ByteArrayInputStream(sbml.strip().getBytes(StandardCharsets.UTF_8)),
                inputName,
                new NetworkTestSupport().getNetworkFactory(),
                new GroupTestSupport().getGroupFactory());
        task.run(mock(TaskMonitor.class));

        CyNetwork network = task.getNetworks()[0];
        CyRootNetwork root = ((CySubNetwork) network).getRootNetwork();
        List<String> names = new ArrayList<>();
        names.add(root.getRow(root).get(CyNetwork.NAME, String.class));
        for (CyNetwork subnetwork : task.getNetworks()) {
            names.add(subnetwork.getRow(subnetwork).get(CyNetwork.NAME, String.class));
        }
        return names;
    }

    @Test
    void modelWithoutIdIsNamedByTheFileName() throws Exception {
        List<String> expected = List.of("model.xml", "All__model.xml", "Kinetic__model.xml", "model.xml");

        // Import > Network from File passes the file name
        assertEquals(expected, networkNamesOfModelWithoutId("model.xml"));
        // Import > Network from URL passes the URL
        assertEquals(expected, networkNamesOfModelWithoutId("https://example.org/models/model.xml"));
        assertEquals(expected, networkNamesOfModelWithoutId("file:/home/user/models/model.xml"));
        assertEquals(expected, networkNamesOfModelWithoutId("C:\\Users\\user\\model.xml"));
    }

    @Test
    void fileNameOfInputName() {
        assertEquals("model.xml", SubnetworkBuilder.fileName("model.xml"));
        assertEquals("model.xml", SubnetworkBuilder.fileName("/home/user/models/model.xml"));
        assertEquals("model.xml", SubnetworkBuilder.fileName("file:/home/user/my%20models/model.xml"));
        assertEquals("my model.xml", SubnetworkBuilder.fileName("file:/home/user/my%20model.xml"));
        assertEquals("BIOMD1", SubnetworkBuilder.fileName("https://example.org/model/download/BIOMD1?filename=x.xml"));
        assertEquals("model.xml", SubnetworkBuilder.fileName("C:\\Users\\user\\model.xml"));
        assertEquals("https://example.org/", SubnetworkBuilder.fileName("https://example.org/"));
        assertEquals("", SubnetworkBuilder.fileName(null));
    }
}
