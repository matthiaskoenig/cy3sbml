package org.cy3sbml.styles;

import org.cy3sbml.util.IOUtil;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
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
    public static void createStyle(StyleInfo info){

        // read template
        String template = info.getTemplate();
        String name = info.getName();
        System.out.println(String.format("Create style: <{}> with template <{}>",
                name, template));

        InputStream xmlStream = IOUtil.readResource(info.getTemplate());
        try {
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.parse(xmlStream);

            // modify the template with information
            // - set name
            // - add mappings


            for (Mapping m: info.getMappings()){
                System.out.println(m);
            }

            // save the template





        }catch (ParserConfigurationException | IOException | SAXException e){
            e.printStackTrace();
        }



    }

    /**
     * Create all styles.
     * This creates/updates the styles based on the current settings in SBML.java.
     */
    public static void main(String[] args){
        List<StyleInfo> styleInfos = new LinkedList<>();
        styleInfos.add(new StyleInfo01());  // cy3sbml
        for (StyleInfo info: styleInfos){
            StyleFactory.createStyle(info);
        }

    }


}
