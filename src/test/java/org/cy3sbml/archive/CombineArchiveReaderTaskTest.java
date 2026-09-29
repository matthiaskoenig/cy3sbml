package org.cy3sbml.archive;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.startsWith;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.cy3sbml.SBML;
import org.cy3sbml.SBMLManager;
import org.cy3sbml.SBMLReaderError;
import org.cy3sbml.comp.ModelResolution;
import org.cy3sbml.reader.SBMLReaderTask;
import org.cytoscape.application.CyApplicationManager;
import org.cytoscape.ding.NetworkViewTestSupport;
import org.cytoscape.group.GroupTestSupport;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNetworkFactory;
import org.cytoscape.model.NetworkTestSupport;
import org.cytoscape.model.subnetwork.CyRootNetwork;
import org.cytoscape.model.subnetwork.CySubNetwork;
import org.cytoscape.view.model.CyNetworkViewFactory;
import org.cytoscape.work.TaskMonitor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.sbml.jsbml.SBMLDocument;

class CombineArchiveReaderTaskTest {
    @TempDir
    Path temp;

    private ArchiveDirectories directories;
    private final CyNetworkFactory networkFactory = new NetworkTestSupport().getNetworkFactory();
    private final TaskMonitor monitor = mock(TaskMonitor.class);

    @BeforeEach
    void setUp() {
        directories = new ArchiveDirectories(temp.resolve("archives"));
    }

    @AfterEach
    void tearDown() {
        directories.deleteAll();
    }

    private CombineArchiveReaderTask task(InputStream stream, String name) {
        return new CombineArchiveReaderTask(
                stream,
                name,
                directories,
                (in, fileName, location) -> new SBMLReaderTask(
                        in, fileName, location, networkFactory, new GroupTestSupport().getGroupFactory()),
                null);
    }

    private CombineArchiveReaderTask read(String resource) throws Exception {
        try (InputStream stream = CombineArchiveReaderTaskTest.class.getResourceAsStream("/models/omex/" + resource)) {
            CombineArchiveReaderTask task = task(stream, resource);
            task.run(monitor);
            return task;
        }
    }

    private static Set<String> collections(CombineArchiveReaderTask task) {
        return Arrays.stream(task.getNetworks())
                .map(n -> ((CySubNetwork) n).getRootNetwork())
                .map(root -> root.getRow(root).get(CyNetwork.NAME, String.class))
                .collect(Collectors.toSet());
    }

    private static String archiveColumn(CyNetwork network) {
        CyRootNetwork root = ((CySubNetwork) network).getRootNetwork();
        return root.getRow(root).get(SBML.ATTR_ARCHIVE, String.class);
    }

    @Test
    void singleArchiveImportsTheMasterModel() throws Exception {
        CombineArchiveReaderTask task = read("single.omex");

        assertEquals(Set.of("single"), collections(task));
        assertEquals(3, task.getNetworks().length);
        for (CyNetwork network : task.getNetworks()) {
            assertEquals("single.omex", archiveColumn(network));
        }
        assertEquals("model.xml", task.getArchiveInfo().modelsToImport().get(0).location());
    }

    @Test
    void compArchiveReadsTheExternalModelFromTheArchive() throws Exception {
        CombineArchiveReaderTask task = read("comp.omex");

        // the main model, the external model definition and the flat model; no collection
        // of its own for models/sub.xml
        assertEquals(Set.of("top", "sub", "Flat__top"), collections(task));
    }

    @Test
    void withoutMasterEverySbmlFileIsImported() throws Exception {
        assertEquals(Set.of("first", "second"), collections(read("no_master.omex")));
    }

    @Test
    void anArchiveWithoutSbmlFails() {
        SBMLReaderError e = assertThrows(SBMLReaderError.class, () -> read("no_sbml.omex"));
        assertEquals("The archive no_sbml.omex contains no SBML file.", e.getMessage());
    }

