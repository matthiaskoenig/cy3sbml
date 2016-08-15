package org.cy3sbml.styles;

import org.cy3sbml.SBML;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Dark style.*/
public class StyleInfo_cy3sbmlDark extends StyleInfo {
    public static final String NAME = "cy3sbml-dark";
    public static final String TEMPLATE = "/styles/template_cy3sbml-dark.xml";

    /** Constructor. */
    public StyleInfo_cy3sbmlDark() {
        super(NAME, TEMPLATE);
        setMappings(createMappings());
    }

    /** Create the mappings for the given style. */
    @Override
    public List<Mapping> createMappings() {

        List<Mapping> mappings = super.createMappings();

        ////////////////////////////////
        // passthroughMapping
        ////////////////////////////////

        ////////////////////////////////
        // discreteMapping
        ////////////////////////////////

        Map<String, String> m3 = new HashMap<>();
        m3.put(SBML.NODETYPE_SPECIES, "#666666");
        m3.put(SBML.NODETYPE_REACTION_REVERSIBLE, "#FFFFFF");
        m3.put(SBML.NODETYPE_REACTION_IRREVERSIBLE, "#FF3333");
        m3.put(SBML.NODETYPE_COMPARTMENT, "#00CC66");
        m3.put(SBML.NODETYPE_PARAMETER, "#0099FF");
        m3.put(SBML.NODETYPE_LOCAL_PARAMETER, "#0099FF");
        m3.put(SBML.NODETYPE_ALGEBRAIC_RULE, "66CCFF");
        m3.put(SBML.NODETYPE_ASSIGNMENT_RULE, "66CCFF");
        m3.put(SBML.NODETYPE_RATE_RULE, "66CCFF");
        m3.put(SBML.NODETYPE_INITIAL_ASSIGNMENT, "#FF6666");
        m3.put(SBML.NODETYPE_KINETIC_LAW, "#66CCFF");
        m3.put(SBML.NODETYPE_QUAL_SPECIES, "#666666");
        m3.put(SBML.NODETYPE_QUAL_TRANSITION, "#999999");
        m3.put(SBML.NODETYPE_FBC_GENEPRODUCT, "#FF9966");
        m3.put(SBML.NODETYPE_UNIT_DEFINITION, "#CCCCCC");
        m3.put(SBML.NODETYPE_UNIT, "#000000");
        mappings.add(new MappingDiscrete(Mapping.DataType.string,
                VisualPropertyKey.NODE_FILL_COLOR, SBML.NODETYPE_ATTR_EXTENDED, "#666666", m3));

        // EDGE //

        Map<String, String> m10 = new HashMap<>();
        m10.put(SBML.INTERACTION_REACTION_PRODUCT, "#FFFFFF");
        m10.put(SBML.INTERACTION_REACTION_REACTANT, "#FFFFFF");
        m10.put(SBML.INTERACTION_REACTION_MODIFIER, "#3333FF");
        m10.put(SBML.INTERACTION_REACTION_ACTIVATOR, "#006600");
        m10.put(SBML.INTERACTION_REACTION_INHIBITOR, "#FF3333");
        m10.put(SBML.INTERACTION_QUAL_TRANSITION_OUTPUT, "#FFFFFF");
        m10.put(SBML.INTERACTION_QUAL_TRANSITION_INPUT, "#FFFFFF");
        m10.put(SBML.INTERACTION_FBC_GENEPRODUCT_SPECIES, "#3333FF");
        m10.put(SBML.INTERACTION_SBASE_UNITDEFINITION, "#FF3333");
        mappings.add(new MappingDiscrete(Mapping.DataType.string,
                VisualPropertyKey.EDGE_STROKE_UNSELECTED_PAINT, SBML.INTERACTION_ATTR_EXTENDED, "#CCCCCC", m10));
        mappings.add(new MappingDiscrete(Mapping.DataType.string,
                VisualPropertyKey.EDGE_SOURCE_ARROW_UNSELECTED_PAINT, SBML.INTERACTION_ATTR_EXTENDED, "#CCCCCC", m10));

        return mappings;
    }
}
