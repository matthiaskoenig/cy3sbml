package org.cy3sbml.styles;

import java.util.Map;
import org.cy3sbml.SBML;
import org.cytoscape.view.model.VisualProperty;
import org.cytoscape.view.presentation.property.BasicVisualLexicon;
import org.cytoscape.view.presentation.property.NodeShapeVisualProperty;
import org.cytoscape.view.presentation.property.values.NodeShape;
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
 * <li>compartments are round rectangles, and the compartment glyphs are transparent and
 * behind the other nodes.</li>
 * </ul>
 */
public final class LayoutStyleFactory {
    /** Id of the dependency that locks width and height of the nodes to the node size. */
    static final String NODE_SIZE_LOCKED = "nodeSizeLocked";
    /** Z location of the compartment glyphs, the other nodes have 0. */
    static final Double COMPARTMENT_Z = -1.0;
    /** Transparency of the compartment glyphs, 0 (transparent) to 255 (opaque). */
    static final Integer COMPARTMENT_TRANSPARENCY = 60;

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

        style.addVisualMappingFunction(shapes(base));
        DiscreteMapping<String, Double> z = discrete(SBML.ATTR_LAYOUT_GLYPH_TYPE, BasicVisualLexicon.NODE_Z_LOCATION);
        z.putMapValue(SBML.NODETYPE_LAYOUT_COMPARTMENTGLYPH, COMPARTMENT_Z);
        style.addVisualMappingFunction(z);
        DiscreteMapping<String, Integer> transparency =
                discrete(SBML.ATTR_LAYOUT_GLYPH_TYPE, BasicVisualLexicon.NODE_TRANSPARENCY);
        transparency.putMapValue(SBML.NODETYPE_LAYOUT_COMPARTMENTGLYPH, COMPARTMENT_TRANSPARENCY);
        style.addVisualMappingFunction(transparency);
        return style;
    }

    /** The node shapes of the base style, with round rectangles for the compartments. */
    private DiscreteMapping<String, NodeShape> shapes(VisualStyle base) {
        DiscreteMapping<String, NodeShape> shapes = discrete(SBML.NODETYPE_ATTR, BasicVisualLexicon.NODE_SHAPE);
        VisualMappingFunction<?, NodeShape> baseShapes = base.getVisualMappingFunction(BasicVisualLexicon.NODE_SHAPE);
        if (baseShapes instanceof DiscreteMapping<?, NodeShape> baseMapping
                && SBML.NODETYPE_ATTR.equals(baseMapping.getMappingColumnName())) {
            for (Map.Entry<?, NodeShape> entry : baseMapping.getAll().entrySet()) {
                shapes.putMapValue(String.valueOf(entry.getKey()), entry.getValue());
            }
        }
        shapes.putMapValue(SBML.NODETYPE_COMPARTMENT, NodeShapeVisualProperty.ROUND_RECTANGLE);
        return shapes;
    }

    private <V> DiscreteMapping<String, V> discrete(String column, VisualProperty<V> property) {
        return (DiscreteMapping<String, V>) discrete.createVisualMappingFunction(column, String.class, property);
    }
}