    @Test
    void aModelThatCannotBeReadIsSkipped() throws Exception {
        String sbml = new String(
                CombineArchiveReaderTaskTest.class
                        .getResourceAsStream("/models/unittests/core_01.xml")
                        .readAllBytes(),
                StandardCharsets.UTF_8);
        String format = "http://identifiers.org/combine.specifications/sbml";
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ZipOutputStream out = new ZipOutputStream(bytes)) {
            for (Map.Entry<String, String> entry : Map.of(
                            "manifest.xml",
                            "<omexManifest xmlns=\"http://identifiers.org/combine.specifications/omex-manifest\">"
                                    + "<content location=\"./good.xml\" format=\"" + format + "\"/>"
                                    + "<content location=\"./broken.xml\" format=\"" + format + "\"/>"
                                    + "</omexManifest>",
                            "good.xml",
                            sbml,
                            "broken.xml",
                            "<sbml")
                    .entrySet()) {
                out.putNextEntry(new ZipEntry(entry.getKey()));
                out.write(entry.getValue().getBytes(StandardCharsets.UTF_8));
                out.closeEntry();
            }
        }
        CombineArchiveReaderTask task = task(new ByteArrayInputStream(bytes.toByteArray()), "mixed.omex");

        task.run(monitor);

        assertTrue(task.getNetworks().length > 0);
        verify(monitor)
                .showMessage(
                        eq(TaskMonitor.Level.ERROR), startsWith("cy3sbml could not read the SBML file 'broken.xml'"));
    }

    private List<Path> leftovers() throws Exception {
        Path root = temp.resolve("archives");
        if (!Files.exists(root)) {
            return List.of();
        }
        try (var paths = Files.list(root)) {
            return paths.toList();
        }
    }

    @Test
    void importLeavesNoFilesBehind() throws Exception {
        read("single.omex");
        read("single.omex");

        assertEquals(List.of(), leftovers());
    }

    @Test
    void failedImportLeavesNoFilesBehind() throws Exception {
        assertThrows(SBMLReaderError.class, () -> read("no_sbml.omex"));

        assertEquals(List.of(), leftovers());
    }

    /**
     * The unpacked files are deleted after the import: the comp resolver the info panel
     * uses already holds the external models of the archive.
     */
    @Test
    void externalModelsResolveAfterTheFilesAreDeleted() throws Exception {
        SBMLManager manager = new SBMLManager(mock(CyApplicationManager.class));
        CyNetworkViewFactory viewFactory = new NetworkViewTestSupport().getNetworkViewFactory();
        CombineArchiveReaderTask task;
        try (InputStream stream = CombineArchiveReaderTaskTest.class.getResourceAsStream("/models/omex/comp.omex")) {
            task = new CombineArchiveReaderTask(
                    stream,
                    "comp.omex",
                    directories,
                    (in, fileName, location) -> new SBMLReaderTask(
                            in,
                            fileName,
                            location,
                            networkFactory,
                            new GroupTestSupport().getGroupFactory(),
                            viewFactory,
                            null,
                            null,
                            null,
                            manager),
                    manager);
            task.run(monitor);
        }
        CyNetwork top = Arrays.stream(task.getNetworks())
                .filter(n -> "All__top".equals(n.getRow(n).get(CyNetwork.NAME, String.class)))
                .findFirst()
                .orElseThrow();
        task.buildCyNetworkView(top);
        assertEquals(List.of(), leftovers());

        SBMLDocument document = manager.getSBMLDocument(top);
        ModelResolution resolution =
                manager.getSBaseRefResolver(document).models().resolve(document, "sub_def");

        assertTrue(resolution instanceof ModelResolution.Resolved, resolution.toString());
    }

    @Test
    void deleteAllRemovesTheExtractedFiles() throws Exception {
        read("single.omex");

        directories.deleteAll();

        assertTrue(Files.notExists(temp.resolve("archives")));
    }
}
