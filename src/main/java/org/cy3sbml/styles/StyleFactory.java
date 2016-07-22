package org.cy3sbml.styles;

import org.cy3sbml.util.IOUtil;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedList;
import java.util.List;

/**
 * Factory for creating visual styles depending on the current
 * SBML attributes and values.
 * This allows simple update of the styles with changed attributes.
 *
 * A template engine is used to fill in the styles with the given
 * information.
 *
 * Template only defines the default values.
 * The additional mappings are added.
 */
public class StyleFactory {

    /**
     * Creates VisualStyle from StyleInfo
     */
    public static void createStyle(StyleInfo info, String targetDir){

        // read template
        String template = info.getTemplate();
        String name = info.getName();
        System.out.println(String.format("Create style: <%s> with template <%s>",
                name, template));

        InputStream xmlStream = IOUtil.readResource(info.getTemplate());
        try {
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.parse(xmlStream);

            // modify template with information
            // - set name
            NodeList nList = doc.getElementsByTagName("visualStyle");
            Node n = nList.item(0);
            if (n.getNodeType() == Node.ELEMENT_NODE){
                Element e = (Element) n;
                e.setAttribute("name", name);
            }

            // - add mappings
            for (Mapping m: info.getMappings()){
                // find the correct visualProperty for the mapping
                NodeList vpList = doc.getElementsByTagName("visualProperty");
                for (int k=0; k<vpList.getLength(); k++){
                    Node nvp = vpList.item(k);
                    if (nvp.getNodeType() == Node.ELEMENT_NODE){
                        Element evp = (Element) nvp;
                        String vpName = evp.getAttribute("name");
                        // found correct property
                        String propertyName = m.getVisualProperty().toString();

                        if (vpName.equals(m.getVisualProperty().toString())){
                            // set default
                            evp.setAttribute("default", m.getDefaultValue());

                            // set mapping
                            if (m.getMappingType() == Mapping.MappingType.PASSTHROUGH){
                                System.out.println(Mapping.MappingType.PASSTHROUGH);
                                // create mapping node
                                Element eMap = doc.createElement("passthroughMapping");
                                eMap.setAttribute("attributeType", m.getDataType().toString());
                                eMap.setAttribute("attributeName", m.getAttributeName());
                                nvp.appendChild(eMap);

                            }else if (m.getMappingType() == Mapping.MappingType.DISCRETE){
                                System.out.println(Mapping.MappingType.DISCRETE);
                                // create mapping node
                            }else if (m.getMappingType() == Mapping.MappingType.CONTINOUS){
                                // TODO: implement
                                System.out.println("Continous mapping not supported.");
                            }

                            System.out.println("visualProperty set: " + vpName);
                        } else {
                            continue;
                        }
                    }
                }


            }

            // save the template
            File file = new File(targetDir, name + ".xml");
            System.out.println(file.getAbsolutePath());

            Transformer transformer = TransformerFactory.newInstance().newTransformer();
            Result output = new StreamResult(file);
            Source input = new DOMSource(doc);
            transformer.transform(input, output);

        }catch (ParserConfigurationException | IOException | SAXException | TransformerException e){
            e.printStackTrace();
        }



    }

    /**
     * Create all styles.
     * This creates/updates the styles based on the current settings in SBML.java.
     */
    public static void main(String[] args){
        String targetDir = "/home/mkoenig/git/cy3sbml/src/main/resources/styles";

        List<StyleInfo> styleInfos = new LinkedList<>();
        styleInfos.add(new StyleInfo01());  // cy3sbml
        for (StyleInfo info: styleInfos){
            StyleFactory.createStyle(info, targetDir);
        }

    }


}