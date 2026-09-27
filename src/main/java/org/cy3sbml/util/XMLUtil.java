package org.cy3sbml.util;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.apache.commons.text.StringEscapeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.DefaultHandler;

public class XMLUtil {
    private static final Logger logger = LoggerFactory.getLogger(XMLUtil.class);
    public static final Integer INDENT_AMOUNT = 4;

    public static final String XML_INDENT = new String(new char[INDENT_AMOUNT]).replace("\0", " ");
    public static final String HTML_INDENT = new String(new char[INDENT_AMOUNT]).replace("\0", "&nbsp;");

    /**
     * Convert XML String to html string.
     */
    public static String xml2Html(String xml) {
        Document doc = XMLUtil.readXMLString(xml);
        if (doc != null) {
            String xmlTidy = XMLUtil.writeNodeToTidyString(doc);
            if (xmlTidy != null) {
                xml = xmlTidy;
            }
        }
        // escape the rest, i.e. things like < and >
        String html = StringEscapeUtils.escapeHtml4(xml);

        // keep formating in html
        // Not working due to escaping of the respective tags
        html = html.replaceAll("\n", "<br />").replaceAll(XML_INDENT, HTML_INDENT);

        return html;
    }

    /**
     * Create tidy xml string from xml string.
     */
    public static String xml2xml(String xml) {
        String html = null;
        Document doc = XMLUtil.readXMLString(xml);
        if (doc != null) {
            html = XMLUtil.writeNodeToTidyString(doc);
        }
        return html;
    }

    /**
     * Creates a {@link DocumentBuilderFactory} hardened against XML external entity (XXE)
     * attacks. The XML parsed by cy3sbml (SBML notes and annotations, layout files, the
     * bundled style templates) never needs a document type declaration, so any DOCTYPE is
     * rejected outright; this also rules out entity expansion attacks. External general
     * and parameter entities, external DTD loading, XInclude and entity reference
     * expansion are disabled as well, as defense in depth, and secure processing is on.
     */
    public static DocumentBuilderFactory documentBuilderFactory() throws ParserConfigurationException {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
        factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
        factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
        factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
        factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
        factory.setXIncludeAware(false);
        factory.setExpandEntityReferences(false);
        return factory;
    }

    /**
     * Creates a {@link Transformer} (identity transform) that never fetches external DTDs
     * or stylesheets.
     */
    public static Transformer transformer() throws TransformerConfigurationException {
        TransformerFactory factory = TransformerFactory.newInstance();
        factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
        factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_STYLESHEET, "");
        return factory.newTransformer();
    }

    /**
     * Creates a document builder from the hardened {@link #documentBuilderFactory()} that
     * reports parse errors only through its exceptions. The default error handler also
     * prints them to stderr.
     */
    public static DocumentBuilder documentBuilder() throws ParserConfigurationException {
        DocumentBuilder builder = documentBuilderFactory().newDocumentBuilder();
        builder.setErrorHandler(new DefaultHandler() {
            @Override
            public void fatalError(SAXParseException e) throws SAXException {
                throw e;
            }
        });
        return builder;
    }

    /**
     * Read XML Document from String.
     */
    public static Document readXMLString(String xml) {
        InputStream xmlStream = IOUtil.string2InputStream(xml);
        Document doc = null;
        try {
            doc = documentBuilder().parse(xmlStream);
        } catch (SAXException | ParserConfigurationException | IOException e) {
            // invalid XML in a model, e.g. in the notes, is a data problem, not a bug
            logger.warn("Reading xml string failed: {}", e.getMessage());
        }
        return doc;
    }

    /**
     * Write XML Document to file.
     */
    public static void writeNodeToTidyFile(Node node, File file) {
        XMLUtil.cleanEmptyTextNodes(node);
        Transformer transformer;
        try {
            transformer = XMLUtil.transformer();
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", INDENT_AMOUNT.toString());
            Result output = new StreamResult(file);
            Source input = new DOMSource(node);
            transformer.transform(input, output);
        } catch (TransformerException e) {
            logger.error("Writing node failed.", e);
        }
    }

    /**
     * Write XML Document to string.
     * See: http://stackoverflow.com/questions/5456680/xml-document-to-string
     */
    public static String writeNodeToTidyString(Node node) {
        XMLUtil.cleanEmptyTextNodes(node);

        String output = null;
        try {
            Transformer transformer = XMLUtil.transformer();
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", INDENT_AMOUNT.toString());
            transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
            StringWriter writer = new StringWriter();
            transformer.transform(new DOMSource(node), new StreamResult(writer));
            output = writer.getBuffer().toString();

        } catch (TransformerException e) {
            logger.error("Writing node failed.", e);
        }
        return output;
    }

    /**
     * Removes text nodes that only contains whitespace. The conditions for
     * removing text nodes, besides only containing whitespace, are: If the
     * parent node has at least one child of any of the following types, all
     * whitespace-only text-node children will be removed: - ELEMENT child -
     * CDATA child - COMMENT child
     * <p>
     * The purpose of this is to make the format() method (that use a
     * Transformer for formatting) more consistent regarding indenting and line
     * breaks.
     */
    public static void cleanEmptyTextNodes(Node parentNode) {
        boolean removeEmptyTextNodes = false;
        Node childNode = parentNode.getFirstChild();
        while (childNode != null) {
            removeEmptyTextNodes |= checkNodeTypes(childNode);
            childNode = childNode.getNextSibling();
        }

        if (removeEmptyTextNodes) {
            removeEmptyTextNodes(parentNode);
        }
    }

    private static void removeEmptyTextNodes(Node parentNode) {
        Node childNode = parentNode.getFirstChild();
        while (childNode != null) {
            // grab the "nextSibling" before the child node is removed
            Node nextChild = childNode.getNextSibling();

            short nodeType = childNode.getNodeType();
            if (nodeType == Node.TEXT_NODE) {
                boolean containsOnlyWhitespace = childNode.getNodeValue().trim().isEmpty();
                if (containsOnlyWhitespace) {
                    parentNode.removeChild(childNode);
                }
            }
            childNode = nextChild;
        }
    }

    private static boolean checkNodeTypes(Node childNode) {
        short nodeType = childNode.getNodeType();

        if (nodeType == Node.ELEMENT_NODE) {
            cleanEmptyTextNodes(childNode); // recurse into subtree
        }

        if (nodeType == Node.ELEMENT_NODE || nodeType == Node.CDATA_SECTION_NODE || nodeType == Node.COMMENT_NODE) {
            return true;
        } else {
            return false;
        }
    }
}
