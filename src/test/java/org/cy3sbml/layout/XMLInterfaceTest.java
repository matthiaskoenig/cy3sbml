package org.cy3sbml.layout;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
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

        Map<String, CyBoundingBox> boxes = XMLInterface.readLayoutFromXML(file);

        assertEquals(1, boxes.size());
        CyBoundingBox box = boxes.get("complete");
        assertEquals(1.0, box.getXpos());
        assertEquals(2.0, box.getYpos());
        assertEquals(3.0, box.getHeight());
        assertEquals(4.0, box.getWidth());
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

        Map<String, CyBoundingBox> boxes = XMLInterface.readLayoutFromXML(layout);

        assertTrue(boxes.isEmpty(), "the layout with a DOCTYPE must be rejected: " + boxes.keySet());
    }
}
