package org.cy3sbml.layout;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class XMLInterfaceTest {

    @TempDir
    Path tempDir;

    private File write(String xml) throws Exception {
        Path file = tempDir.resolve("layout.xml");
        Files.writeString(file, xml, StandardCharsets.UTF_8);
        return file.toFile();
    }

    @Test
    void skipsBoundingBoxesWithMissingOrInvalidAttributes() throws Exception {
        File file = write("""
                <layout>
                  <listOfBoundingBoxes>
                    <boundingBox id="complete" xpos="1.0" ypos="2.0" height="3.0" width="4.0"/>
                    <boundingBox id="noWidth" xpos="1.0" ypos="2.0" height="3.0"/>
                    <boundingBox xpos="1.0" ypos="2.0" height="3.0" width="4.0"/>
                    <boundingBox id="notANumber" xpos="x" ypos="2.0" height="3.0" width="4.0"/>
                  </listOfBoundingBoxes>
                </layout>
                """);

        List<CyBoundingBox> boxes = XMLInterface.readLayoutFromXML(file);

        assertEquals(List.of(new CyBoundingBox(null, "complete", null, 1.0, 2.0, 3.0, 4.0)), boxes);
    }

    @Test
    void writesAndReadsTheCyIdAndTheSbmlId() throws Exception {
        List<CyBoundingBox> boxes = List.of(
                new CyBoundingBox("meta_S1", "S1", null, 1.0, 2.0, 3.0, 4.0),
                new CyBoundingBox("kineticLaw_R1", null, null, 5.0, 6.0, 7.0, 8.0));
        File file = tempDir.resolve("layout.xml").toFile();

        XMLInterface.writeXMLFileForLayout(file, boxes);

        String xml = Files.readString(file.toPath(), StandardCharsets.UTF_8);
        assertTrue(xml.contains("cyId=\"meta_S1\""), xml);
        assertTrue(xml.contains("id=\"S1\""), xml);
        assertEquals(boxes, XMLInterface.readLayoutFromXML(file));
    }

    /** A layout that cannot be written is an error, not a silently missing file. */
    @Test
    void writingToADirectoryFails() {
        File directory = tempDir.toFile();

        IOException error =
                assertThrows(IOException.class, () -> XMLInterface.writeXMLFileForLayout(directory, List.of()));

        assertTrue(error.getMessage().startsWith("The layout could not be written"), error.getMessage());
    }

    /** The nodes of a layout network (#71) have the glyph, aliases have the same cyId. */
    @Test
    void writesAndReadsTheGlyph() throws Exception {
        List<CyBoundingBox> boxes = List.of(
                new CyBoundingBox("A", "A", "sg_A", 1.0, 2.0, 3.0, 4.0),
                new CyBoundingBox("A", "A", "sg_A2", 5.0, 6.0, 7.0, 8.0));
        File file = tempDir.resolve("layout.xml").toFile();

        XMLInterface.writeXMLFileForLayout(file, boxes);

        String xml = Files.readString(file.toPath(), StandardCharsets.UTF_8);
        assertTrue(xml.contains("glyph=\"sg_A2\""), xml);
        assertEquals(boxes, XMLInterface.readLayoutFromXML(file));
    }

    @Test
    void returnsNoBoxesForMalformedXml() throws Exception {
        assertTrue(XMLInterface.readLayoutFromXML(write("<layout><boundingBox")).isEmpty());
    }

    /** A layout file with an external entity must not leak a local file into node ids. */
    @Test
    void readLayoutFromXMLDoesNotResolveExternalEntities() throws Exception {
        Path secret = tempDir.resolve("secret.txt");
        Files.writeString(secret, "TOP-SECRET", StandardCharsets.UTF_8);
        File layout = tempDir.resolve("layout.xml").toFile();
        Files.writeString(
                layout.toPath(),
                "<?xml version=\"1.0\"?>\n"
                        + "<!DOCTYPE layout [<!ENTITY xxe SYSTEM \"" + secret.toUri() + "\">]>\n"
                        + "<layout><listOfBoundingBoxes>"
                        + "<boundingBox id=\"&xxe;\" xpos=\"1\" ypos=\"2\" height=\"3\" width=\"4\"/>"
                        + "</listOfBoundingBoxes></layout>",
                StandardCharsets.UTF_8);

        List<CyBoundingBox> boxes = XMLInterface.readLayoutFromXML(layout);

        assertTrue(boxes.isEmpty(), "the layout with a DOCTYPE must be rejected: " + boxes);
    }
}
