package org.cy3sbml.archive;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class CombineArchiveTest {
    private static final String SBML_L3V1 = "http://identifiers.org/combine.specifications/sbml.level-3.version-1";

    @TempDir
    Path directory;

    private ArchiveInfo extract(String resource) throws Exception {
        try (InputStream stream = CombineArchiveTest.class.getResourceAsStream("/models/omex/" + resource)) {
            return CombineArchive.extract(stream, resource, directory);
        }
    }

    /** A zip with the given entries (name -> content). */
    private static InputStream zip(Map<String, String> entries) throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ZipOutputStream out = new ZipOutputStream(bytes)) {
            for (Map.Entry<String, String> entry : entries.entrySet()) {
                out.putNextEntry(new ZipEntry(entry.getKey()));
                out.write(entry.getValue().getBytes(StandardCharsets.UTF_8));
                out.closeEntry();
            }
        }
        return new ByteArrayInputStream(bytes.toByteArray());
    }

    private static String manifest(String... contents) {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                + "<omexManifest xmlns=\"http://identifiers.org/combine.specifications/omex-manifest\">\n"
                + String.join("\n", contents) + "\n</omexManifest>\n";
    }

    @Test
    void singleArchiveHasItsEntriesMetadataAndMaster() throws Exception {
        ArchiveInfo info = extract("single.omex");

        assertEquals("single.omex", info.name());
        // the archive itself (".") and the manifest are no entries
        assertEquals(
                List.of("model.xml", "simulation.sedml", "data/data.csv", "metadata.rdf"),
                info.entries().stream().map(ArchiveInfo.Entry::location).toList());
        assertEquals(List.of(new ArchiveInfo.Entry("model.xml", SBML_L3V1, true)), info.modelsToImport());
        assertEquals("A reaction with a simulation and data.", info.description());
        assertEquals(
                List.of(new ArchiveInfo.Creator("Jane", "Doe", "jane.doe@example.org", "Example University")),
                info.creators());
        assertTrue(Files.isRegularFile(directory.resolve("model.xml")));
        assertTrue(Files.isRegularFile(directory.resolve("data/data.csv")));
    }

    @Test
    void withoutMasterAllSbmlFilesAreImported() throws Exception {
        ArchiveInfo info = extract("no_master.omex");

        assertEquals(
                List.of("first.xml", "second.xml"),
                info.modelsToImport().stream().map(ArchiveInfo.Entry::location).toList());
    }

    @Test
    void compArchiveImportsOnlyTheMaster() throws Exception {
        ArchiveInfo info = extract("comp.omex");

        assertEquals(
                List.of("top.xml"),
                info.modelsToImport().stream().map(ArchiveInfo.Entry::location).toList());
        assertTrue(Files.isRegularFile(directory.resolve("models/sub.xml")));
    }

    @Test
    void bioModelsArchive() throws Exception {
        ArchiveInfo info = extract("BIOMD0000000012.omex");

        assertEquals(
                List.of("BIOMD0000000012_url.xml"),
                info.modelsToImport().stream().map(ArchiveInfo.Entry::location).toList());
        assertEquals("A synthetic oscillatory network of transcriptional regulators", info.title());
        // the XHTML description is plain text
        assertTrue(info.description().startsWith("This model describes the deterministic version"), info.description());
        assertFalse(info.description().contains("<"), info.description());
        assertEquals(List.of(), info.creators());
    }

    @Test
    void archiveWithoutSbmlIsAnError() {
        CombineArchiveException e = assertThrows(CombineArchiveException.class, () -> extract("no_sbml.omex"));
        assertEquals("The archive no_sbml.omex contains no SBML file.", e.getMessage());
    }

    @Test
    void notAZipIsAnError() {
        CombineArchiveException e = assertThrows(
                CombineArchiveException.class,
                () -> CombineArchive.extract(
                        new ByteArrayInputStream("hello".getBytes(StandardCharsets.UTF_8)), "a.omex", directory));
        assertEquals("The file a.omex is not a COMBINE archive: it is no zip file.", e.getMessage());
    }

    @Test
    void zipWithoutManifestIsAnError() throws Exception {
        InputStream stream = zip(Map.of("model.xml", "<sbml/>"));

        CombineArchiveException e =
                assertThrows(CombineArchiveException.class, () -> CombineArchive.extract(stream, "a.omex", directory));
        assertEquals("The file a.omex is not a COMBINE archive: it has no manifest.xml.", e.getMessage());
    }

    @Test
    void entryOutsideTheArchiveIsRejected() throws Exception {
        Map<String, String> entries = new LinkedHashMap<>();
        entries.put("manifest.xml", manifest());
        entries.put("../evil.xml", "<sbml/>");
        Path extraction = directory.resolve("archive");

        CombineArchiveException e = assertThrows(
                CombineArchiveException.class, () -> CombineArchive.extract(zip(entries), "a.omex", extraction));
        assertEquals("The archive a.omex has the entry '../evil.xml' outside the archive.", e.getMessage());
        assertFalse(Files.exists(directory.resolve("evil.xml")));
    }

    @Test
    void manifestLocationMissingInTheZipIsAnError() throws Exception {
        InputStream stream = zip(Map.of(
                "manifest.xml",
                manifest("<content location=\"./model.xml\" format=\"" + SBML_L3V1 + "\" master=\"true\"/>")));

        CombineArchiveException e =
                assertThrows(CombineArchiveException.class, () -> CombineArchive.extract(stream, "a.omex", directory));
        assertEquals("The archive a.omex has no file model.xml, which its manifest lists.", e.getMessage());
    }

    @Test
    void sbmlFormats() {
        assertTrue(ArchiveInfo.isSbml("http://identifiers.org/combine.specifications/sbml"));
        assertTrue(ArchiveInfo.isSbml("https://identifiers.org/combine.specifications/sbml.level-3.version-2"));
        assertTrue(ArchiveInfo.isSbml("application/sbml+xml"));
        assertTrue(ArchiveInfo.isSbml("http://purl.org/NET/mediatypes/application/sbml+xml"));
        assertFalse(ArchiveInfo.isSbml("http://identifiers.org/combine.specifications/sed-ml"));
    }

    @Test
    void formatLabels() {
        assertEquals("SBML L3V1", ArchiveInfo.formatLabel(SBML_L3V1));
        assertEquals("SBML", ArchiveInfo.formatLabel("http://identifiers.org/combine.specifications/sbml"));
        assertEquals(
                "SED-ML L1V3",
                ArchiveInfo.formatLabel("http://identifiers.org/combine.specifications/sed-ml.level-1.version-3"));
        assertEquals(
                "OMEX metadata",
                ArchiveInfo.formatLabel("http://identifiers.org/combine.specifications/omex-metadata"));
        assertEquals("text/csv", ArchiveInfo.formatLabel("https://purl.org/NET/mediatypes/text/csv"));
        assertEquals("image/png", ArchiveInfo.formatLabel("http://purl.org/NET/mediatypes/image/png"));
    }

    @Test
    void manifestLocationOutsideTheArchiveIsRejected() throws Exception {
        Files.writeString(directory.resolve("outside.xml"), "<sbml/>");
        InputStream stream = zip(Map.of(
                "manifest.xml",
                manifest("<content location=\"../outside.xml\" format=\"" + SBML_L3V1 + "\" master=\"true\"/>")));

        CombineArchiveException e = assertThrows(
                CombineArchiveException.class,
                () -> CombineArchive.extract(stream, "a.omex", directory.resolve("archive")));
        assertEquals("The archive a.omex has the entry '../outside.xml' outside the archive.", e.getMessage());
    }

    @Test
    void malformedXhtmlDescriptionIsReducedToItsText() {
        assertEquals("A model of glycolysis", CombineArchive.description("<p>A <b>model</p> of glycolysis"));
    }

    @Test
    void malformedManifestIsAnError() throws Exception {
        InputStream stream = zip(Map.of("manifest.xml", "<omexManifest><content"));

        CombineArchiveException e =
                assertThrows(CombineArchiveException.class, () -> CombineArchive.extract(stream, "a.omex", directory));
        assertTrue(e.getMessage().startsWith("The manifest of the archive a.omex cannot be read"), e.getMessage());
    }
}
