package org.cy3sbml.layout;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.NodeList;
import org.w3c.dom.Node;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import java.util.Collection;
import java.util.HashMap;
import java.io.File;

public class XMLInterface {
	public static String LAYOUT = "layout";
	public static String BOX_LIST = "listOfBoundingBoxes";
	
	public static String BOX = "boundingBox";
	public static String BOX_ID = "id";
	public static String BOX_X = "xpos";
	public static String BOX_Y = "ypos";
	public static String BOX_HEIGHT = "height";
	public static String BOX_WIDTH = "width";
	
	// XML EXPORT //
	
	public static void writeXMLFileForLayout(String filename, Collection<CyBoundingBox> boxes){
		File xmlFile = new File(filename);
		writeXMLFileForLayout(xmlFile, boxes);
	}
	
	public static void writeXMLFileForLayout(File xmlFile, Collection<CyBoundingBox> boxes){
		Document doc = createXMLDocumentFromLayout(boxes);
		writeXMLDocumentToFile(doc, xmlFile);
	}
	
	private static Document createXMLDocumentFromLayout(Collection<CyBoundingBox> boxes){
		Document doc = null;
		try {
			DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder docBuilder;
			docBuilder = docFactory.newDocumentBuilder();
			doc = docBuilder.newDocument();
			Element rootElement = doc.createElement(LAYOUT);
			doc.appendChild(rootElement);
			
			Element boxListNode = doc.createElement(BOX_LIST);
			rootElement.appendChild(boxListNode);
			
			for (CyBoundingBox box: boxes){
				addDomForBoundingBox(doc, boxListNode, box);
			}
		} catch (ParserConfigurationException e) {
			doc = null;
			e.printStackTrace();
		}
		return doc;
	}
	
	private static void addDomForBoundingBox(Document doc, Element boxListElement, CyBoundingBox box){
		Element boxNode = doc.createElement(BOX);
		boxListElement.appendChild(boxNode);
		boxNode.setAttribute(BOX_ID, box.getNodeId());
		boxNode.setAttribute(BOX_X, Double.toString(box.getXpos()));
		boxNode.setAttribute(BOX_Y, Double.toString(box.getYpos()));
		boxNode.setAttribute(BOX_HEIGHT, Double.toString(box.getHeight()));
		boxNode.setAttribute(BOX_WIDTH, Double.toString(box.getWidth()));
	}
	
	private static void writeXMLDocumentToFile(Document doc, File xmlFile){
		try {
			TransformerFactory transformerFactory = TransformerFactory.newInstance();
			Transformer transformer;

			transformer = transformerFactory.newTransformer();
			DOMSource source = new DOMSource(doc);
			StreamResult result = new StreamResult(xmlFile);
			transformer.transform(source, result);
		} catch (TransformerException e) {
			e.printStackTrace();
		}
	}
	
	// XML IMPORT //

	public static HashMap<String, CyBoundingBox> readLayoutFromXML(String filename){
		File xmlFile = new File(filename);
		return readLayoutFromXML(xmlFile);
	}
	
	public static HashMap<String, CyBoundingBox> readLayoutFromXML(File xmlFile){
		HashMap<String, CyBoundingBox> boxes = new HashMap<String, CyBoundingBox>();
		
		try {
			DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
			Document doc = dBuilder.parse(xmlFile);
			doc.getDocumentElement().normalize();

			NodeList boxList = doc.getElementsByTagName(BOX);
			for (int k=0; k< boxList.getLength(); ++k){
				Node boxNode = boxList.item(k);
				CyBoundingBox box = readBoundingBoxFromNode(boxNode);
				boxes.put(box.getNodeId(), box); 
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return boxes;
	}
	
	private static CyBoundingBox readBoundingBoxFromNode(Node boxNode){
		NamedNodeMap map = boxNode.getAttributes();
		String nodeId = map.getNamedItem(BOX_ID).getTextContent(); 
		String xpos = map.getNamedItem(BOX_X).getTextContent();
		String ypos = map.getNamedItem(BOX_Y).getTextContent();
		String height = map.getNamedItem(BOX_HEIGHT).getTextContent();
		String width = map.getNamedItem(BOX_WIDTH).getTextContent();
		
		CyBoundingBox box = new CyBoundingBox(nodeId,
				Double.parseDouble(xpos),
				Double.parseDouble(ypos),
				Double.parseDouble(height),
				Double.parseDouble(width));
		return box;
	}
}