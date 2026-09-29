package org.cy3sbml.gui;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Optional;
import org.cy3sbml.archive.ArchiveImport;
import org.cy3sbml.archive.ArchiveInfo;
import org.junit.jupiter.api.Test;
import org.sbml.jsbml.SBMLDocument;

/** The info panel section of the COMBINE archive a document was imported from. */
class ArchiveHtmlTest {
    private static final ArchiveImport ARCHIVE = new ArchiveImport(
            new ArchiveInfo(
                    "single.omex",
                    "A <title>",
                    "A reaction & data.",
                    List.of(new ArchiveInfo.Creator("Jane", "Doe", "jane.doe@example.org", "Example University")),
                    List.of(
                            new ArchiveInfo.Entry(
                                    "model.xml",
                                    "http://identifiers.org/combine.specifications/sbml.level-3.version-1",
                                    true),
                            new ArchiveInfo.Entry(
                                    "simulation.sedml",
                                    "http://identifiers.org/combine.specifications/sed-ml.level-1.version-3",
                                    false),
                            new ArchiveInfo.Entry("data/data.csv", "https://purl.org/NET/mediatypes/text/csv", false))),
            "model.xml");

    @Test
    void showsTheArchiveWithItsMetadataAndEntries() {
        String html = ArchiveHtml.create(ARCHIVE);

        assertTrue(html.contains("<span class=\"qualifier\">archive</span> <b>single.omex</b>"), html);
        assertTrue(html.contains("A &lt;title&gt;"), html);
        assertTrue(html.contains("A reaction &amp; data."), html);
        assertTrue(html.contains("Jane Doe, Example University"), html);
        // the imported file is bold
        assertTrue(html.contains("<td><b>model.xml</b></td><td>SBML L3V1, master</td>"), html);
        assertTrue(html.contains("<td>simulation.sedml</td><td>SED-ML L1V3</td>"), html);
        assertTrue(html.contains("<td>data/data.csv</td><td>text/csv</td>"), html);
    }

    @Test
    void emptyMetadataIsLeftOut() {
        ArchiveImport archive = new ArchiveImport(
                new ArchiveInfo("a.omex", "", "", List.of(), ARCHIVE.info().entries()), "model.xml");

        String html = ArchiveHtml.create(archive);

        assertFalse(html.contains("<p></p>"), html);
        assertFalse(html.contains("creator"), html);
    }

    @Test
    void documentInfoContainsTheArchive() throws Exception {
        SBMLDocument document = new SBMLDocument(3, 1);
        document.createModel("m");

        String withArchive = new SBaseHTMLFactory(
                        "file:///app/gui/", null, null, null, null, null, d -> Optional.of(ARCHIVE))
                .createInfo(document);
        String withoutArchive = new SBaseHTMLFactory(
                        "file:///app/gui/", null, null, null, null, null, d -> Optional.empty())
                .createInfo(document);

        assertTrue(withArchive.contains("<span class=\"qualifier\">archive</span>"), withArchive);
        assertFalse(withoutArchive.contains("<span class=\"qualifier\">archive</span>"), withoutArchive);
    }
}
