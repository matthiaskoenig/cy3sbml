package org.cy3sbml.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.xml.parsers.DocumentBuilderFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

class XMLUtilTest {

    @TempDir
    Path tempDir;

    @Test
    void readXMLStringParsesValidXml() {
        Document doc = XMLUtil.readXMLString("<root><child>text</child></root>");
        assertEquals("root", doc.getDocumentElement().getTagName());
    }

    @Test
    void readXMLStringReturnsNullForMalformedXml() {
        assertNull(XMLUtil.readXMLString("<root><child>"));
    }

    @Test
    void writeNodeToTidyStringProducesIndentedXml() {
        Document doc = XMLUtil.readXMLString("<root><child>text</child></root>");
        String tidy = XMLUtil.writeNodeToTidyString(doc);
        assertTrue(tidy.contains("<child>text</child>"));
    }

    @Test
    void writeNodeToTidyFileWritesReadableXml() throws Exception {
        Document doc = XMLUtil.readXMLString("<root><child>text</child></root>");
        File file = tempDir.resolve("out.xml").toFile();

        XMLUtil.writeNodeToTidyFile(doc, file);

        String content = Files.readString(file.toPath(), StandardCharsets.UTF_8);
        assertTrue(content.contains("<child>text</child>"));
    }

    @Test
    void xml2HtmlEscapesTagsAndKeepsLineBreaks() {
        String html = XMLUtil.xml2Html("<a>x</a>");
        assertTrue(html.contains("&lt;a&gt;"));
        assertTrue(html.contains("<br />"));
    }

    @Test
    void xml2xmlReturnsTidiedXmlString() {
        String tidy = XMLUtil.xml2xml("<root><child>text</child></root>");
        assertTrue(tidy.contains("<child>text</child>"));
    }

    @Test
    void cleanEmptyTextNodesRemovesWhitespaceOnlyTextBetweenElements() throws Exception {
        Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
        Element root = doc.createElement("root");
        doc.appendChild(root);
        root.appendChild(doc.createTextNode("   \n  "));
        Element child = doc.createElement("child");
        root.appendChild(child);
        root.appendChild(doc.createTextNode("   "));

        XMLUtil.cleanEmptyTextNodes(root);

        int textNodeCount = 0;
        Node n = root.getFirstChild();
        while (n != null) {
            if (n.getNodeType() == Node.TEXT_NODE) {
                textNodeCount++;
            }
            n = n.getNextSibling();
        }
        assertEquals(0, textNodeCount);
    }
}
