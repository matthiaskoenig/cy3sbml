package org.cy3sbml.styles;

import org.cy3sbml.SBML;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * Parent class of all StyleInfo.
 */
public class StyleInfo {
    private String name;
    private String template;
    private List<Mapping> mappings;

    /** Constructor. */
    public StyleInfo(String name, String template){
        this.name = name;
        this.template = template;
        this.mappings = new LinkedList<>();
    }

    public String getTemplate() { return template; }

    public List<Mapping> getMappings() { return mappings; }

    public String getName() { return name; }

    public void setMappings(List<Mapping> mappings){
        this.mappings = mappings;
    }

    /**
     * Creates the mappings for all styles.
     * Style specific mappings are defined in the subclasses.
     */
    public List<Mapping> createMappings(){
        List<Mapping> mappings = new LinkedList<>();

        ////////////////////////////////
        // passthroughMapping
        ////////////////////////////////

        mappings.add(new MappingPassthrough(Mapping.DataType.string,
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
        mappings.add(new MappingDiscrete(Mapping.DataType.integer,
                VisualPropertyKey.NODE_BORDER_PAINT, SBML.ATTR_COMPARTMENT_CODE, "#000000", m1));

        Map<String, String> m2 = new HashMap<>();
        m2.put(SBML.NODETYPE_SPECIES, "20");
        m2.put(SBML.NODETYPE_QUAL_SPECIES, "20");
        m2.put(SBML.NODETYPE_FBC_GENEPRODUCT, "20");
        mappings.add(new MappingDiscrete(Mapping.DataType.integer,
                VisualPropertyKey.NODE_LABEL_FONT_SIZE, SBML.NODETYPE_ATTR, "16", m2));

        Map<String, String> m4 = new HashMap<>();
        m4.put(SBML.NODETYPE_REACTION, "Liberation Sans Bold,plain,12");
        m4.put(SBML.NODETYPE_COMPARTMENT, "Liberation Sans Bold,plain,12");
        m4.put(SBML.NODETYPE_PARAMETER, "Liberation Sans Bold,plain,12");
        m4.put(SBML.NODETYPE_LOCAL_PARAMETER, "Liberation Sans Bold,plain,12");
        m4.put(SBML.NODETYPE_ALGEBRAIC_RULE, "Liberation Sans Bold,plain,12");
        m4.put(SBML.NODETYPE_ASSIGNMENT_RULE, "Liberation Sans Bold,plain,12");
        m4.put(SBML.NODETYPE_RATE_RULE, "Liberation Sans Bold,plain,12");
        m4.put(SBML.NODETYPE_INITIAL_ASSIGNMENT, "Liberation Sans Bold,plain,12");
        m4.put(SBML.NODETYPE_KINETIC_LAW, "Liberation Sans Bold,plain,12");
        m4.put(SBML.NODETYPE_QUAL_TRANSITION, "Liberation Sans Bold,plain,12");
        mappings.add(new MappingDiscrete(Mapping.DataType.string,
                VisualPropertyKey.NODE_LABEL_FONT_FACE, SBML.NODETYPE_ATTR, "Liberation Sans,plain,12", m4));

        Map<String, String> m5 = new HashMap<>();
        m5.put(SBML.NODETYPE_SPECIES, "40.0");
        m5.put(SBML.NODETYPE_REACTION, "15.0");
        m5.put(SBML.NODETYPE_COMPARTMENT, "90.0");
        m5.put(SBML.NODETYPE_PARAMETER, "30.0");
        m5.put(SBML.NODETYPE_LOCAL_PARAMETER, "30.0");
        m5.put(SBML.NODETYPE_ALGEBRAIC_RULE, "20.0");
        m5.put(SBML.NODETYPE_ASSIGNMENT_RULE, "20.0");
        m5.put(SBML.NODETYPE_RATE_RULE, "20.0");
        m5.put(SBML.NODETYPE_KINETIC_LAW, "20.0");
        m5.put(SBML.NODETYPE_QUAL_SPECIES, "40.0");
        m5.put(SBML.NODETYPE_QUAL_TRANSITION, "15.0");
        m5.put(SBML.NODETYPE_FBC_GENEPRODUCT, "40.0");
        m5.put(SBML.NODETYPE_UNIT_DEFINITION, "30.0");
        m5.put(SBML.NODETYPE_UNIT, "20.0");
        m5.put(SBML.NODETYPE_EVENT, "40.0");
        m5.put(SBML.NODETYPE_EVENT_ASSIGNMENT, "20.0");
        mappings.add(new MappingDiscrete(Mapping.DataType.string,
                VisualPropertyKey.NODE_SIZE, SBML.NODETYPE_ATTR, "25", m5));

        Map<String, String> m6 = new HashMap<>();
        m6.put(SBML.NODETYPE_REACTION, "N,S,c,0.00,0.00");
        m6.put(SBML.NODETYPE_PARAMETER, "N,S,c,0.00,0.00");
        m6.put(SBML.NODETYPE_LOCAL_PARAMETER, "N,S,c,0.00,0.00");
        m6.put(SBML.NODETYPE_ALGEBRAIC_RULE, "N,S,c,0.00,0.00");
        m6.put(SBML.NODETYPE_ASSIGNMENT_RULE, "N,S,c,0.00,0.00");
        m6.put(SBML.NODETYPE_RATE_RULE, "N,S,c,0.00,0.00");
        m6.put(SBML.NODETYPE_KINETIC_LAW, "N,S,c,0.00,0.00");
        m6.put(SBML.NODETYPE_QUAL_TRANSITION, "N,S,c,0.00,0.00");
        mappings.add(new MappingDiscrete(Mapping.DataType.string,
                VisualPropertyKey.NODE_LABEL_POSITION, SBML.NODETYPE_ATTR, "C,C,c,0.00,0.00", m6));

        Map<String, String> m7 = new HashMap<>();
        m7.put(SBML.NODETYPE_REACTION, "RECTANGLE");
        m7.put(SBML.NODETYPE_COMPARTMENT, "HEXAGON");
        m7.put(SBML.NODETYPE_PARAMETER, "DIAMOND");
        m7.put(SBML.NODETYPE_LOCAL_PARAMETER, "DIAMOND");
        m7.put(SBML.NODETYPE_QUAL_TRANSITION, "RECTANGLE");
        m7.put(SBML.NODETYPE_FBC_GENEPRODUCT, "TRIANGLE");
        m7.put(SBML.NODETYPE_UNIT_DEFINITION, "PARALLELOGRAM");
        m7.put(SBML.NODETYPE_UNIT, "PARALLELOGRAM");
        m7.put(SBML.NODETYPE_EVENT, "HEXAGON");
        m7.put(SBML.NODETYPE_EVENT_ASSIGNMENT, "HEXAGON");

        mappings.add(new MappingDiscrete(Mapping.DataType.string,
                VisualPropertyKey.NODE_SHAPE, SBML.NODETYPE_ATTR, "ELLIPSE", m7));

        // EDGE

        Map<String, String> m8 = new HashMap<>();
        m8.put(SBML.INTERACTION_REACTION_PRODUCT, "DELTA");
        m8.put(SBML.INTERACTION_QUAL_TRANSITION_OUTPUT, "DELTA");
        mappings.add(new MappingDiscrete(Mapping.DataType.string,
                VisualPropertyKey.EDGE_TARGET_ARROW_SHAPE, SBML.INTERACTION_ATTR, "NONE", m8));

        Map<String, String> m9 = new HashMap<>();
        m9.put(SBML.INTERACTION_REACTION_MODIFIER, "CIRCLE");
        m9.put(SBML.INTERACTION_REACTION_ACTIVATOR, "DIAMOND");
        m9.put(SBML.INTERACTION_REACTION_INHIBITOR, "T");
        mappings.add(new MappingDiscrete(Mapping.DataType.string,
                VisualPropertyKey.EDGE_SOURCE_ARROW_SHAPE, SBML.INTERACTION_ATTR_EXTENDED, "NONE", m9));

        Map<String, String> m11 = new HashMap<>();
        m11.put(SBML.INTERACTION_REACTION_MODIFIER, "LONG_DASH");
        m11.put(SBML.INTERACTION_UNIT_UNITDEFINITION, "DOT");
        m11.put(SBML.INTERACTION_SBASE_UNITDEFINITION, "DOT");
        m11.put(SBML.INTERACTION_VARIABLE_EVENT_ASSIGNMENT, "DOT");
        m11.put(SBML.INTERACTION_REFERENCE_EVENT_ASSIGNMENT, "DOT");
        mappings.add(new MappingDiscrete(Mapping.DataType.string,
                VisualPropertyKey.EDGE_LINE_TYPE, SBML.INTERACTION_ATTR, "SOLID", m11));

        return mappings;
    }

}
