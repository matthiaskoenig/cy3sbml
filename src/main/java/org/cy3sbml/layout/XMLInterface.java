package org.cy3sbml.layout;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.cy3sbml.util.XMLUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class XMLInterface {
    private static final Logger logger = LoggerFactory.getLogger(XMLInterface.class);

    public static final String LAYOUT = "layout";
    public static final String BOX_LIST = "listOfBoundingBoxes";

    public static final String BOX = "boundingBox";
    public static final String BOX_CYID = "cyId";
    public static final String BOX_ID = "id";
    public static final String BOX_X = "xpos";
    public static final String BOX_Y = "ypos";
    public static final String BOX_HEIGHT = "height";
    public static final String BOX_WIDTH = "width";

    // XML EXPORT //

    public static void writeXMLFileForLayout(File xmlFile, Collection<CyBoundingBox> boxes) {
        Document doc = createXMLDocumentFromLayout(boxes);
        writeXMLDocumentToFile(doc, xmlFile);
    }

    private static Document createXMLDocumentFromLayout(Collection<CyBoundingBox> boxes) {
        Document doc = null;
        try {
            doc = XMLUtil.documentBuilder().newDocument();
            Element rootElement = doc.createElement(LAYOUT);
            doc.appendChild(rootElement);

            Element boxListNode = doc.createElement(BOX_LIST);
            rootElement.appendChild(boxListNode);

            for (CyBoundingBox box : boxes) {
                addDomForBoundingBox(doc, boxListNode, box);
            }
        } catch (ParserConfigurationException e) {
            doc = null;
            logger.error("Problems with xml parsing.", e);
        }
        return doc;
    }

    private static void addDomForBoundingBox(Document doc, Element boxListElement, CyBoundingBox box) {
        Element boxNode = doc.createElement(BOX);
        boxListElement.appendChild(boxNode);
        if (box.cyId() != null) {
            boxNode.setAttribute(BOX_CYID, box.cyId());
        }
        if (box.sbmlId() != null) {
            boxNode.setAttribute(BOX_ID, box.sbmlId());
        }
        boxNode.setAttribute(BOX_X, Double.toString(box.x()));
        boxNode.setAttribute(BOX_Y, Double.toString(box.y()));
        boxNode.setAttribute(BOX_HEIGHT, Double.toString(box.height()));
        boxNode.setAttribute(BOX_WIDTH, Double.toString(box.width()));
    }

    private static void writeXMLDocumentToFile(Document doc, File xmlFile) {
        try {
            Transformer transformer = XMLUtil.transformer();
            DOMSource source = new DOMSource(doc);
            StreamResult result = new StreamResult(xmlFile);
            transformer.transform(source, result);
        } catch (TransformerException e) {
            logger.error("Problems writing layout", e);
        }
    }

    // XML IMPORT //

    /**
     * Reads the bounding boxes of the layout file, or returns an empty list if the file cannot
     * be read.
     */
    public static List<CyBoundingBox> readLayoutFromXML(File xmlFile) {
        List<CyBoundingBox> boxes = new ArrayList<>();

        try {
            Document doc = XMLUtil.documentBuilder().parse(xmlFile);
            doc.getDocumentElement().normalize();

            NodeList boxList = doc.getElementsByTagName(BOX);
            for (int k = 0; k < boxList.getLength(); ++k) {
                Node boxNode = boxList.item(k);
                CyBoundingBox box = readBoundingBoxFromNode(boxNode);
                if (box != null) {
                    boxes.add(box);
                }
            }
        } catch (ParserConfigurationException | SAXException | IOException e) {
            // an unreadable layout file is a data problem, not a bug
            logger.warn("Could not read layout {}: {}", xmlFile, e.getMessage());
        }
        return boxes;
    }

    /**
     * Reads the bounding box of the given node, or returns null if an attribute is missing
     * or not a number.
     */
    private static CyBoundingBox readBoundingBoxFromNode(Node boxNode) {
        NamedNodeMap map = boxNode.getAttributes();
        String cyId = attribute(map, BOX_CYID);
        String sbmlId = attribute(map, BOX_ID);
        String nodeId = cyId != null ? cyId : sbmlId;
        String xpos = attribute(map, BOX_X);
        String ypos = attribute(map, BOX_Y);
        String height = attribute(map, BOX_HEIGHT);
        String width = attribute(map, BOX_WIDTH);
        if (nodeId == null || xpos == null || ypos == null || height == null || width == null) {
            logger.warn("Bounding box with missing attributes skipped: id={}", nodeId);
            return null;
        }
        try {
            return new CyBoundingBox(
                    cyId,
                    sbmlId,
                    Double.parseDouble(xpos),
                    Double.parseDouble(ypos),
                    Double.parseDouble(height),
                    Double.parseDouble(width));
        } catch (NumberFormatException e) {
            logger.warn("Bounding box with invalid number skipped: id={}: {}", nodeId, e.getMessage());
            return null;
        }
    }

    private static String attribute(NamedNodeMap map, String name) {
        Node item = map.getNamedItem(name);
        return item == null ? null : item.getTextContent();
    }
}
