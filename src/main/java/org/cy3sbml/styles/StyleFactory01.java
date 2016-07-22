package org.cy3sbml.styles;

import org.cy3sbml.SBML;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;


/**
 * Definition of mappings for style.
 *
 * Uses a template file and than sets the specific mappings in the file.
 *
 * TODO: use colorbrewer for colors
 */
public class StyleFactory01 extends StyleFactory{

    public static final String NAME = "cy3sbml";
    public static final String TEMPLATE = "template_01.xml";

    public StyleFactory01(){
        super(NAME, TEMPLATE);
        setMappings(createMappings());
    }

    /** Create the mappings for the given style. */
    @Override
    public List<Mapping> createMappings(){
        List<Mapping> mappings = new LinkedList<>();

        ////////////////////////////////
        // passthroughMapping
        ////////////////////////////////

        mappings.add(new MappingPassthrough(Mapping.DataType.INTEGER,
                VisualPropertyKey.NODE_LABEL, SBML.LABEL, ""));

        ////////////////////////////////
        // discreteMapping
        ////////////////////////////////

        Map<String, String> m1 = new HashMap<>();
        m1.put("1", "#3333FF");
        m1.put("2", "#FF3333");
        m1.put("3", "#336600");
        m1.put("4", "#CC6600");
        m1.put("5", "#CCCC00");
        m1.put("6", "#66CCFF");
        m1.put("7", "#990099");
        m1.put("8", "#F0F0F0");
        mappings.add(new MappingDiscrete(Mapping.DataType.INTEGER,
                VisualPropertyKey.NODE_FILL_COLOR, SBML.ATTR_COMPARTMENT_CODE, "#000000", m1));

        Map<String, String> m2 = new HashMap<>();
        m2.put(SBML.NODETYPE_SPECIES, "20");
        m2.put(SBML.NODETYPE_QUAL_SPECIES, "20");
        m2.put(SBML.NODETYPE_FBC_GENEPRODUCT, "20");
        mappings.add(new MappingDiscrete(Mapping.DataType.INTEGER,
                VisualPropertyKey.NODE_LABEL_FONT_SIZE, SBML.NODETYPE_ATTR, "16", m2));

        return mappings;
    }

}
