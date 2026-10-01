package org.cy3sbml.archive;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.cy3sbml.comp.CompModels;
import org.cy3sbml.reader.SBMLReaderTask;
import org.cytoscape.group.GroupTestSupport;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.NetworkTestSupport;
import org.cytoscape.work.TaskMonitor;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.sbml.jsbml.SBMLDocument;
import org.sbml.jsbml.SBMLReader;
import org.sbml.jsbml.ext.comp.CompConstants;
import org.sbml.jsbml.ext.comp.CompSBMLDocumentPlugin;
import org.sbml.jsbml.ext.comp.ExternalModelDefinition;

class CombineArchiveWriterTest {
    @TempDir
    Path temp;

    /** Reads the test model with its location set, as the reader does. */
    private static SBMLDocument read(String resource) throws Exception {
        File file =
                new File(CombineArchiveWriterTest.class.getResource(resource).toURI());
        SBMLDocument document = SBMLReader.read(file);
        document.setLocationURI(file.toURI().toString());
        return document;
    }

    /** The archive of the document at the location, extracted into a new directory. */
    private ArchiveInfo archive(SBMLDocument document, String location) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        String master = CombineArchiveWriter.write(document, location, new CompModels(document), out);
        assertEquals("./" + location, master);
        return CombineArchive.extract(
                new ByteArrayInputStream(out.toByteArray()), "model.omex", temp.resolve("extracted"));
    }

    private static Set<String> locations(ArchiveInfo info) {
        return info.entries().stream().map(ArchiveInfo.Entry::location).collect(Collectors.toSet());
    }

    @Test
    void writesTheDocumentAsMasterEntry() throws Exception {
        SBMLDocument document = read("/models/unittests/core_01.xml");

        ArchiveInfo info = archive(document, "model.xml");

        assertEquals(1, info.entries().size(), info.entries().toString());
        ArchiveInfo.Entry entry = info.entries().get(0);
        assertEquals("model.xml", entry.location());
        assertTrue(entry.master());
        assertTrue(ArchiveInfo.isSbml(entry.format()), entry.format());
        assertTrue(entry.format().endsWith("sbml.level-" + document.getLevel() + ".version-" + document.getVersion()));
    }

    /**
     * The external model definitions of top.xml name file:ext.xml, sub/ext2.xml (which names
     * ../ext.xml) and missing.xml, which does not exist.
     */
    @Test
    void writesTheExternalModelsAtTheirSources() throws Exception {
        SBMLDocument document = read("/models/comp/unit/top.xml");

        ArchiveInfo info = archive(document, "top.xml");

        assertEquals(Set.of("top.xml", "ext.xml", "sub/ext2.xml"), locations(info));
        assertEquals(
                List.of("top.xml"),
                info.entries().stream()
                        .filter(ArchiveInfo.Entry::master)
                        .map(ArchiveInfo.Entry::location)
                        .toList());
        assertTrue(info.entries().stream().allMatch(e -> ArchiveInfo.isSbml(e.format())));
    }

    /** The archive of a comp model imports with its external models resolved, like the file. */
    @Test
    void importedArchiveResolvesTheExternalModels() throws Exception {
        SBMLDocument document = read("/models/comp/koenig-toymodel/toy_top_level.xml");
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        CombineArchiveWriter.write(document, "toy_top_level.xml", new CompModels(document), out);

        CombineArchiveReaderTask task = new CombineArchiveReaderTask(
                new ByteArrayInputStream(out.toByteArray()),
                "toy.omex",
                new ArchiveDirectories(temp.resolve("archives")),
                (stream, name, location) -> new SBMLReaderTask(
                        stream,
                        name,
                        location,
                        new NetworkTestSupport().getNetworkFactory(),
                        new GroupTestSupport().getGroupFactory()),
                null);
        task.run(mock(TaskMonitor.class));

        Set<String> names = java.util.Arrays.stream(task.getNetworks())
                .map(network -> network.getRow(network).get(CyNetwork.NAME, String.class))
                .collect(Collectors.toSet());
        // the main model, the four external models and the flat model, each with its networks
        assertTrue(names.contains("toy_top_level"), names.toString());
        assertTrue(names.stream().anyMatch(n -> n.startsWith("toy_fba")), names.toString());
        assertTrue(names.stream().anyMatch(n -> n.startsWith("Flat__")), names.toString());
    }

    /** A source outside the archive, or a url, is left out: its model is not resolved. */
    @Test
    void leavesOutSourcesOutsideTheArchive() throws Exception {
        SBMLDocument document = read("/models/comp/unit/ext.xml");
        CompSBMLDocumentPlugin plugin = (CompSBMLDocumentPlugin) document.getPlugin(CompConstants.shortLabel);
        ExternalModelDefinition outside = plugin.createExternalModelDefinition("outside");
        outside.setSource("../top.xml");
        // nothing listens on port 1, the connection is refused immediately
        ExternalModelDefinition remote = plugin.createExternalModelDefinition("remote");
        remote.setSource("http://127.0.0.1:1/x.xml");

        ArchiveInfo info = archive(document, "ext.xml");

        assertEquals(Set.of("ext.xml"), locations(info));
    }

    /**
     * Every collection of an archive import has the archive import of the master entry, so the
     * location of a document of the archive is the entry of its own file.
     */
    @Test
    void masterLocationOfADocumentOfAnArchive() throws Exception {
        ArchiveImport imported = new ArchiveImport(
                new ArchiveInfo(
                        "a.omex",
                        null,
                        null,
                        List.of(),
                        List.of(
                                new ArchiveInfo.Entry("top.xml", "sbml", true),
                                new ArchiveInfo.Entry("lib/sub.xml", "sbml", false))),
                "top.xml");
        SBMLDocument master = new SBMLDocument(3, 1);
        master.setLocationURI("file:/tmp/archive-1/top.xml");
        SBMLDocument external = new SBMLDocument(3, 1);
        external.setLocationURI("file:/tmp/archive-1/lib/sub.xml");

        assertEquals("top.xml", CombineArchiveWriter.masterLocation(master, Optional.of(imported)));
        assertEquals("lib/sub.xml", CombineArchiveWriter.masterLocation(external, Optional.of(imported)));
    }

    @Test
    void masterLocation() throws Exception {
        SBMLDocument document = new SBMLDocument(3, 1);
        assertEquals("model.xml", CombineArchiveWriter.masterLocation(document, Optional.empty()));
        document.setLocationURI("file:/tmp/m1.xml");
        assertEquals("m1.xml", CombineArchiveWriter.masterLocation(document, Optional.empty()));
        document.setLocationURI("file:/tmp/a%20b.xml");
        assertEquals("model.xml", CombineArchiveWriter.masterLocation(document, Optional.empty()));
        // the document of an archive import is the unpacked file of its entry
        ArchiveImport imported = new ArchiveImport(
                new ArchiveInfo(
                        "a.omex",
                        null,
                        null,
                        List.of(),
                        List.of(new ArchiveInfo.Entry("./models/m1.xml", "sbml", true))),
                "./models/m1.xml");
        document.setLocationURI("file:/tmp/archive-2/models/m1.xml");
        assertEquals("models/m1.xml", CombineArchiveWriter.masterLocation(document, Optional.of(imported)));
        // without location, the entry of the import
        assertEquals(
                "models/m1.xml", CombineArchiveWriter.masterLocation(new SBMLDocument(3, 1), Optional.of(imported)));
    }
}
