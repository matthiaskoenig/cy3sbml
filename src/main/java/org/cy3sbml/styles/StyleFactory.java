package org.cy3sbml.styles;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.TreeMap;
import javax.xml.parsers.ParserConfigurationException;
import org.cy3sbml.util.IOUtil;
import org.cy3sbml.util.XMLUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * Generates the style files of cy3sbml from a template, which defines the default values,
 * and the mappings of a {@link StyleInfo}.
 * <p>
 * The bundled styles {@code resources/styles/cy3sbml*.xml} are generated with
 * {@link #createStyle}; to change a style, change its {@code StyleInfo} or template and
 * regenerate the file ({@code StyleFactoryTest} checks that they are consistent).
 */
public class StyleFactory {
    private static final Logger logger = LoggerFactory.getLogger(StyleFactory.class);

    /**
     * Writes the style file of the style information: its template with the name and the
     * mappings of the style.
     *
     * @param info the style information
     * @param file the style file to write
     */
    public static void createStyle(StyleInfo info, File file) {
        String name = info.getName();
        try (InputStream xmlStream = IOUtil.readResource(info.getTemplate())) {
            Document doc = XMLUtil.documentBuilder().parse(xmlStream);

            // modify template with information
            // - set name
            NodeList nList = doc.getElementsByTagName("visualStyle");
            Node n = nList.item(0);
            if (n.getNodeType() == Node.ELEMENT_NODE) {
                Element e = (Element) n;
                e.setAttribute("name", name);
            }

            // - add mappings
            for (Mapping m : info.getMappings()) {
                // find the correct visualProperty for the mapping
                NodeList vpList = doc.getElementsByTagName("visualProperty");
                for (int k = 0; k < vpList.getLength(); k++) {
                    Node nvp = vpList.item(k);
                    if (nvp.getNodeType() == Node.ELEMENT_NODE) {
                        Element evp = (Element) nvp;
                        String vpName = evp.getAttribute("name");
                        // found correct property

                        if (vpName.equals(m.getVisualProperty().toString())) {
                            // set default
                            evp.setAttribute("default", m.getDefaultValue());

                            // set mapping
                            if (m.getMappingType() == Mapping.MappingType.PASSTHROUGH) {

                                // create mapping node
                                Element eMap = doc.createElement("passthroughMapping");
                                eMap.setAttribute(
                                        "attributeType", m.getDataType().attributeType());
                                eMap.setAttribute("attributeName", m.getAttributeName());
                                nvp.appendChild(eMap);

                            } else if (m.getMappingType() == Mapping.MappingType.DISCRETE) {

                                // create mapping node
                                Element eMap = doc.createElement("discreteMapping");
                                eMap.setAttribute(
                                        "attributeType", m.getDataType().attributeType());
                                eMap.setAttribute("attributeName", m.getAttributeName());
                                nvp.appendChild(eMap);

                                // create mapping entries, sorted for a stable file
                                Map<String, String> map = new TreeMap<>(((MappingDiscrete) m).getMap());
                                for (String attributeValue : map.keySet()) {
                                    String value = map.get(attributeValue);
                                    Element eEntry = doc.createElement("discreteMappingEntry");
                                    eEntry.setAttribute("attributeValue", attributeValue);
                                    eEntry.setAttribute("value", value);
                                    eMap.appendChild(eEntry);
                                }

                            } else if (m.getMappingType() == Mapping.MappingType.CONTINOUS) {
                                logger.warn("Continuous mapping not supported: {}", m);
                            }
                        }
                    }
                }
            }

            // save the template
            logger.info("Write style: {}", file.getAbsolutePath());
            XMLUtil.writeNodeToTidyFile(doc, file);

        } catch (ParserConfigurationException | IOException | SAXException e) {
            logger.error("Style could not be created.", e);
        }
    }
}
