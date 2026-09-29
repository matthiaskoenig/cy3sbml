package org.cy3sbml.styles;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
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
import org.cytoscape.view.vizmap.mappings.PassthroughMapping;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * The layout style of the layout networks (#71), derived from a cy3sbml style: the node sizes
 * are the glyph sizes.
 */
class LayoutStyleFactoryTest {

    private VisualStyle base;
    private VisualStyle copy;
    private VisualPropertyDependency<?> sizeLocked;
    private DiscreteMapping<String, NodeShape> baseShape;
    private VisualMappingFunctionFactory passthrough;
    private VisualMappingFunctionFactory discrete;
    private LayoutStyleFactory factory;
    // the discrete mappings the factory creates, by visual property
    private final Map<VisualProperty<?>, DiscreteMapping<?, ?>> created = new HashMap<>();

    @BeforeEach
    @SuppressWarnings({"unchecked", "rawtypes"})
    void setUp() {
        base = mock(VisualStyle.class);
        when(base.getTitle()).thenReturn(SBML.STYLE_CY3SBML);
        baseShape = mock(DiscreteMapping.class);
        when(baseShape.getMappingColumnName()).thenReturn(SBML.NODETYPE_ATTR);
        when(baseShape.getAll())
                .thenReturn(Map.of(
                        SBML.NODETYPE_REACTION, NodeShapeVisualProperty.RECTANGLE,
                        SBML.NODETYPE_COMPARTMENT, NodeShapeVisualProperty.HEXAGON));
        when(base.getVisualMappingFunction(BasicVisualLexicon.NODE_SHAPE))
                .thenReturn((VisualMappingFunction) baseShape);

        copy = mock(VisualStyle.class);
        sizeLocked = mock(VisualPropertyDependency.class);
        when(sizeLocked.getIdString()).thenReturn(LayoutStyleFactory.NODE_SIZE_LOCKED);
        Set<VisualPropertyDependency<?>> dependencies = new HashSet<>();
        dependencies.add(sizeLocked);
        when(copy.getAllVisualPropertyDependencies()).thenReturn(dependencies);
        VisualStyleFactory styleFactory = mock(VisualStyleFactory.class);
        when(styleFactory.createVisualStyle(base)).thenReturn(copy);

        passthrough = mock(VisualMappingFunctionFactory.class);
        when(passthrough.createVisualMappingFunction(any(), any(), any())).thenAnswer(invocation -> {
            PassthroughMapping mapping = mock(PassthroughMapping.class);
            when(mapping.getMappingColumnName()).thenReturn(invocation.getArgument(0));
            when(mapping.getVisualProperty()).thenReturn(invocation.getArgument(2));
            return mapping;
        });
        discrete = mock(VisualMappingFunctionFactory.class);
        when(discrete.createVisualMappingFunction(any(), any(), any())).thenAnswer(invocation -> {
            DiscreteMapping mapping = mock(DiscreteMapping.class);
            when(mapping.getMappingColumnName()).thenReturn(invocation.getArgument(0));
            when(mapping.getVisualProperty()).thenReturn(invocation.getArgument(2));
            created.put(invocation.getArgument(2), mapping);
            return mapping;
        });
        factory = new LayoutStyleFactory(styleFactory, passthrough, discrete);
    }

    @Test
    void layoutStyleIsACopyWithTheLayoutTitle() {
        assertSame(copy, factory.create(base));

        verify(copy).setTitle(SBML.STYLE_CY3SBML + SBML.STYLE_SUFFIX_LAYOUT);
    }

    @Test
    void nodeSizeIsNotLocked() {
        factory.create(base);

        verify(sizeLocked).setDependency(false);
    }

    @Test
    void nodeSizeIsTheGlyphSize() {
        factory.create(base);

        verify(passthrough)
                .createVisualMappingFunction(SBML.ATTR_LAYOUT_WIDTH, Double.class, BasicVisualLexicon.NODE_WIDTH);
        verify(passthrough)
                .createVisualMappingFunction(SBML.ATTR_LAYOUT_HEIGHT, Double.class, BasicVisualLexicon.NODE_HEIGHT);
        verify(copy, times(2)).addVisualMappingFunction(any(PassthroughMapping.class));
    }

    @Test
    @SuppressWarnings("unchecked")
    void compartmentGlyphsAreRoundRectanglesBehindTheOtherNodes() {
        factory.create(base);

        DiscreteMapping<String, NodeShape> shape =
                (DiscreteMapping<String, NodeShape>) created.get(BasicVisualLexicon.NODE_SHAPE);
        verify(shape).putMapValue(SBML.NODETYPE_REACTION, NodeShapeVisualProperty.RECTANGLE);
        verify(shape).putMapValue(SBML.NODETYPE_COMPARTMENT, NodeShapeVisualProperty.ROUND_RECTANGLE);
        verify(copy).addVisualMappingFunction(shape);
        // the mapping of the base style is not changed
        verify(baseShape, never()).putMapValue(any(), any());

        DiscreteMapping<String, Double> z =
                (DiscreteMapping<String, Double>) created.get(BasicVisualLexicon.NODE_Z_LOCATION);
        assertEquals(SBML.ATTR_LAYOUT_GLYPH_TYPE, z.getMappingColumnName());
        verify(z).putMapValue(eq(SBML.NODETYPE_LAYOUT_COMPARTMENTGLYPH), eq(LayoutStyleFactory.COMPARTMENT_Z));
        DiscreteMapping<String, Integer> transparency =
                (DiscreteMapping<String, Integer>) created.get(BasicVisualLexicon.NODE_TRANSPARENCY);
        verify(transparency)
                .putMapValue(
                        eq(SBML.NODETYPE_LAYOUT_COMPARTMENTGLYPH), eq(LayoutStyleFactory.COMPARTMENT_TRANSPARENCY));
    }
}
