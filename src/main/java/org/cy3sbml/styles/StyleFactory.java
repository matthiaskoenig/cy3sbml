package org.cy3sbml.styles;

import org.cy3sbml.util.IOUtil;
import org.cy3sbml.util.XMLUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

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
 *
 * To change the styles change the style information in the StyleInfo classes.
 */
public class StyleFactory {
    private static final Logger logger = LoggerFactory.getLogger(StyleFactory.class);

    /**
     * Creates VisualStyle from StyleInfo
     */
    public static void createStyle(StyleInfo info, File file){

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
                                Element eMap = doc.createElement("discreteMapping");
                                eMap.setAttribute("attributeType", m.getDataType().toString());
                                eMap.setAttribute("attributeName", m.getAttributeName());
                                nvp.appendChild(eMap);

                                // create mapping entries
                                Map<String, String> map = ((MappingDiscrete) m).getMap();
                                for (String attributeValue: map.keySet()){
                                    String value = map.get(attributeValue);
                                    Element eEntry = doc.createElement("discreteMappingEntry");
                                    eEntry.setAttribute("attributeValue", attributeValue);
                                    eEntry.setAttribute("value", value);
                                    nvp.appendChild(eMap);
                                    eMap.appendChild(eEntry);
                                }

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
            System.out.println(file.getAbsolutePath());
            XMLUtil.writeNodeToTidyFile(doc, file);

        }catch (ParserConfigurationException | IOException | SAXException e){
            logger.error("Style could not be created.", e);
            e.printStackTrace();
        }
    }

    ///////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * Create all styles.
     * This creates/updates the styles based on the current settings in SBML.java.
     *
     * For the installation
     *
     */
    public static void main(String[] args){
        String targetDir = "/home/mkoenig/git/cy3sbml/src/main/resources/styles";


        List<StyleInfo> styleInfos = new LinkedList<>();
        styleInfos.add(new StyleInfo_cy3sbml());  // cy3sbml
        styleInfos.add(new StyleInfo_cy3sbmlDark());  // cy3sbml-dark
        for (StyleInfo info: styleInfos){
            File file = new File(targetDir, info.getName() + ".xml");
            StyleFactory.createStyle(info, file);
        }
    }

}
