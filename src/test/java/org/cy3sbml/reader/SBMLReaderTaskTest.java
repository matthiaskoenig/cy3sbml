package org.cy3sbml.reader;

import static org.cytoscape.view.presentation.property.BasicVisualLexicon.NODE_X_LOCATION;
import static org.cytoscape.view.presentation.property.BasicVisualLexicon.NODE_Y_LOCATION;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import org.cy3sbml.SBML;
import org.cy3sbml.SBMLManager;
import org.cy3sbml.SBMLReaderError;
import org.cy3sbml.util.AttributeUtil;
import org.cytoscape.application.CyApplicationManager;
import org.cytoscape.ding.NetworkViewTestSupport;
import org.cytoscape.group.GroupTestSupport;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNetworkFactory;
import org.cytoscape.model.CyNode;
import org.cytoscape.model.CyRow;
import org.cytoscape.model.NetworkTestSupport;
import org.cytoscape.model.subnetwork.CyRootNetwork;
import org.cytoscape.model.subnetwork.CySubNetwork;
import org.cytoscape.property.CyProperty;
import org.cytoscape.view.layout.CyLayoutAlgorithm;
import org.cytoscape.view.layout.CyLayoutAlgorithmManager;
import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.view.model.View;
import org.cytoscape.view.vizmap.VisualMappingManager;
import org.cytoscape.view.vizmap.VisualStyle;
import org.cytoscape.work.Task;
import org.cytoscape.work.TaskIterator;
import org.cytoscape.work.TaskMonitor;
import org.junit.jupiter.api.Test;
import org.sbml.jsbml.SBMLDocument;

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

        // the main model, the three model definitions and the flat model
        verify(networkFactory, times(5)).createNetwork();
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
        List<String> expected = List.of("model.xml", "model.xml", "model.xml__kinetic", "model.xml__all");

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

    /** Reads the resource with its location, as Cytoscape does for a file, and returns the task. */
    private static SBMLReaderTask readWithLocation(String resource, boolean withLocation) throws Exception {
        URL url = SBMLReaderTaskTest.class.getResource(resource);
        String fileName = resource.substring(resource.lastIndexOf('/') + 1);
        SBMLReaderTask task;
        try (InputStream stream = url.openStream()) {
            task = new SBMLReaderTask(
                    stream,
                    fileName,
                    withLocation ? url.toURI() : null,
                    new NetworkTestSupport().getNetworkFactory(),
                    new GroupTestSupport().getGroupFactory());
            task.run(mock(TaskMonitor.class));
        }
        assertFalse(task.getError());
        return task;
    }

    /** The names of the base networks, one per network collection. */
    private static List<String> collectionNames(SBMLReaderTask task) {
        return Arrays.stream(task.getNetworks())
                .map(network -> ((CySubNetwork) network).getRootNetwork())
                .distinct()
                .map(root -> root.getRow(root).get(CyNetwork.NAME, String.class))
                .toList();
    }

    private static final String TOY_TOP_LEVEL = "/models/comp/koenig-toymodel/toy_top_level.xml";

    @Test
    void readerCreatesNetworksOfExternalAndFlatModels() throws Exception {
        SBMLReaderTask task = readWithLocation(TOY_TOP_LEVEL, true);

        assertEquals(
                List.of(
                        "toy_top_level",
                        "toy_ode_bounds",
                        "toy_fba",
                        "toy_ode_update",
                        "toy_ode_model",
                        "Flat__toy_top_level"),
                collectionNames(task));
        assertEquals(18, task.getNetworks().length);
    }

    /** The flat network has the species of the libSBML flattening. */
    @Test
    void flatNetworkHasTheSpeciesOfTheFlatModel() throws Exception {
        SBMLReaderTask task = readWithLocation(TOY_TOP_LEVEL, true);
        JsonNode reference;
        try (InputStream stream = getClass().getResourceAsStream("/models/comp/comp-flat-reference.json")) {
            reference = new ObjectMapper().readTree(stream);
        }
        List<String> expected = new ArrayList<>();
        reference.get("koenig-toymodel/toy_top_level.xml").get("species").forEach(id -> expected.add(id.asText()));

        CyNetwork flat = Arrays.stream(task.getNetworks())
                .filter(n -> "Flat__toy_top_level__all".equals(n.getRow(n).get(CyNetwork.NAME, String.class)))
                .findFirst()
                .orElseThrow();
        List<String> species = flat.getNodeList().stream()
                .filter(n -> SBML.NODETYPE_SPECIES.equals(flat.getRow(n).get(SBML.NODETYPE_ATTR, String.class)))
                .map(n -> flat.getRow(n).get(SBML.ATTR_ID, String.class))
                .sorted()
                .toList();
        assertEquals(expected, species);
    }

    /** Without the location of the file, the external models are not found, and there is no flat model. */
    @Test
    void readerWithoutLocationCreatesOnlyTheMainNetwork() throws Exception {
        SBMLReaderTask task = readWithLocation(TOY_TOP_LEVEL, false);

        assertEquals(List.of("toy_top_level"), collectionNames(task));
    }

    @Test
    void missingExternalFileSkipsOnlyItsNetwork() throws Exception {
        SBMLReaderTask task = readWithLocation("/models/comp/unit/top.xml", true);

        // top.xml refers to missing.xml, which no submodel uses, and to ext.xml and sub/ext2.xml
        // several times
        assertEquals(List.of("top", "local", "ext_main", "inner", "Flat__top"), collectionNames(task));
    }

    /** inst_a.xml and inst_b.xml instantiate each other: both networks, no flat network. */
    @Test
    void instantiationCycleThroughFilesSkipsTheFlatNetwork() throws Exception {
        SBMLReaderTask task = readWithLocation("/models/comp/unit/inst_a.xml", true);

        assertEquals(List.of("inst_a", "inst_b"), collectionNames(task));
    }

    @Test
    void submodelThatCannotBeInstantiatedSkipsTheFlatNetwork() throws Exception {
        SBMLReaderTask task = readWithLocation("/models/comp/unit/cycle_a.xml", true);

        assertEquals(List.of("cycle_a_main"), collectionNames(task));
    }

    @Test
    void documentWithOnlyModelDefinitionsCreatesTheirNetworks() throws Exception {
        String sbml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <sbml xmlns="http://www.sbml.org/sbml/level3/version1/core"
                      xmlns:comp="http://www.sbml.org/sbml/level3/version1/comp/version1"
                      level="3" version="1" comp:required="true">
                  <comp:listOfModelDefinitions>
                    <comp:modelDefinition id="definition">
                      <listOfCompartments>
                        <compartment id="c" constant="true"/>
                      </listOfCompartments>
                    </comp:modelDefinition>
                  </comp:listOfModelDefinitions>
                </sbml>
                """;
        SBMLReaderTask task = new SBMLReaderTask(
                new ByteArrayInputStream(sbml.strip().getBytes(StandardCharsets.UTF_8)),
                "definitions.xml",
                new NetworkTestSupport().getNetworkFactory(),
                new GroupTestSupport().getGroupFactory());
        task.run(mock(TaskMonitor.class));

        assertFalse(task.getError());
        assertEquals(List.of("definition"), collectionNames(task));
    }

    /** Every network is registered with the document of its model. */
    @Test
    void networksAreRegisteredWithTheDocumentOfTheirModel() throws Exception {
        URL url = getClass().getResource(TOY_TOP_LEVEL);
        SBMLManager sbmlManager = new SBMLManager(mock(CyApplicationManager.class));
        SBMLReaderTask task;
        try (InputStream stream = url.openStream()) {
            task = new SBMLReaderTask(
                    stream,
                    "toy_top_level.xml",
                    url.toURI(),
                    new NetworkTestSupport().getNetworkFactory(),
                    new GroupTestSupport().getGroupFactory(),
                    new NetworkViewTestSupport().getNetworkViewFactory(),
                    null,
                    null,
                    null,
                    sbmlManager);
            task.run(mock(TaskMonitor.class));
        }
        for (CyNetwork network : task.getNetworks()) {
            task.buildCyNetworkView(network);
        }

        Map<String, String> documentOfNetwork = new HashMap<>();
        for (CyNetwork network : task.getNetworks()) {
            SBMLDocument document = sbmlManager.getSBMLDocument(network);
            String file = document.getLocationURI()
                    .substring(document.getLocationURI().lastIndexOf('/') + 1);
            documentOfNetwork.put(
                    network.getRow(network).get(CyNetwork.NAME, String.class),
                    file + " " + document.isPackageEnabled("comp"));
        }
        assertEquals("toy_top_level.xml true", documentOfNetwork.get("toy_top_level"));
        assertEquals("toy_fba.xml true", documentOfNetwork.get("toy_fba"));
        // the flat model is in its own document, without the comp package
        assertEquals("toy_top_level.xml false", documentOfNetwork.get("Flat__toy_top_level"));
    }

    /**
     * The view of a layout network (#71) has the nodes at the glyph positions and the layout
     * style, without the force-directed layout of the other views.
     */
    @Test
    void layoutNetworkViewsHaveTheGlyphPositionsAndTheLayoutStyle() throws Exception {
        CyLayoutAlgorithm algorithm = mock(CyLayoutAlgorithm.class);
        List<CyNetworkView> laidOut = new ArrayList<>();
        when(algorithm.createTaskIterator(any(), any(), any(), any())).thenAnswer(invocation -> {
            laidOut.add(invocation.getArgument(0));
            return new TaskIterator(mock(Task.class));
        });
        CyLayoutAlgorithmManager layoutManager = mock(CyLayoutAlgorithmManager.class);
        when(layoutManager.getLayout(anyString())).thenReturn(algorithm);
        VisualStyle style = style(SBML.STYLE_CY3SBML);
        VisualStyle layoutStyle = style(SBML.STYLE_CY3SBML + SBML.STYLE_SUFFIX_LAYOUT);
        VisualMappingManager vmm = mock(VisualMappingManager.class);
        when(vmm.getAllVisualStyles()).thenReturn(Set.of(style, layoutStyle));
        Properties properties = new Properties();
        properties.setProperty(SBML.PROPERTY_VISUAL_STYLE, SBML.STYLE_CY3SBML);
        @SuppressWarnings("unchecked")
        CyProperty<Properties> cyProperty = mock(CyProperty.class);
        when(cyProperty.getProperties()).thenReturn(properties);

        SBMLReaderTask task;
        try (InputStream stream = getClass().getResourceAsStream("/models/unittests/layout_02.xml")) {
            task = new SBMLReaderTask(
                    stream,
                    "layout_02.xml",
                    null,
                    new NetworkTestSupport().getNetworkFactory(),
                    new GroupTestSupport().getGroupFactory(),
                    new NetworkViewTestSupport().getNetworkViewFactory(),
                    vmm,
                    layoutManager,
                    cyProperty,
                    null);
            task.run(mock(TaskMonitor.class));
        }
        Map<String, CyNetworkView> views = new HashMap<>();
        for (CyNetwork network : task.getNetworks()) {
            views.put(network.getRow(network).get(CyNetwork.NAME, String.class), task.buildCyNetworkView(network));
        }

        CyNetworkView layoutView = views.get("layout_02__layout_layout1");
        assertFalse(laidOut.contains(layoutView));
        assertTrue(laidOut.contains(views.get("layout_02")));
        verify(vmm).setVisualStyle(layoutStyle, layoutView);
        verify(vmm).setVisualStyle(style, views.get("layout_02"));
        // the layout task does not apply the style to the views of the layout networks
        verify(layoutStyle).apply(layoutView);
        verify(style).apply(views.get("layout_02"));
        CyNetwork layout = layoutView.getModel();
        for (CyNode node : layout.getNodeList()) {
            View<CyNode> nodeView = layoutView.getNodeView(node);
            CyRow row = layout.getRow(node);
            assertEquals(row.get(SBML.ATTR_LAYOUT_X, Double.class), nodeView.getVisualProperty(NODE_X_LOCATION));
            assertEquals(row.get(SBML.ATTR_LAYOUT_Y, Double.class), nodeView.getVisualProperty(NODE_Y_LOCATION));
        }
    }

    private static VisualStyle style(String title) {
        VisualStyle style = mock(VisualStyle.class);
        when(style.getTitle()).thenReturn(title);
        return style;
    }
}
