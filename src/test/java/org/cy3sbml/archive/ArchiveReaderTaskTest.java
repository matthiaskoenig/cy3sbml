package org.cy3sbml.archive;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNetworkFactory;
import org.cytoscape.model.NetworkTestSupport;
import org.cytoscape.work.TaskMonitor;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class ArchiveReaderTaskTest {

    @ParameterizedTest
    @CsvSource({
        "/studies/s1/, study",
        "/models/m1/, model",
        "/assays/a1/, assay",
        "studies/s1/, study",
        "./models/m1/, model",
        "/studies/, folder",
        "/studies/s1/data/, folder",
        "/other/x/, folder",
        "/other/, folder",
    })
    void folderImageFollowsTheParentFolderType(String path, String extension) {
        assertEquals(extension, ArchiveReaderTask.folderExtension(path));
    }

    @Test
    void runCreatesSingleNetworkNamedAfterTheArchiveFile() throws Exception {
        CyNetworkFactory networkFactory = new NetworkTestSupport().getNetworkFactory();
        ArchiveReaderTask task = new ArchiveReaderTask(
                new ByteArrayInputStream(new byte[0]), "myArchive.omex", networkFactory, null, null, null);

        task.run(mock(TaskMonitor.class));

        CyNetwork[] networks = task.getNetworks();
        assertEquals(1, networks.length);
        CyNetwork network = networks[0];
        assertEquals("myArchive.omex Content", network.getRow(network).get(CyNetwork.NAME, String.class));
    }

    /**
     * Reading the archive manifest and content (and therefore extracting any entry to
     * disk) is not implemented yet, see issue #116: {@link ArchiveReaderTask#run} never
     * reads the given {@code InputStream} at all. There is consequently no extraction
     * code path to defend against zip-slip today.
     * <p>
     * This test documents that and guards the current, safe behavior: running the task
     * with an archive that contains a "../" entry name must not throw, must still produce
     * exactly the one (empty) network the task always produces, and - since nothing is
     * extracted - must not write any file into a fresh, otherwise empty directory. Once
     * #116 adds real extraction, this test should be replaced by one that unzips into a
     * target directory and asserts the "../" entry is rejected or kept inside it.
     */
    @Test
    void hostileEntryNameDoesNotEscapeBecauseNothingIsExtractedYet(@TempDir Path tempDir) throws Exception {
        byte[] archive = zipOf(Map.of("manifest.xml", "<manifest/>", "../evil.xml", "<sbml/>"));
        CyNetworkFactory networkFactory = new NetworkTestSupport().getNetworkFactory();
        ArchiveReaderTask task = new ArchiveReaderTask(
                new ByteArrayInputStream(archive), "hostile.omex", networkFactory, null, null, null);

        assertDoesNotThrow(() -> task.run(mock(TaskMonitor.class)));

        assertEquals(1, task.getNetworks().length);
        try (var files = Files.list(tempDir)) {
            assertTrue(files.findAny().isEmpty(), "no file should have been written since extraction is unimplemented");
        }
    }

    private static byte[] zipOf(Map<String, String> entries) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(out)) {
            for (Map.Entry<String, String> entry : entries.entrySet()) {
                zip.putNextEntry(new ZipEntry(entry.getKey()));
                zip.write(entry.getValue().getBytes(StandardCharsets.UTF_8));
                zip.closeEntry();
            }
        }
        return out.toByteArray();
    }
}
