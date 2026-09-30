package org.cy3sbml.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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

    /**
     * XXE: an external general entity pointing to a local file must not pull the file's
     * content into the parsed document. The document type declaration is rejected, so
     * parsing fails and nothing is read.
     */
    @Test
    void readXMLStringDoesNotResolveExternalEntities() throws Exception {
        Path secret = tempDir.resolve("secret.txt");
        Files.writeString(secret, "TOP-SECRET", StandardCharsets.UTF_8);
        String xml = "<?xml version=\"1.0\"?>\n"
                + "<!DOCTYPE root [<!ENTITY xxe SYSTEM \"" + secret.toUri() + "\">]>\n"
                + "<root>&xxe;</root>";

        assertNull(XMLUtil.readXMLString(xml));
        String html = XMLUtil.xml2Html(xml);
        assertFalse(html.contains("TOP-SECRET"), html);
    }

    /** XXE via an external parameter entity / external DTD must not be loaded either. */
    @Test
    void readXMLStringDoesNotLoadExternalDtds() throws Exception {
        Path dtd = tempDir.resolve("evil.dtd");
        Path marker = tempDir.resolve("secret.txt");
        Files.writeString(marker, "TOP-SECRET", StandardCharsets.UTF_8);
        Files.writeString(dtd, "<!ENTITY xxe SYSTEM \"" + marker.toUri() + "\">", StandardCharsets.UTF_8);
        String xml = "<?xml version=\"1.0\"?>\n"
                + "<!DOCTYPE root [<!ENTITY % ext SYSTEM \"" + dtd.toUri() + "\"> %ext;]>\n"
                + "<root>&xxe;</root>";

        assertNull(XMLUtil.readXMLString(xml));
    }

    @Test
    void documentBuilderFactoryIsHardened() throws Exception {
        DocumentBuilderFactory factory = XMLUtil.documentBuilderFactory();
        assertTrue(factory.getFeature("http://apache.org/xml/features/disallow-doctype-decl"));
        assertFalse(factory.getFeature("http://xml.org/sax/features/external-general-entities"));
        assertFalse(factory.getFeature("http://xml.org/sax/features/external-parameter-entities"));
        assertFalse(factory.getFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd"));
        assertTrue(factory.getFeature(javax.xml.XMLConstants.FEATURE_SECURE_PROCESSING));
        assertFalse(factory.isXIncludeAware());
        assertFalse(factory.isExpandEntityReferences());
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
