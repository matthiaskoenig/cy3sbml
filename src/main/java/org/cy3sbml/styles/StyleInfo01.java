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
public class StyleInfo01 extends StyleInfo {

    public static final String NAME = "cy3sbml";
    public static final String TEMPLATE = "/styles/template_cy3sbml.xml";

    public StyleInfo01(){
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
                VisualPropertyKey.NODE_BORDER_PAINT, SBML.ATTR_COMPARTMENT_CODE, "#000000", m1));

        Map<String, String> m2 = new HashMap<>();
        m2.put(SBML.NODETYPE_SPECIES, "20");
        m2.put(SBML.NODETYPE_QUAL_SPECIES, "20");
        m2.put(SBML.NODETYPE_FBC_GENEPRODUCT, "20");
        mappings.add(new MappingDiscrete(Mapping.DataType.INTEGER,
                VisualPropertyKey.NODE_LABEL_FONT_SIZE, SBML.NODETYPE_ATTR, "16", m2));

        Map<String, String> m3 = new HashMap<>();
        m3.put(SBML.NODETYPE_SPECIES, "#F0F0F0");
        m3.put(SBML.NODETYPE_REACTION, "#999999");
        m3.put(SBML.NODETYPE_COMPARTMENT, "#00CC66");
        m3.put(SBML.NODETYPE_PARAMETER, "#0099FF");
        m3.put(SBML.NODETYPE_LOCAL_PARAMTER, "#0099FF");
        m3.put(SBML.NODETYPE_RULE, "#009933");
        m3.put(SBML.NODETYPE_INITIAL_ASSIGNMENT, "#FF6666");
        m3.put(SBML.NODETYPE_KINETIC_LAW, "#66CCFF");
        m3.put(SBML.NODETYPE_QUAL_SPECIES, "#F0F0F0");
        m3.put(SBML.NODETYPE_QUAL_TRANSITION, "#999999");
        m3.put(SBML.NODETYPE_FBC_GENEPRODUCT, "#FF9966");
        mappings.add(new MappingDiscrete(Mapping.DataType.STRING,
                VisualPropertyKey.NODE_FILL_COLOR, SBML.NODETYPE_ATTR, "#FFFFFF", m3));

        Map<String, String> m4 = new HashMap<>();
        m4.put(SBML.NODETYPE_REACTION, "Liberation Sans Bold,plain,12");
        m4.put(SBML.NODETYPE_COMPARTMENT, "Liberation Sans Bold,plain,12");
        m4.put(SBML.NODETYPE_PARAMETER, "Liberation Sans Bold,plain,12");
        m4.put(SBML.NODETYPE_LOCAL_PARAMTER, "Liberation Sans Bold,plain,12");
        m4.put(SBML.NODETYPE_RULE, "Liberation Sans Bold,plain,12");
        m4.put(SBML.NODETYPE_INITIAL_ASSIGNMENT, "Liberation Sans Bold,plain,12");
        m4.put(SBML.NODETYPE_KINETIC_LAW, "Liberation Sans Bold,plain,12");
        m4.put(SBML.NODETYPE_QUAL_TRANSITION, "Liberation Sans Bold,plain,12");
        mappings.add(new MappingDiscrete(Mapping.DataType.STRING,
                VisualPropertyKey.NODE_LABEL_FONT_FACE, SBML.NODETYPE_ATTR, "Liberation Sans,plain,12", m4));

        Map<String, String> m5 = new HashMap<>();
        m5.put(SBML.NODETYPE_SPECIES, "50.0");
        m5.put(SBML.NODETYPE_REACTION, "15.0");
        m5.put(SBML.NODETYPE_COMPARTMENT, "90.0");
        m5.put(SBML.NODETYPE_PARAMETER, "30.0");
        m5.put(SBML.NODETYPE_LOCAL_PARAMTER, "30.0");
        m5.put(SBML.NODETYPE_RULE, "20.0");
        m5.put(SBML.NODETYPE_KINETIC_LAW, "20.0");
        m5.put(SBML.NODETYPE_QUAL_SPECIES, "50.0");
        m5.put(SBML.NODETYPE_QUAL_TRANSITION, "15.0");
        m5.put(SBML.NODETYPE_FBC_GENEPRODUCT, "50.0");
        mappings.add(new MappingDiscrete(Mapping.DataType.STRING,
                VisualPropertyKey.NODE_SIZE, SBML.NODETYPE_ATTR, "25", m5));

        Map<String, String> m6 = new HashMap<>();
        m6.put(SBML.NODETYPE_REACTION, "N,S,c,0.00,0.00");
        m6.put(SBML.NODETYPE_PARAMETER, "N,S,c,0.00,0.00");
        m6.put(SBML.NODETYPE_LOCAL_PARAMTER, "N,S,c,0.00,0.00");
        m6.put(SBML.NODETYPE_RULE, "N,S,c,0.00,0.00");
        m6.put(SBML.NODETYPE_KINETIC_LAW, "N,S,c,0.00,0.00");
        m6.put(SBML.NODETYPE_QUAL_TRANSITION, "N,S,c,0.00,0.00");
        mappings.add(new MappingDiscrete(Mapping.DataType.STRING,
                VisualPropertyKey.NODE_LABEL_POSITION, SBML.NODETYPE_ATTR, "C,C,c,0.00,0.00", m6));

        Map<String, String> m7 = new HashMap<>();
        m7.put(SBML.NODETYPE_REACTION, "RECTANGLE");
        m7.put(SBML.NODETYPE_COMPARTMENT, "HEXAGON");
        m7.put(SBML.NODETYPE_PARAMETER, "DIAMOND");
        m7.put(SBML.NODETYPE_LOCAL_PARAMTER, "DIAMOND");
        m7.put(SBML.NODETYPE_QUAL_TRANSITION, "RECTANGLE");
        m7.put(SBML.NODETYPE_FBC_GENEPRODUCT, "TRIANGLE");
        mappings.add(new MappingDiscrete(Mapping.DataType.STRING,
                VisualPropertyKey.NODE_SHAPE, SBML.NODETYPE_ATTR, "ELLIPSE", m7));

        // EDGE
        
        Map<String, String> m8 = new HashMap<>();
        m8.put(SBML.INTERACTION_REACTION_PRODUCT, "DELTA");
        m8.put(SBML.INTERACTION_QUAL_TRANSITION_OUTPUT, "DELTA");
        mappings.add(new MappingDiscrete(Mapping.DataType.STRING,
                VisualPropertyKey.EDGE_TARGET_ARROW_SHAPE, SBML.INTERACTION_ATTR, "NONE", m8));

        Map<String, String> m9 = new HashMap<>();
        m9.put(SBML.INTERACTION_REACTION_MODIFIER, "CIRCLE");
        mappings.add(new MappingDiscrete(Mapping.DataType.STRING,
                VisualPropertyKey.EDGE_SOURCE_ARROW_SHAPE, SBML.INTERACTION_ATTR, "NONE", m9));

        Map<String, String> m10 = new HashMap<>();
        m10.put(SBML.INTERACTION_REACTION_PRODUCT, "#000000");
        m10.put(SBML.INTERACTION_REACTION_REACTANT, "#000000");
        m10.put(SBML.INTERACTION_QUAL_TRANSITION_OUTPUT, "#000000");
        m10.put(SBML.INTERACTION_QUAL_TRANSITION_INPUT, "#000000");
        m10.put(SBML.INTERACTION_REACTION_MODIFIER, "#3333FF");
        m10.put(SBML.INTERACTION_FBC_GENEPRODUCT_SPECIES, "#3333FF");
        mappings.add(new MappingDiscrete(Mapping.DataType.STRING,
                VisualPropertyKey.EDGE_STROKE_UNSELECTED_PAINT, SBML.INTERACTION_ATTR, "#CCCCCC", m10));

        return mappings;
    }

}
