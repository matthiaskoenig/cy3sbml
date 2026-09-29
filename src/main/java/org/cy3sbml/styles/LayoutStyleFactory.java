package org.cy3sbml.styles;

import java.util.Map;
import org.cy3sbml.SBML;
import org.cytoscape.view.model.VisualProperty;
import org.cytoscape.view.presentation.property.BasicVisualLexicon;
import org.cytoscape.view.presentation.property.NodeShapeVisualProperty;
import org.cytoscape.view.presentation.property.values.Justification;
import org.cytoscape.view.presentation.property.values.ObjectPosition;
import org.cytoscape.view.presentation.property.values.Position;
import org.cytoscape.view.vizmap.VisualMappingFunction;
import org.cytoscape.view.vizmap.VisualMappingFunctionFactory;
import org.cytoscape.view.vizmap.VisualPropertyDependency;
import org.cytoscape.view.vizmap.VisualStyle;
import org.cytoscape.view.vizmap.VisualStyleFactory;
import org.cytoscape.view.vizmap.mappings.DiscreteMapping;

/**
 * Creates the style of the layout networks (#71) from a cy3sbml style: a copy named
 * {@code <style>}{@link SBML#STYLE_SUFFIX_LAYOUT} with the node sizes of the glyphs.
 * <p>
 * The style is created in code, so it follows every change of the base style:
 * <ul>
 * <li>width and height are the {@code layout_width} and {@code layout_height} of the node
 * (the base style locks them to one size),</li>
 * <li>compartments are round rectangles with the label at the top, and the compartment glyphs
 * are transparent and behind the other nodes.</li>
 * </ul>
 */
public final class LayoutStyleFactory {
    /** Id of the dependency that locks width and height of the nodes to the node size. */
    static final String NODE_SIZE_LOCKED = "nodeSizeLocked";
    /** Z location of the compartment glyphs, the other nodes have 0. */
    static final Double COMPARTMENT_Z = -1.0;
    /** Transparency of the compartment glyphs, 0 (transparent) to 255 (opaque). */
    static final Integer COMPARTMENT_TRANSPARENCY = 60;
    /** Label of the compartment glyphs: inside at the top, not over the nodes in the centre. */
    static final ObjectPosition COMPARTMENT_LABEL_POSITION =
            new ObjectPosition(Position.NORTH, Position.NORTH, Justification.JUSTIFY_CENTER, 0.0, 5.0);

    private final VisualStyleFactory styleFactory;
    private final VisualMappingFunctionFactory passthrough;
    private final VisualMappingFunctionFactory discrete;

    /**
     * @param styleFactory factory of the copy of the base style
     * @param passthrough  factory of the passthrough mappings
     * @param discrete     factory of the discrete mappings
     */
    public LayoutStyleFactory(
            VisualStyleFactory styleFactory,
            VisualMappingFunctionFactory passthrough,
            VisualMappingFunctionFactory discrete) {
        this.styleFactory = styleFactory;
        this.passthrough = passthrough;
        this.discrete = discrete;
    }

    /** Name of the layout style of the style. */
    public static String layoutStyleName(String styleName) {
        return styleName + SBML.STYLE_SUFFIX_LAYOUT;
    }

    /** Creates the layout style of the base style. */
    public VisualStyle create(VisualStyle base) {
        VisualStyle style = styleFactory.createVisualStyle(base);
        style.setTitle(layoutStyleName(base.getTitle()));

        for (VisualPropertyDependency<?> dependency : style.getAllVisualPropertyDependencies()) {
            if (NODE_SIZE_LOCKED.equals(dependency.getIdString())) {
                dependency.setDependency(false);
            }
        }
        style.addVisualMappingFunction(passthrough.createVisualMappingFunction(
                SBML.ATTR_LAYOUT_WIDTH, Double.class, BasicVisualLexicon.NODE_WIDTH));
        style.addVisualMappingFunction(passthrough.createVisualMappingFunction(
                SBML.ATTR_LAYOUT_HEIGHT, Double.class, BasicVisualLexicon.NODE_HEIGHT));

        style.addVisualMappingFunction(
                withCompartment(base, BasicVisualLexicon.NODE_SHAPE, NodeShapeVisualProperty.ROUND_RECTANGLE));
        style.addVisualMappingFunction(
                withCompartment(base, BasicVisualLexicon.NODE_LABEL_POSITION, COMPARTMENT_LABEL_POSITION));
        DiscreteMapping<String, Double> z = discrete(SBML.ATTR_LAYOUT_GLYPH_TYPE, BasicVisualLexicon.NODE_Z_LOCATION);
        z.putMapValue(SBML.NODETYPE_LAYOUT_COMPARTMENTGLYPH, COMPARTMENT_Z);
        style.addVisualMappingFunction(z);
        DiscreteMapping<String, Integer> transparency =
                discrete(SBML.ATTR_LAYOUT_GLYPH_TYPE, BasicVisualLexicon.NODE_TRANSPARENCY);
        transparency.putMapValue(SBML.NODETYPE_LAYOUT_COMPARTMENTGLYPH, COMPARTMENT_TRANSPARENCY);
        style.addVisualMappingFunction(transparency);
        return style;
    }

    /**
     * The mapping of the node type of the base style for the property, with the value for the
     * compartments.
     */
    private <V> DiscreteMapping<String, V> withCompartment(
            VisualStyle base, VisualProperty<V> property, V compartment) {
        DiscreteMapping<String, V> mapping = discrete(SBML.NODETYPE_ATTR, property);
        VisualMappingFunction<?, V> baseMapping = base.getVisualMappingFunction(property);
        if (baseMapping instanceof DiscreteMapping<?, V> baseDiscrete
                && SBML.NODETYPE_ATTR.equals(baseDiscrete.getMappingColumnName())) {
            for (Map.Entry<?, V> entry : baseDiscrete.getAll().entrySet()) {
                mapping.putMapValue(String.valueOf(entry.getKey()), entry.getValue());
            }
        }
        mapping.putMapValue(SBML.NODETYPE_COMPARTMENT, compartment);
        return mapping;
    }

    private <V> DiscreteMapping<String, V> discrete(String column, VisualProperty<V> property) {
        return (DiscreteMapping<String, V>) discrete.createVisualMappingFunction(column, String.class, property);
    }
}
